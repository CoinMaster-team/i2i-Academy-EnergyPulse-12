package com.coinmaster.energypulse.telemetry.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LiveHomeResponse(
        UUID homeId,
        String name,
        BigDecimal energyQuotaKwh,
        BigDecimal budgetLimit,
        BigDecimal accumulatedEnergyKwh,
        BigDecimal accumulatedCost,
        BigDecimal quotaPercentage,
        BigDecimal budgetPercentage,
        BigDecimal totalPowerWatts,
        String status,
        List<LiveApplianceResponse> appliances,
        OffsetDateTime updatedAt) {

    public LiveHomeResponse {
        appliances = List.copyOf(appliances);
    }
}
