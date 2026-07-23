package com.coinmaster.energypulse.notification.gemini;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
                .andRespond(withSuccess(
                        """
                                {
                                  "candidates": [
                                    {
                                      "content": {
                                        "parts": [
                                          {"text": "Cihazı güvenli şekilde kontrol edin."}
                                        ]
                                      }
                                    }
                                  ]
                                }
                                """,
                        MediaType.APPLICATION_JSON));

        String recommendation = client.generateRecommendation("Türkçe öneri üret.");

        assertEquals("Cihazı güvenli şekilde kontrol edin.", recommendation);
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
