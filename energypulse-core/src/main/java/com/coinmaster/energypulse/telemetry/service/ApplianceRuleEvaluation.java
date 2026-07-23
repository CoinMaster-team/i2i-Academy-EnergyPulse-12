package com.coinmaster.energypulse.telemetry.service;

import java.math.BigDecimal;

public record ApplianceRuleEvaluation(
        BigDecimal usagePercentage,
        String status,
        int consecutiveLimitBreaches,
        boolean anomalyDetected,
        boolean recovered) {
}
