package com.coinmaster.energypulse.telemetry.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyRuleEngineServiceTest {

    private final EnergyRuleEngineService service =
            new EnergyRuleEngineService();

    @Test
    void warnsAtEightyPercentWithoutMarkingAnAnomaly() {
        ApplianceRuleEvaluation evaluation = service.evaluate(
                new BigDecimal("800"),
                new BigDecimal("1000"),
                0,
                false);

        assertThat(evaluation.status()).isEqualTo("WARNING");
        assertThat(evaluation.usagePercentage())
                .isEqualByComparingTo("80.00");
        assertThat(evaluation.anomalyDetected()).isFalse();
    }

    @Test
    void detectsAnAnomalyAfterThreeConsecutiveLimitBreaches() {
        ApplianceRuleEvaluation evaluation = service.evaluate(
                new BigDecimal("1200"),
                new BigDecimal("1000"),
                2,
                false);

        assertThat(evaluation.status()).isEqualTo("ANOMALY");
        assertThat(evaluation.consecutiveLimitBreaches()).isEqualTo(3);
        assertThat(evaluation.anomalyDetected()).isTrue();
    }

    @Test
    void marksAnAnomalousApplianceAsRecoveredBelowItsLimit() {
        ApplianceRuleEvaluation evaluation = service.evaluate(
                new BigDecimal("700"),
                new BigDecimal("1000"),
                4,
                true);

        assertThat(evaluation.status()).isEqualTo("NORMAL");
        assertThat(evaluation.consecutiveLimitBreaches()).isZero();
        assertThat(evaluation.recovered()).isTrue();
    }
}
