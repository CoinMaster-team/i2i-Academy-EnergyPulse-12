package com.coinmaster.energypulse.telemetry.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class EnergyRuleEngineService {

    private static final BigDecimal WARNING_PERCENTAGE = new BigDecimal("80");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int ANOMALY_BREACH_COUNT = 3;

    public ApplianceRuleEvaluation evaluate(
            BigDecimal currentWatt,
            BigDecimal safeLimitWatt,
            int previousConsecutiveBreaches,
            boolean previouslyAnomalous) {
        if (currentWatt == null || currentWatt.signum() < 0) {
            throw new IllegalArgumentException("Current watt must be non-negative.");
        }

        if (safeLimitWatt == null || safeLimitWatt.signum() <= 0) {
            throw new IllegalArgumentException("Safe watt limit must be positive.");
        }

        BigDecimal usagePercentage = currentWatt
                .multiply(ONE_HUNDRED)
                .divide(safeLimitWatt, 2, RoundingMode.HALF_UP);
        boolean limitExceeded = currentWatt.compareTo(safeLimitWatt) > 0;
        int consecutiveBreaches = limitExceeded
                ? previousConsecutiveBreaches + 1
                : 0;
        boolean anomalous = limitExceeded
                && (previouslyAnomalous
                || consecutiveBreaches >= ANOMALY_BREACH_COUNT);

        String status;
        if (anomalous) {
            status = "ANOMALY";
        } else if (usagePercentage.compareTo(WARNING_PERCENTAGE) >= 0) {
            status = "WARNING";
        } else {
            status = "NORMAL";
        }

        return new ApplianceRuleEvaluation(
                usagePercentage,
                status,
                consecutiveBreaches,
                !previouslyAnomalous && anomalous,
                previouslyAnomalous && !limitExceeded);
    }
}
