package com.coinmaster.energypulse.telemetry.service;

import java.time.OffsetDateTime;

public record LiveTelemetryUpdate(
        boolean accepted,
        OffsetDateTime previousMeasurementAt,
        ApplianceRuleEvaluation ruleEvaluation) {

    public static LiveTelemetryUpdate ignored() {
        return new LiveTelemetryUpdate(false, null, null);
    }
}
