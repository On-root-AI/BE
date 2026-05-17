package com.OnRoot.onroot.global.client;

import com.OnRoot.onroot.global.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public GeminiApiClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.base-url}") String baseUrl,
            @Value("${gemini.api.model}") String model) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String generate(String prompt) {
        String url = baseUrl + "/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String generated = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
            log.info("Gemini 응답 수신 완료 ({}자)", generated.length());
            return generated;
        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());
            throw new ExternalServiceException("AI 서비스 호출에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
