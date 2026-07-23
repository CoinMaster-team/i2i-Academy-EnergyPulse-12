package com.coinmaster.energypulse.telemetry.cache;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LiveApplianceState(
        UUID applianceId,
        String name,
        BigDecimal safeLimitWatt,
        BigDecimal currentWatt,
        String status,
        int consecutiveLimitBreaches,
        UUID lastEventId,
        OffsetDateTime measuredAt) implements Serializable {

    public static LiveApplianceState idle(
            UUID applianceId,
            String name,
            BigDecimal safeLimitWatt) {
        return new LiveApplianceState(
                applianceId,
                name,
                safeLimitWatt,
                BigDecimal.ZERO,
                "NORMAL",
                0,
                null,
                null);
    }
}
