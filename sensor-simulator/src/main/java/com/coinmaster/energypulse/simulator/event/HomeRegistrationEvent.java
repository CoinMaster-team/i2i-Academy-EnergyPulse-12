package com.coinmaster.energypulse.simulator.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record HomeRegistrationEvent(
        UUID eventId,
        int schemaVersion,
        OffsetDateTime occurredAt,
        UUID homeId,
        String homeName,
        BigDecimal energyQuotaKwh,
        BigDecimal budgetLimit,
        BigDecimal accumulatedEnergyKwh,
        BigDecimal accumulatedCost,
        List<ApplianceRegistrationEvent> appliances) {

    public HomeRegistrationEvent {
        appliances = List.copyOf(appliances);
    }
}
