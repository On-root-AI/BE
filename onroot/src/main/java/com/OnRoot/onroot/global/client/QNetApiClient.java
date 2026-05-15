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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class QNetApiClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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

    public List<ExamSchedule> fetchAll() {
        List<ExamSchedule> result = new ArrayList<>();
        result.addAll(fetch("getEList", "기사/산업기사"));
        result.addAll(fetch("getPEList", "기술사"));
        return result;
    }

    private List<ExamSchedule> fetch(String endpoint, String defaultSubject) {
        String url = baseUrl + "/" + endpoint + "?serviceKey=" + serviceKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Connection", "close");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String body = response.getBody();
        return parseJson(body, defaultSubject);
    }

    private List<ExamSchedule> parseJson(String json, String defaultSubject) {
        List<ExamSchedule> schedules = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isMissingNode()) return schedules;

            Iterable<JsonNode> itemList = items.isArray() ? items : List.of(items);

            for (JsonNode item : itemList) {
                String description = item.path("description").asText();
                String examDateStr = String.valueOf(item.path("docexamdt").asLong());

                String dedupeKey = description + "_" + examDateStr;
                if (seen.contains(dedupeKey)) continue;
                seen.add(dedupeKey);

                String subject = description.contains("(")
                        ? description.substring(0, description.indexOf("(")).trim()
                        : defaultSubject;

                schedules.add(ExamSchedule.builder()
                        .examName(description)
                        .subject(subject)
                        .applicationStart(parseDate(item.path("docregstartdt").asLong()))
                        .applicationEnd(parseDate(item.path("docregenddt").asLong()))
                        .examDate(parseDate(item.path("docexamdt").asLong()))
                        .resultDate(parseDate(item.path("docpassdt").asLong()))
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Q-Net API 응답 파싱 실패", e);
        }

        return schedules;
    }

    private LocalDate parseDate(long value) {
        if (value == 0) return null;
        return LocalDate.parse(String.valueOf(value), DATE_FORMATTER);
    }
}
