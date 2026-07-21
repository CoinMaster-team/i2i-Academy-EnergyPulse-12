package com.coinmaster.energypulse.home.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplianceResponse(
        UUID id,
        String name,
        BigDecimal safeLimitWatt,
        BigDecimal simulationMinWatt,
        BigDecimal simulationMaxWatt) {
}
