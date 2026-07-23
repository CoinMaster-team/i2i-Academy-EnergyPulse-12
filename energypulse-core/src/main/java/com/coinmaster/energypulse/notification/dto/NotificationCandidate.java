package com.coinmaster.energypulse.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationCandidate(
        UUID operationalEventId,
        UUID homeId,
        String homeName,
        String recipientEmail,
        UUID applianceId,
        String applianceName,
        String eventType,
        String eventDetails,
        OffsetDateTime occurredAt) {
}
