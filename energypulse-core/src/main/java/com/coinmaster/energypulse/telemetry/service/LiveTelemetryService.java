package com.coinmaster.energypulse.telemetry.service;

import com.coinmaster.energypulse.home.event.ApplianceRegistrationEvent;
import com.coinmaster.energypulse.home.event.HomeRegistrationEvent;
import com.coinmaster.energypulse.telemetry.cache.LiveApplianceState;
import com.coinmaster.energypulse.telemetry.cache.LiveHomeState;
import com.coinmaster.energypulse.telemetry.cache.LiveTelemetryStore;
import com.coinmaster.energypulse.telemetry.dto.LiveApplianceResponse;
import com.coinmaster.energypulse.telemetry.dto.LiveHomeResponse;
import com.coinmaster.energypulse.telemetry.dto.LiveTelemetryResponse;
import com.coinmaster.energypulse.telemetry.event.TelemetryEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class LiveTelemetryService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final LiveTelemetryStore store;
    private final EnergyRuleEngineService ruleEngine;

    public LiveTelemetryService(
            LiveTelemetryStore store,
            EnergyRuleEngineService ruleEngine) {
        this.store = store;
        this.ruleEngine = ruleEngine;
    }

    public void registerHome(HomeRegistrationEvent event) {
        LiveHomeState existing = store.findHome(event.homeId()).orElse(null);
        List<LiveApplianceState> appliances = event.appliances()
                .stream()
                .map(appliance -> mergeAppliance(existing, appliance))
                .sorted(Comparator.comparing(LiveApplianceState::name))
                .toList();

        store.save(new LiveHomeState(
                event.homeId(),
                event.homeName(),
                totalPower(appliances),
                homeStatus(appliances),
                appliances,
                existing == null ? event.occurredAt() : existing.updatedAt()));
    }

    public LiveTelemetryUpdate recordTelemetry(
            TelemetryEvent event,
            String homeName,
            String applianceName,
            BigDecimal safeLimitWatt) {
        LiveHomeState home = store.findHome(event.homeId())
                .orElseGet(() -> emptyHome(event.homeId(), homeName));
        List<LiveApplianceState> appliances = new ArrayList<>(home.appliances());
        int applianceIndex = findApplianceIndex(appliances, event.applianceId());
        LiveApplianceState previous = applianceIndex >= 0
                ? appliances.get(applianceIndex)
                : LiveApplianceState.idle(
                        event.applianceId(),
                        applianceName,
                        safeLimitWatt);

        if (event.eventId().equals(previous.lastEventId())
                || isOlderThanCurrentMeasurement(event, previous)) {
            return LiveTelemetryUpdate.ignored();
        }

        ApplianceRuleEvaluation evaluation = ruleEngine.evaluate(
                event.currentWatt(),
                safeLimitWatt,
                previous.consecutiveLimitBreaches(),
                "ANOMALY".equals(previous.status()));
        LiveApplianceState updated = new LiveApplianceState(
                event.applianceId(),
                applianceName,
                safeLimitWatt,
                event.currentWatt(),
                evaluation.status(),
                evaluation.consecutiveLimitBreaches(),
                event.eventId(),
                event.occurredAt());

        if (applianceIndex >= 0) {
            appliances.set(applianceIndex, updated);
        } else {
            appliances.add(updated);
        }

        appliances.sort(Comparator.comparing(LiveApplianceState::name));
        store.save(new LiveHomeState(
                event.homeId(),
                homeName,
                totalPower(appliances),
                homeStatus(appliances),
                appliances,
                event.occurredAt()));

        return new LiveTelemetryUpdate(
                true,
                previous.measuredAt(),
                evaluation);
    }

    public LiveTelemetryResponse getLiveTelemetry() {
        List<LiveHomeResponse> homes = store.findAll()
                .stream()
                .sorted(Comparator.comparing(LiveHomeState::name))
                .map(this::toResponse)
                .toList();

        return new LiveTelemetryResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                homes);
    }

    private LiveApplianceState mergeAppliance(
            LiveHomeState existing,
            ApplianceRegistrationEvent appliance) {
        if (existing == null) {
            return LiveApplianceState.idle(
                    appliance.applianceId(),
                    appliance.name(),
                    appliance.safeLimitWatt());
        }

        return existing.appliances()
                .stream()
                .filter(current -> current.applianceId().equals(appliance.applianceId()))
                .findFirst()
                .map(current -> new LiveApplianceState(
                        current.applianceId(),
                        appliance.name(),
                        appliance.safeLimitWatt(),
                        current.currentWatt(),
                        current.status(),
                        current.consecutiveLimitBreaches(),
                        current.lastEventId(),
                        current.measuredAt()))
                .orElseGet(() -> LiveApplianceState.idle(
                        appliance.applianceId(),
                        appliance.name(),
                        appliance.safeLimitWatt()));
    }

    private LiveHomeState emptyHome(UUID homeId, String homeName) {
        return new LiveHomeState(
                homeId,
                homeName,
                BigDecimal.ZERO,
                "NORMAL",
                List.of(),
                null);
    }

    private int findApplianceIndex(
            List<LiveApplianceState> appliances,
            UUID applianceId) {
        for (int index = 0; index < appliances.size(); index++) {
            if (appliances.get(index).applianceId().equals(applianceId)) {
                return index;
            }
        }

        return -1;
    }

    private boolean isOlderThanCurrentMeasurement(
            TelemetryEvent event,
            LiveApplianceState previous) {
        return previous.measuredAt() != null
                && event.occurredAt().isBefore(previous.measuredAt());
    }

    private BigDecimal totalPower(List<LiveApplianceState> appliances) {
        return appliances.stream()
                .map(LiveApplianceState::currentWatt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String homeStatus(List<LiveApplianceState> appliances) {
        if (appliances.stream().anyMatch(
                appliance -> "ANOMALY".equals(appliance.status()))) {
            return "ANOMALY";
        }

        if (appliances.stream().anyMatch(
                appliance -> "WARNING".equals(appliance.status()))) {
            return "WARNING";
        }

        return "NORMAL";
    }

    private LiveHomeResponse toResponse(LiveHomeState home) {
        List<LiveApplianceResponse> appliances = home.appliances()
                .stream()
                .map(this::toResponse)
                .toList();

        return new LiveHomeResponse(
                home.homeId(),
                home.name(),
                home.totalPowerWatts(),
                home.status(),
                appliances,
                home.updatedAt());
    }

    private LiveApplianceResponse toResponse(LiveApplianceState appliance) {
        BigDecimal percentage = appliance.safeLimitWatt().signum() == 0
                ? BigDecimal.ZERO
                : appliance.currentWatt()
                        .multiply(ONE_HUNDRED)
                        .divide(appliance.safeLimitWatt(), 2, RoundingMode.HALF_UP);

        return new LiveApplianceResponse(
                appliance.applianceId(),
                appliance.name(),
                appliance.safeLimitWatt(),
                appliance.currentWatt(),
                percentage,
                appliance.status(),
                appliance.measuredAt());
    }
}
