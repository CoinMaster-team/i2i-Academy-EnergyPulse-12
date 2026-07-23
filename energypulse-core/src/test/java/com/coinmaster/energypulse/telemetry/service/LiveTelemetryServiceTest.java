package com.coinmaster.energypulse.telemetry.service;

import com.coinmaster.energypulse.home.event.ApplianceRegistrationEvent;
import com.coinmaster.energypulse.home.event.HomeRegistrationEvent;
import com.coinmaster.energypulse.telemetry.cache.InMemoryLiveTelemetryStore;
import com.coinmaster.energypulse.telemetry.dto.LiveHomeResponse;
import com.coinmaster.energypulse.telemetry.event.TelemetryEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LiveTelemetryServiceTest {

    private final LiveTelemetryService service = new LiveTelemetryService(
            new InMemoryLiveTelemetryStore(),
            new EnergyRuleEngineService());

    @Test
    void keepsLiveStateAndEmitsAnomalyAndRecoveryTransitions() {
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        service.registerHome(new HomeRegistrationEvent(
                UUID.randomUUID(),
                1,
                startedAt,
                homeId,
                "Test home",
                List.of(new ApplianceRegistrationEvent(
                        applianceId,
                        "Kettle",
                        new BigDecimal("1000"),
                        new BigDecimal("500"),
                        new BigDecimal("1300")))));

        LiveTelemetryUpdate first = record(
                homeId,
                applianceId,
                "1100",
                startedAt.plusSeconds(2));
        LiveTelemetryUpdate second = record(
                homeId,
                applianceId,
                "1150",
                startedAt.plusSeconds(4));
        LiveTelemetryUpdate third = record(
                homeId,
                applianceId,
                "1200",
                startedAt.plusSeconds(6));

        assertThat(first.ruleEvaluation().anomalyDetected()).isFalse();
        assertThat(second.ruleEvaluation().anomalyDetected()).isFalse();
        assertThat(third.ruleEvaluation().anomalyDetected()).isTrue();

        LiveHomeResponse anomalousHome = service.getLiveTelemetry()
                .homes()
                .get(0);
        assertThat(anomalousHome.status()).isEqualTo("ANOMALY");
        assertThat(anomalousHome.totalPowerWatts())
                .isEqualByComparingTo("1200");

        LiveTelemetryUpdate recovered = record(
                homeId,
                applianceId,
                "700",
                startedAt.plusSeconds(8));

        assertThat(recovered.ruleEvaluation().recovered()).isTrue();
        assertThat(service.getLiveTelemetry().homes().get(0).status())
                .isEqualTo("NORMAL");
    }

    @Test
    void ignoresAnAlreadyRecordedEvent() {
        UUID eventId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        OffsetDateTime measuredAt = OffsetDateTime.now(ZoneOffset.UTC);
        TelemetryEvent event = new TelemetryEvent(
                eventId,
                1,
                measuredAt,
                homeId,
                applianceId,
                new BigDecimal("250"));

        LiveTelemetryUpdate first = service.recordTelemetry(
                event,
                "Home",
                "Device",
                new BigDecimal("500"));
        LiveTelemetryUpdate duplicate = service.recordTelemetry(
                event,
                "Home",
                "Device",
                new BigDecimal("500"));

        assertThat(first.accepted()).isTrue();
        assertThat(duplicate.accepted()).isFalse();
    }

    private LiveTelemetryUpdate record(
            UUID homeId,
            UUID applianceId,
            String currentWatt,
            OffsetDateTime occurredAt) {
        return service.recordTelemetry(
                new TelemetryEvent(
                        UUID.randomUUID(),
                        1,
                        occurredAt,
                        homeId,
                        applianceId,
                        new BigDecimal(currentWatt)),
                "Test home",
                "Kettle",
                new BigDecimal("1000"));
    }
}
