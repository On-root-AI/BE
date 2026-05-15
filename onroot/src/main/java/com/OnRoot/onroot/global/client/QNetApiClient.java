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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern ROUND_PATTERN = Pattern.compile("제(\\d+회)");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String serviceKey;

    public QNetApiClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${qnet.api.base-url}") String baseUrl,
            @Value("${qnet.api.key}") String serviceKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    public List<ExamSchedule> fetchAllWithExamNames(Map<String, List<ExamCodeParser.ExamInfo>> examsBySeries) {
        List<ExamSchedule> result = new ArrayList<>();
        result.addAll(buildSchedulesForSeries("getEList", "기사", examsBySeries));
        result.addAll(buildSchedulesForSeries("getPEList", "기술사", examsBySeries));
        return result;
    }

    private List<ExamSchedule> buildSchedulesForSeries(
            String endpoint, String seriesName,
            Map<String, List<ExamCodeParser.ExamInfo>> examsBySeries) {

        List<RoundSchedule> rounds = fetchRounds(endpoint);
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

                LocalDate pracDate = parseDate(round.pracExamStartDt());
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
            long docExamDt, long docRegStartDt, long docRegEndDt, long docPassDt,
            long pracExamStartDt, long pracRegStartDt, long pracRegEndDt, long pracPassDt
    ) {}

    private List<RoundSchedule> fetchRounds(String endpoint) {
        String url = baseUrl + "/" + endpoint + "?serviceKey=" + serviceKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Connection", "close");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return parseRounds(response.getBody());
        } catch (Exception e) {
            log.error("Q-Net {} 호출 실패: {}", endpoint, e.getMessage());
            return List.of();
        }
    }

    private List<RoundSchedule> parseRounds(String json) {
        List<RoundSchedule> rounds = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("response").path("body").path("items").path("item");
            if (items.isMissingNode()) return rounds;

            Iterable<JsonNode> itemList = items.isArray() ? items : List.of(items);
            for (JsonNode item : itemList) {
                String description = item.path("description").asText();
                long docExamDt = item.path("docexamdt").asLong();
                String key = description + "_" + docExamDt;
                if (seen.contains(key)) continue;
                seen.add(key);

                rounds.add(new RoundSchedule(
                        description,
                        docExamDt,
                        item.path("docregstartdt").asLong(),
                        item.path("docregenddt").asLong(),
                        item.path("docpassdt").asLong(),
                        item.path("pracexamstartdt").asLong(),
                        item.path("pracregstartdt").asLong(),
                        item.path("pracregenddt").asLong(),
                        item.path("pracpassdt").asLong()
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Q-Net 응답 파싱 실패", e);
        }

        return rounds;
    }

    private String extractRoundLabel(String description) {
        Matcher m = ROUND_PATTERN.matcher(description);
        return m.find() ? m.group(1) : description;
    }

    private LocalDate parseDate(long value) {
        if (value == 0) return null;
        return LocalDate.parse(String.valueOf(value), DATE_FORMATTER);
    }
}
