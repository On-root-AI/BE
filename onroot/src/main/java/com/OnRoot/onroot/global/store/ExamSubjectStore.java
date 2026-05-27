package com.OnRoot.onroot.global.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamSubjectStore {

    private final ObjectMapper objectMapper;

    private final Map<String, ExamSubjects> byName = new HashMap<>();

    public record ExamSubjects(String jmcd, String name, List<String> written, List<String> practical) {}

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("exam-subjects.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());

            root.fields().forEachRemaining(entry -> {
                String jmcd = entry.getKey();
                JsonNode node = entry.getValue();
                String name = node.path("name").asText();
                List<String> written = readList(node.path("written"));
                List<String> practical = readList(node.path("practical"));
                ExamSubjects subjects = new ExamSubjects(jmcd, name, written, practical);
                byName.put(name, subjects);
            });

            log.info("시험 과목 데이터 로드 완료: {}개 자격증", byName.size());
        } catch (Exception e) {
            log.error("시험 과목 데이터 로드 실패", e);
        }
    }

    public Optional<ExamSubjects> findByName(String examName) {
        if (examName == null || examName.isBlank()) return Optional.empty();
        if (byName.containsKey(examName)) return Optional.of(byName.get(examName));
        return byName.entrySet().stream()
                .filter(e -> examName.contains(e.getKey()) || e.getKey().contains(examName))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private List<String> readList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) node.forEach(n -> result.add(n.asText()));
        return result;
    }
}