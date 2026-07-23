package com.coinmaster.energypulse.consumption.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyConsumptionResponse(
        LocalDate date,
        BigDecimal totalEnergyKwh,
        BigDecimal totalCost) {
}
