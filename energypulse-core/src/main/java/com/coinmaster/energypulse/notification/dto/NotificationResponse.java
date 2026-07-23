package com.coinmaster.energypulse.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID homeId,
        String homeName,
        UUID applianceId,
        String applianceName,
        String eventType,
        String eventDetails,
        String recipientEmail,
        String provider,
        String languageCode,
        String promptText,
        String recommendationText,
        String generationStatus,
        String emailStatus,
        String generationError,
        String emailError,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt) {
}
