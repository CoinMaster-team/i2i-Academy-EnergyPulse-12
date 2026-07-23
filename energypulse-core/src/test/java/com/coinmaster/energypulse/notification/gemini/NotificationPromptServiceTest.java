package com.coinmaster.energypulse.notification.gemini;

import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPromptServiceTest {

    private final NotificationPromptService promptService = new NotificationPromptService();

    @Test
    void shouldCreateTurkishPromptWithEventContext() {
        String prompt = promptService.createPrompt(candidate());

        assertTrue(prompt.contains("yalnızca Türkçe"));
        assertTrue(prompt.contains("QA Smart Home"));
        assertTrue(prompt.contains("Air Conditioner"));
        assertTrue(prompt.contains("APPLIANCE_ANOMALY_DETECTED"));
        assertTrue(prompt.contains("Elektrik panosuna"));
    }

    private NotificationCandidate candidate() {
        return new NotificationCandidate(
                UUID.fromString("213df402-f11c-4432-a3a5-c38bc2208a67"),
                UUID.fromString("7d04ff7b-5d99-48c4-8777-187e7cf0d521"),
                "QA Smart Home",
                "qa@energypulse.local",
                UUID.fromString("1b8ef114-41fa-4b09-9292-28fe52c41f95"),
                "Air Conditioner",
                "APPLIANCE_ANOMALY_DETECTED",
                "{\"powerWatts\":2350}",
                OffsetDateTime.parse("2026-07-23T10:30:00Z"));
    }
}
