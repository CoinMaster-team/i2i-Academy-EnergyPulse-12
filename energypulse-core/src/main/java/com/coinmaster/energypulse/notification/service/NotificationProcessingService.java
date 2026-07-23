package com.coinmaster.energypulse.notification.service;

import com.coinmaster.energypulse.notification.dto.GeneratedRecommendation;
import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import com.coinmaster.energypulse.notification.gemini.RecommendationService;
import com.coinmaster.energypulse.notification.mail.EmailDeliveryService;
import com.coinmaster.energypulse.notification.repository.NotificationQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProcessingService.class);
    private static final int MAX_ERROR_LENGTH = 2000;

    private final NotificationQueryRepository notificationRepository;
    private final RecommendationService recommendationService;
    private final EmailDeliveryService emailDeliveryService;
    private final int batchSize;

    public NotificationProcessingService(
            NotificationQueryRepository notificationRepository,
            RecommendationService recommendationService,
            EmailDeliveryService emailDeliveryService,
            @Value("${app.notifications.batch-size:20}") int batchSize) {
        this.notificationRepository = notificationRepository;
        this.recommendationService = recommendationService;
        this.emailDeliveryService = emailDeliveryService;
        this.batchSize = Math.max(1, batchSize);
    }

    public int processPendingNotifications() {
        List<NotificationCandidate> candidates =
                notificationRepository.findPendingCandidates(batchSize);

        return (int) candidates.stream()
                .filter(this::processCandidateSafely)
                .count();
    }

    public boolean processEvent(UUID operationalEventId) {
        return notificationRepository.findPendingCandidate(operationalEventId)
                .map(this::processCandidate)
                .orElse(false);
    }

    private boolean processCandidateSafely(NotificationCandidate candidate) {
        try {
            return processCandidate(candidate);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Notification processing failed for operational event {}.",
                    candidate.operationalEventId(),
                    exception);
            return false;
        }
    }

    private boolean processCandidate(NotificationCandidate candidate) {
        String prompt = recommendationService.createPrompt(candidate);
        GeneratedRecommendation recommendation =
                recommendationService.generate(candidate, prompt);

        return notificationRepository.createPendingNotification(
                        candidate,
                        prompt,
                        recommendation)
                .map(notificationId -> {
                    deliverEmail(
                            notificationId,
                            candidate,
                            recommendation);
                    return true;
                })
                .orElse(false);
    }

    private void deliverEmail(
            UUID notificationId,
            NotificationCandidate candidate,
            GeneratedRecommendation recommendation) {
        try {
            emailDeliveryService.sendNotification(candidate, recommendation);
            notificationRepository.markEmailSent(notificationId);
        } catch (RuntimeException exception) {
            notificationRepository.markEmailFailed(
                    notificationId,
                    shortenError(exception));
        }
    }

    private String shortenError(RuntimeException exception) {
        String message = exception.getMessage();
        String safeMessage = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;

        return safeMessage.substring(0, Math.min(safeMessage.length(), MAX_ERROR_LENGTH));
    }
}
