package com.OnRoot.onroot.global.client;

import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import com.OnRoot.onroot.global.parser.ExamCodeParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class QNetApiClient {

    private static final String QUAL_EXAM_URL = "http://apis.data.go.kr/B490007/qualExamSchd/getQualExamSchdList";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern ROUND_PATTERN = Pattern.compile("제(\\d+회)");
    private static final int PAGE_SIZE = 50;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceKey;

    public QNetApiClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${qnet.api.key}") String serviceKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
    }

    public List<ExamSchedule> fetchEngineers(Map<String, List<ExamCodeParser.ExamInfo>> examsBySeries) {
        return buildSchedulesForSeries("기사", examsBySeries);
    }

    public List<ExamSchedule> fetchProfessionals(Map<String, List<ExamCodeParser.ExamInfo>> examsBySeries) {
        return buildSchedulesForSeries("기술사", examsBySeries);
    }

    private List<ExamSchedule> buildSchedulesForSeries(
            String seriesName,
            Map<String, List<ExamCodeParser.ExamInfo>> examsBySeries) {

        List<RoundSchedule> rounds = fetchRoundsForSeries(seriesName);
        List<ExamCodeParser.ExamInfo> exams = examsBySeries.getOrDefault(seriesName, List.of());

        if (rounds.isEmpty() || exams.isEmpty()) {
            log.warn("{} 일정 데이터 없음: rounds={}, exams={}", seriesName, rounds.size(), exams.size());
            return List.of();
        }

        List<ExamSchedule> result = new ArrayList<>();
        for (RoundSchedule round : rounds) {
            String roundLabel = extractRoundLabel(round.description());
            for (ExamCodeParser.ExamInfo exam : exams) {
                LocalDate docDate = parseDate(round.docExamDt());
                if (docDate != null) {
                    result.add(ExamSchedule.builder()
                            .examName(exam.jmfldnm() + " 필기 " + roundLabel)
                            .subject(exam.jmfldnm())
                            .applicationStart(parseDate(round.docRegStartDt()))
                            .applicationEnd(parseDate(round.docRegEndDt()))
                            .examDate(docDate)
                            .resultDate(parseDate(round.docPassDt()))
                            .build());
                }

                LocalDate pracDate = parseDate(round.pracExamDt());
                if (pracDate != null) {
                    result.add(ExamSchedule.builder()
                            .examName(exam.jmfldnm() + " 실기 " + roundLabel)
                            .subject(exam.jmfldnm())
                            .applicationStart(parseDate(round.pracRegStartDt()))
                            .applicationEnd(parseDate(round.pracRegEndDt()))
                            .examDate(pracDate)
                            .resultDate(parseDate(round.pracPassDt()))
                            .build());
                }
            }
        }

        log.info("{} 시험일정 생성: {}건 ({}개 종목 × {}회차)",
                seriesName, result.size(), exams.size(), rounds.size());
        return result;
    }

    private record RoundSchedule(
            String description,
            String docExamDt, String docRegStartDt, String docRegEndDt, String docPassDt,
            String pracExamDt, String pracRegStartDt, String pracRegEndDt, String pracPassDt
    ) {}

    private List<RoundSchedule> fetchRoundsForSeries(String seriesName) {
        int currentYear = LocalDate.now().getYear();
        Set<String> seen = new HashSet<>();
        List<RoundSchedule> all = new ArrayList<>();

        for (int year = currentYear; year <= currentYear + 1; year++) {
            all.addAll(fetchRoundsForYear(year, seriesName, seen));
        }

        log.info("{} 회차 조회 완료: {}건", seriesName, all.size());
        return all;
    }

    private List<RoundSchedule> fetchRoundsForYear(int year, String seriesName, Set<String> seen) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Connection", "close");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<RoundSchedule> result = new ArrayList<>();
        int pageNo = 1;
        int totalFetched = 0;
        int totalCount = Integer.MAX_VALUE;

        try {
            while (totalFetched < totalCount) {
                String url = QUAL_EXAM_URL
                        + "?serviceKey=" + serviceKey
                        + "&numOfRows=" + PAGE_SIZE
                        + "&pageNo=" + pageNo
                        + "&dataFormat=json"
                        + "&implYy=" + year
                        + "&qualgbCd=T";

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                PageResult pageResult = parsePage(response.getBody(), seriesName, seen);
                totalCount = pageResult.totalCount();
                result.addAll(pageResult.rounds());
                totalFetched += pageResult.pageItemCount();

                if (pageResult.pageItemCount() == 0) break;
                pageNo++;
            }
        } catch (Exception e) {
            log.error("Q-Net {} {}년 호출 실패: {}", seriesName, year, e.getMessage());
        }

        return result;
    }

    private record PageResult(List<RoundSchedule> rounds, int totalCount, int pageItemCount) {}

    private PageResult parsePage(String json, String seriesName, Set<String> seen) {
        List<RoundSchedule> rounds = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);

            String resultCode = root.path("header").path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                log.warn("API 오류 응답: {}", root.path("header").path("resultMsg").asText());
                return new PageResult(rounds, 0, 0);
            }

            JsonNode body = root.path("body");
            int totalCount = body.path("totalCount").asInt(0);
            JsonNode items = body.path("items");

            if (items.isMissingNode() || !items.isArray()) {
                return new PageResult(rounds, totalCount, 0);
            }

            for (JsonNode item : items) {
                String description = item.path("description").asText();
                if (!description.contains("국가기술자격 " + seriesName)) continue;

                String docExamDt = item.path("docExamStartDt").asText();
                String key = description + "_" + docExamDt;
                if (seen.contains(key)) continue;
                seen.add(key);

                rounds.add(new RoundSchedule(
                        description,
                        docExamDt,
                        item.path("docRegStartDt").asText(),
                        item.path("docRegEndDt").asText(),
                        item.path("docPassDt").asText(),
                        item.path("pracExamStartDt").asText(),
                        item.path("pracRegStartDt").asText(),
                        item.path("pracRegEndDt").asText(),
                        item.path("pracPassDt").asText()
                ));
            }

            return new PageResult(rounds, totalCount, items.size());
        } catch (Exception e) {
            throw new RuntimeException("응답 파싱 실패", e);
        }
    }

    private String extractRoundLabel(String description) {
        Matcher m = ROUND_PATTERN.matcher(description);
        return m.find() ? m.group(1) : description;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}