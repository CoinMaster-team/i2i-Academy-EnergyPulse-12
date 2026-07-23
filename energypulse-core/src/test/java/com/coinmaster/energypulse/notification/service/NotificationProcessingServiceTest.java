package com.coinmaster.energypulse.notification.service;

import com.coinmaster.energypulse.notification.dto.GeneratedRecommendation;
import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import com.coinmaster.energypulse.notification.gemini.RecommendationService;
import com.coinmaster.energypulse.notification.mail.EmailDeliveryService;
import com.coinmaster.energypulse.notification.repository.NotificationQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProcessingServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("2d6841fc-036c-4332-a3c0-07bcd17fb0b5");
    private static final UUID NOTIFICATION_ID = UUID.fromString("e9d90ad8-a50f-42bb-872f-8f93099d96c0");

    @Mock
    private NotificationQueryRepository notificationRepository;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private EmailDeliveryService emailDeliveryService;

    private NotificationProcessingService processingService;

    @BeforeEach
    void setUp() {
        processingService = new NotificationProcessingService(
                notificationRepository,
                recommendationService,
                emailDeliveryService,
                20);
    }

    @Test
    void shouldGenerateSendAndMarkNotificationAsSent() {
        NotificationCandidate candidate = candidate();
        GeneratedRecommendation recommendation =
                GeneratedRecommendation.generated("Cihazı kontrol edin.");

        when(notificationRepository.findPendingCandidate(EVENT_ID))
                .thenReturn(Optional.of(candidate));
        when(recommendationService.createPrompt(candidate)).thenReturn("prompt");
        when(recommendationService.generate(candidate, "prompt"))
                .thenReturn(recommendation);
        when(notificationRepository.createPendingNotification(
                candidate,
                "prompt",
                recommendation))
                .thenReturn(Optional.of(NOTIFICATION_ID));

        assertTrue(processingService.processEvent(EVENT_ID));

        verify(emailDeliveryService).sendNotification(candidate, recommendation);
        verify(notificationRepository).markEmailSent(NOTIFICATION_ID);
        verify(notificationRepository, never()).markEmailFailed(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPersistEmailFailure() {
        NotificationCandidate candidate = candidate();
        GeneratedRecommendation recommendation =
                GeneratedRecommendation.fallback("Güvenli öneri", "Gemini unavailable");

        when(notificationRepository.findPendingCandidate(EVENT_ID))
                .thenReturn(Optional.of(candidate));
        when(recommendationService.createPrompt(candidate)).thenReturn("prompt");
        when(recommendationService.generate(candidate, "prompt"))
                .thenReturn(recommendation);
        when(notificationRepository.createPendingNotification(
                candidate,
                "prompt",
                recommendation))
                .thenReturn(Optional.of(NOTIFICATION_ID));
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailDeliveryService)
                .sendNotification(candidate, recommendation);

        assertTrue(processingService.processEvent(EVENT_ID));

        verify(notificationRepository).markEmailFailed(
                org.mockito.ArgumentMatchers.eq(NOTIFICATION_ID),
                contains("SMTP unavailable"));
        verify(notificationRepository, never()).markEmailSent(NOTIFICATION_ID);
    }

    @Test
    void shouldSkipAlreadyClaimedNotification() {
        NotificationCandidate candidate = candidate();
        GeneratedRecommendation recommendation =
                GeneratedRecommendation.generated("Öneri");

        when(notificationRepository.findPendingCandidate(EVENT_ID))
                .thenReturn(Optional.of(candidate));
        when(recommendationService.createPrompt(candidate)).thenReturn("prompt");
        when(recommendationService.generate(candidate, "prompt"))
                .thenReturn(recommendation);
        when(notificationRepository.createPendingNotification(
                candidate,
                "prompt",
                recommendation))
                .thenReturn(Optional.empty());

        assertFalse(processingService.processEvent(EVENT_ID));

        verifyNoInteractions(emailDeliveryService);
    }

    @Test
    void shouldProcessPendingBatch() {
        NotificationCandidate candidate = candidate();
        GeneratedRecommendation recommendation =
                GeneratedRecommendation.generated("Öneri");

        when(notificationRepository.findPendingCandidates(20))
                .thenReturn(List.of(candidate));
        when(recommendationService.createPrompt(candidate)).thenReturn("prompt");
        when(recommendationService.generate(candidate, "prompt"))
                .thenReturn(recommendation);
        when(notificationRepository.createPendingNotification(
                candidate,
                "prompt",
                recommendation))
                .thenReturn(Optional.of(NOTIFICATION_ID));

        int processedCount = processingService.processPendingNotifications();

        assertTrue(processedCount == 1);
    }

    private NotificationCandidate candidate() {
        return new NotificationCandidate(
                EVENT_ID,
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
