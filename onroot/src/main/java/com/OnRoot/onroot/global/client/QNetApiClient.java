package com.OnRoot.onroot.global.client;

import com.OnRoot.onroot.domain.examschedule.entity.ExamSchedule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class QNetApiClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String serviceKey;

    public QNetApiClient(
            RestTemplate restTemplate,
            @Value("${qnet.api.base-url}") String baseUrl,
            @Value("${qnet.api.key}") String serviceKey) {
        this.restTemplate = restTemplate;
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
        String xml = restTemplate.getForObject(url, String.class);
        return parseXml(xml, defaultSubject);
    }

    private List<ExamSchedule> parseXml(String xml, String defaultSubject) {
        List<ExamSchedule> schedules = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);

                String description = getText(item, "description");
                String examDateStr = getText(item, "docexamdt");

                String dedupeKey = description + "_" + examDateStr;
                if (seen.contains(dedupeKey)) continue;
                seen.add(dedupeKey);

                String subject = description.contains("(")
                        ? description.substring(0, description.indexOf("(")).trim()
                        : defaultSubject;

                schedules.add(ExamSchedule.builder()
                        .examName(description)
                        .subject(subject)
                        .applicationStart(parseDate(getText(item, "docregstartdt")))
                        .applicationEnd(parseDate(getText(item, "docregenddt")))
                        .examDate(parseDate(examDateStr))
                        .resultDate(parseDate(getText(item, "docpassdt")))
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Q-Net API 응답 파싱 실패", e);
        }

        return schedules;
    }

    private String getText(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value, DATE_FORMATTER);
    }
}
