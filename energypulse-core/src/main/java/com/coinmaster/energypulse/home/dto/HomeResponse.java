package com.coinmaster.energypulse.home.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record HomeResponse(
        UUID id,
        String name,
        String contactEmail,
        BigDecimal energyQuotaKwh,
        BigDecimal budgetLimit,
        BigDecimal baseTariff,
        BigDecimal penaltyTariff,
        BigDecimal accumulatedEnergyKwh,
        BigDecimal accumulatedCost,
        List<ApplianceResponse> appliances,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}