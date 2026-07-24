package com.coinmaster.energypulse.notification.gemini;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GeminiRecommendationClient implements RecommendationClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    @Autowired
    public GeminiRecommendationClient(
            @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.model:gemini-3.5-flash}") String model) {
        this(RestClient.builder(), baseUrl, apiKey, model);
    }

    GeminiRecommendationClient(
            RestClient.Builder restClientBuilder,
            String baseUrl,
            String apiKey,
            String model) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generateRecommendation(String prompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        if (!model.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalStateException("Gemini model name is invalid.");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("maxOutputTokens", 1200));

        GeminiResponse response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(GeminiResponse.class);

        GeminiCandidate candidate = extractCompletedCandidate(response);
        String recommendation = extractText(candidate);
        if (!StringUtils.hasText(recommendation)) {
            throw new IllegalStateException("Gemini returned an empty recommendation.");
        }

        return recommendation.trim();
    }

    private GeminiCandidate extractCompletedCandidate(GeminiResponse response) {
        if (response == null || response.candidates() == null) {
            throw new IllegalStateException("Gemini returned no recommendation candidate.");
        }

        return response.candidates().stream()
                .filter(candidate -> candidate.content() != null)
                .findFirst()
                .map(candidate -> {
                    if (!"STOP".equals(candidate.finishReason())) {
                        String finishReason = StringUtils.hasText(candidate.finishReason())
                                ? candidate.finishReason()
                                : "UNKNOWN";
                        throw new IllegalStateException(
                                "Gemini response was incomplete: " + finishReason);
                    }
                    return candidate;
                })
                .orElseThrow(() ->
                        new IllegalStateException("Gemini returned no recommendation candidate."));
    }

    private String extractText(GeminiCandidate candidate) {
        return candidate.content().parts().stream()
                .filter(part -> !Boolean.TRUE.equals(part.thought()))
                .map(GeminiPart::text)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private record GeminiResponse(List<GeminiCandidate> candidates) {
    }

    private record GeminiCandidate(GeminiContent content, String finishReason) {
    }

    private record GeminiContent(List<GeminiPart> parts) {

        private GeminiContent {
            parts = parts == null ? List.of() : parts;
        }
    }

    private record GeminiPart(String text, Boolean thought) {
    }
}
