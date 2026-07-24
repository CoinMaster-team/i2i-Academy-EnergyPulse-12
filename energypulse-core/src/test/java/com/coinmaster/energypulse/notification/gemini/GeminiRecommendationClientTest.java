package com.coinmaster.energypulse.notification.gemini;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiRecommendationClientTest {

    @Test
    void shouldCallGeminiAndExtractRecommendation() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GeminiRecommendationClient client = new GeminiRecommendationClient(
                restClientBuilder,
                "https://generativelanguage.googleapis.com",
                "test-api-key",
                "gemini-3.5-flash");

        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(header("x-goog-api-key", "test-api-key"))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(1200))
                .andRespond(withSuccess(
                        """
                                {
                                  "candidates": [
                                    {
                                      "content": {
                                        "parts": [
                                          {"text": "Düşünce özeti", "thought": true},
                                          {"text": "Cihazı güvenli şekilde"},
                                          {"text": "kontrol edin."}
                                        ]
                                      },
                                      "finishReason": "STOP"
                                    }
                                  ]
                                }
                                """,
                        MediaType.APPLICATION_JSON));

        String recommendation = client.generateRecommendation("Türkçe öneri üret.");

        assertEquals(
                "Cihazı güvenli şekilde" + System.lineSeparator() + "kontrol edin.",
                recommendation);
        server.verify();
    }

    @Test
    void shouldRejectIncompleteRecommendation() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GeminiRecommendationClient client = new GeminiRecommendationClient(
                restClientBuilder,
                "https://generativelanguage.googleapis.com",
                "test-api-key",
                "gemini-3.5-flash");

        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withSuccess(
                        """
                                {
                                  "candidates": [
                                    {
                                      "content": {
                                        "parts": [
                                          {"text": "Yarım kalan öneri"}
                                        ]
                                      },
                                      "finishReason": "MAX_TOKENS"
                                    }
                                  ]
                                }
                                """,
                        MediaType.APPLICATION_JSON));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.generateRecommendation("Türkçe öneri üret."));

        assertEquals(
                "Gemini response was incomplete: MAX_TOKENS",
                exception.getMessage());
        server.verify();
    }

    @Test
    void shouldRetryTransientGeminiFailure() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        List<Long> retryDelays = new ArrayList<>();
        GeminiRecommendationClient client = new GeminiRecommendationClient(
                restClientBuilder,
                "https://generativelanguage.googleapis.com",
                "test-api-key",
                "gemini-3.5-flash",
                retryDelays::add);

        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": {
                                    "code": 503,
                                    "status": "UNAVAILABLE"
                                  }
                                }
                                """));
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withSuccess(
                        """
                                {
                                  "candidates": [
                                    {
                                      "content": {
                                        "parts": [
                                          {"text": "İkinci deneme başarılı."}
                                        ]
                                      },
                                      "finishReason": "STOP"
                                    }
                                  ]
                                }
                                """,
                        MediaType.APPLICATION_JSON));

        String recommendation = client.generateRecommendation("Türkçe öneri üret.");

        assertEquals("İkinci deneme başarılı.", recommendation);
        assertEquals(List.of(750L), retryDelays);
        server.verify();
    }

    @Test
    void shouldFailWithoutApiKey() {
        GeminiRecommendationClient client = new GeminiRecommendationClient(
                RestClient.builder(),
                "https://generativelanguage.googleapis.com",
                "",
                "gemini-3.5-flash");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.generateRecommendation("prompt"));

        assertEquals("Gemini API key is not configured.", exception.getMessage());
    }
}
