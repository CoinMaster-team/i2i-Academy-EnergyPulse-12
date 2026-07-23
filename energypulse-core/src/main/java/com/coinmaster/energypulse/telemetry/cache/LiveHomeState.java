package com.coinmaster.energypulse.telemetry.cache;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LiveHomeState(
        UUID homeId,
        String name,
        BigDecimal totalPowerWatts,
        String status,
        List<LiveApplianceState> appliances,
        OffsetDateTime updatedAt) implements Serializable {

    public LiveHomeState {
        appliances = List.copyOf(appliances);
    }
}
