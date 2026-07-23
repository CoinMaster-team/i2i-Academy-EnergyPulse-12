package com.coinmaster.energypulse.notification.gemini;

import com.coinmaster.energypulse.notification.dto.GeneratedRecommendation;
import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationClient recommendationClient;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                recommendationClient,
                new NotificationPromptService(),
                new FallbackRecommendationService());
    }

    @Test
    void shouldReturnGeneratedRecommendation() {
        when(recommendationClient.generateRecommendation("prompt"))
                .thenReturn("Cihazı kontrol edin.");

        GeneratedRecommendation result = recommendationService.generate(
                candidate(),
                "prompt");

        assertEquals("GENERATED", result.status());
        assertEquals("Cihazı kontrol edin.", result.text());
        assertNull(result.error());
    }

    @Test
    void shouldUseFallbackWhenGeminiFails() {
        when(recommendationClient.generateRecommendation("prompt"))
                .thenThrow(new IllegalStateException("Gemini unavailable"));

        GeneratedRecommendation result = recommendationService.generate(
                candidate(),
                "prompt");

        assertEquals("FALLBACK", result.status());
        assertTrue(result.text().contains("Air Conditioner"));
        assertEquals("Gemini unavailable", result.error());
    }

    private NotificationCandidate candidate() {
        return new NotificationCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "QA Smart Home",
                "qa@energypulse.local",
                UUID.randomUUID(),
                "Air Conditioner",
                "APPLIANCE_ANOMALY_DETECTED",
                "{}",
                OffsetDateTime.parse("2026-07-23T10:30:00Z"));
    }
}
