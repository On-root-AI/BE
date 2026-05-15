package com.OnRoot.onroot.global.client;

import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
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
import java.util.List;

@Slf4j
@Component
public class QualExamSchdApiClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String serviceKey;

    public QualExamSchdApiClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${qualexam.api.base-url}") String baseUrl,
            @Value("${qualexam.api.key}") String serviceKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.serviceKey = serviceKey;
    }

    public List<ExamSchedule> fetchAll() {
        int year = LocalDate.now().getYear();
        List<ExamSchedule> result = new ArrayList<>();
        result.addAll(fetchByYear(year));
        result.addAll(fetchByYear(year + 1));
        return result;
    }

    private static final int PAGE_SIZE = 50;

    private List<ExamSchedule> fetchByYear(int year) {
        log.info("국가자격 시험일정 API 호출: year={}", year);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Connection", "close");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<ExamSchedule> result = new ArrayList<>();
        int pageNo = 1;

        try {
            while (true) {
                String url = baseUrl + "/getQualExamSchdList"
                        + "?serviceKey=" + serviceKey
                        + "&implYy=" + year
                        + "&numOfRows=" + PAGE_SIZE
                        + "&pageNo=" + pageNo
                        + "&dataFormat=json";

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                String body = response.getBody();

                PageResult page = parsePage(body, year);
                result.addAll(page.schedules());

                if (result.size() >= page.totalCount() || page.schedules().isEmpty()) break;
                pageNo++;
            }
        } catch (Exception e) {
            log.error("국가자격 시험일정 API 호출 실패 ({}년): {}", year, e.getMessage());
            return List.of();
        }

        return result;
    }

    private record PageResult(List<ExamSchedule> schedules, int totalCount) {}

    private PageResult parsePage(String json, int year) {
        List<ExamSchedule> schedules = new ArrayList<>();
        int totalCount = 0;

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode bodyNode = root.path("body");
            totalCount = bodyNode.path("totalCount").asInt(0);
            JsonNode items = bodyNode.path("items");

            if (items.isMissingNode() || items.isNull() || !items.isArray() || items.isEmpty()) {
                log.warn("국가자격 시험일정 API: {}년 데이터 없음", year);
                return new PageResult(schedules, totalCount);
            }

            for (JsonNode item : items) {
                String description = getField(item, "description");
                if (description.isBlank()) continue;

                String subject = extractSubject(description);

                LocalDate docExamDt = parseDate(getField(item, "docExamStartDt"));
                if (docExamDt != null) {
                    schedules.add(ExamSchedule.builder()
                            .examName(description + " 필기")
                            .subject(subject)
                            .applicationStart(parseDate(getField(item, "docRegStartDt")))
                            .applicationEnd(parseDate(getField(item, "docRegEndDt")))
                            .examDate(docExamDt)
                            .resultDate(parseDate(getField(item, "docPassDt")))
                            .build());
                }

                LocalDate pracExamDt = parseDate(getField(item, "pracExamStartDt"));
                if (pracExamDt != null) {
                    schedules.add(ExamSchedule.builder()
                            .examName(description + " 실기")
                            .subject(subject)
                            .applicationStart(parseDate(getField(item, "pracRegStartDt")))
                            .applicationEnd(parseDate(getField(item, "pracRegEndDt")))
                            .examDate(pracExamDt)
                            .resultDate(parseDate(getField(item, "pracPassDt")))
                            .build());
                }
            }

            log.info("국가자격 시험일정 파싱: {}년 page={} {}건 (total={})", year, schedules.size(), schedules.size(), totalCount);
        } catch (Exception e) {
            log.error("국가자격 시험일정 API 파싱 실패", e);
            throw new RuntimeException("국가자격 시험일정 API 응답 파싱 실패", e);
        }

        return new PageResult(schedules, totalCount);
    }

    private String extractSubject(String description) {
        // "국가기술자격 기능사 (2026년도 제107회)" -> "기능사"
        int parenIdx = description.indexOf("(");
        String base = parenIdx > 0 ? description.substring(0, parenIdx).trim() : description;
        String prefix = "국가기술자격 ";
        return base.startsWith(prefix) ? base.substring(prefix.length()) : base;
    }

    private String getField(JsonNode item, String... keys) {
        for (String key : keys) {
            JsonNode node = item.path(key);
            if (!node.isMissingNode() && !node.isNull()) {
                String val = node.asText().trim();
                if (!val.isEmpty() && !val.equals("null")) return val;
            }
            // lowercase 시도
            node = item.path(key.toLowerCase());
            if (!node.isMissingNode() && !node.isNull()) {
                String val = node.asText().trim();
                if (!val.isEmpty() && !val.equals("null")) return val;
            }
        }
        return "";
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String cleaned = value.replace("-", "").replace(".", "");
            if (cleaned.length() == 8) {
                return LocalDate.parse(cleaned, DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.debug("날짜 파싱 실패: {}", value);
        }
        return null;
    }
}
