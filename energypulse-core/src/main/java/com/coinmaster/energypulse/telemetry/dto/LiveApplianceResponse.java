package com.coinmaster.energypulse.telemetry.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LiveApplianceResponse(
        UUID applianceId,
        String name,
        BigDecimal safeLimitWatt,
        BigDecimal currentWatt,
        BigDecimal usagePercentage,
        String status,
        OffsetDateTime measuredAt) {
}
