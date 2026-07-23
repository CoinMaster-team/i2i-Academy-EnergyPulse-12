package com.coinmaster.energypulse.simulator.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TelemetryEvent(
        UUID eventId,
        int schemaVersion,
        OffsetDateTime occurredAt,
        UUID homeId,
        UUID applianceId,
        BigDecimal currentWatt) {
}
