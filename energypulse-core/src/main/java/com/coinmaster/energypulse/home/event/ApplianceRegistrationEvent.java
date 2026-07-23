package com.coinmaster.energypulse.home.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplianceRegistrationEvent(
        UUID applianceId,
        String name,
        BigDecimal safeLimitWatt,
        BigDecimal simulationMinWatt,
        BigDecimal simulationMaxWatt) {
}