package com.coinmaster.energypulse.telemetry.service;

import com.coinmaster.energypulse.common.exception.ResourceNotFoundException;
import com.coinmaster.energypulse.home.domain.Appliance;
import com.coinmaster.energypulse.home.domain.Home;
import com.coinmaster.energypulse.home.repository.ApplianceRepository;
import com.coinmaster.energypulse.home.repository.HomeRepository;
import com.coinmaster.energypulse.telemetry.event.TelemetryEvent;
import com.coinmaster.energypulse.telemetry.repository.TelemetryJdbcRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class TelemetryProcessingService {

    private static final BigDecimal EIGHTY_PERCENT = new BigDecimal("0.80");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal MILLIS_PER_KILOWATT_HOUR =
            new BigDecimal("3600000000");

    private final HomeRepository homeRepository;
    private final ApplianceRepository applianceRepository;
    private final TelemetryJdbcRepository telemetryRepository;
    private final LiveTelemetryService liveTelemetryService;
    private final long defaultIntervalMillis;
    private final long maximumIntervalMillis;

    public TelemetryProcessingService(
            HomeRepository homeRepository,
            ApplianceRepository applianceRepository,
            TelemetryJdbcRepository telemetryRepository,
            LiveTelemetryService liveTelemetryService,
            @Value("${app.telemetry.default-interval-ms:2000}")
            long defaultIntervalMillis,
            @Value("${app.telemetry.maximum-interval-ms:300000}")
            long maximumIntervalMillis) {
        this.homeRepository = homeRepository;
        this.applianceRepository = applianceRepository;
        this.telemetryRepository = telemetryRepository;
        this.liveTelemetryService = liveTelemetryService;
        this.defaultIntervalMillis = Math.max(1, defaultIntervalMillis);
        this.maximumIntervalMillis = Math.max(
                this.defaultIntervalMillis,
                maximumIntervalMillis);
    }

    @Transactional
    public boolean process(TelemetryEvent event) {
        validate(event);

        if (telemetryRepository.isProcessed(event.eventId())) {
            return false;
        }

        Home home = homeRepository.findById(event.homeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "HOME_NOT_FOUND",
                        "Telemetry references an unknown home."));
        Appliance appliance = applianceRepository.findById(event.applianceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "APPLIANCE_NOT_FOUND",
                        "Telemetry references an unknown appliance."));

        if (!appliance.getHome().getId().equals(home.getId())) {
            throw new IllegalArgumentException(
                    "Telemetry appliance does not belong to the referenced home.");
        }

        LiveTelemetryUpdate liveUpdate = liveTelemetryService.recordTelemetry(
                event,
                home.getName(),
                appliance.getName(),
                appliance.getSafeLimitWatt(),
                home.getEnergyQuotaKwh(),
                home.getBudgetLimit(),
                home.getAccumulatedEnergyKwh(),
                home.getAccumulatedCost());

        if (!liveUpdate.accepted()) {
            return false;
        }

        persistUsage(event, home, liveUpdate.previousMeasurementAt());
        persistApplianceTransitions(
                event,
                appliance,
                liveUpdate.ruleEvaluation());
        return true;
    }

    private void persistUsage(
            TelemetryEvent event,
            Home home,
            OffsetDateTime previousMeasurementAt) {
        if (event.currentWatt().signum() == 0) {
            return;
        }

        BigDecimal previousEnergy = home.getAccumulatedEnergyKwh();
        BigDecimal previousCost = home.getAccumulatedCost();
        long intervalMillis = measurementInterval(
                previousMeasurementAt,
                event.occurredAt());
        BigDecimal energyDelta = event.currentWatt()
                .multiply(BigDecimal.valueOf(intervalMillis))
                .divide(MILLIS_PER_KILOWATT_HOUR, 9, RoundingMode.HALF_UP);
        BigDecimal currentEnergy = previousEnergy.add(energyDelta);
        boolean penaltyActive =
                currentEnergy.compareTo(home.getEnergyQuotaKwh()) >= 0;
        BigDecimal tariffRate = penaltyActive
                ? home.getPenaltyTariff()
                : home.getBaseTariff();
        BigDecimal costDelta = energyDelta
                .multiply(tariffRate)
                .setScale(9, RoundingMode.HALF_UP);
        BigDecimal currentCost = previousCost.add(costDelta);

        boolean inserted = telemetryRepository.insertBillingEntry(
                home.getId(),
                event.eventId(),
                energyDelta,
                tariffRate,
                costDelta,
                currentEnergy,
                currentCost,
                penaltyActive ? "PENALTY" : "NORMAL",
                event.occurredAt());

        if (!inserted) {
            return;
        }

        home.updateAccumulatedUsage(currentEnergy, currentCost);
        liveTelemetryService.updateAccumulatedUsage(
                home.getId(),
                currentEnergy,
                currentCost);
        persistQuotaTransitions(
                event,
                home,
                previousEnergy,
                currentEnergy);
    }

    private void persistQuotaTransitions(
            TelemetryEvent event,
            Home home,
            BigDecimal previousEnergy,
            BigDecimal currentEnergy) {
        BigDecimal quota = home.getEnergyQuotaKwh();
        BigDecimal eightyPercentThreshold = quota.multiply(EIGHTY_PERCENT);

        if (crossed(previousEnergy, currentEnergy, eightyPercentThreshold)) {
            telemetryRepository.insertQuotaEvent(
                    home.getId(),
                    event.eventId(),
                    "QUOTA_80_REACHED",
                    previousEnergy,
                    currentEnergy,
                    quota,
                    percentage(currentEnergy, quota),
                    event.occurredAt());
        }

        if (crossed(previousEnergy, currentEnergy, quota)) {
            BigDecimal usagePercentage = percentage(currentEnergy, quota);
            telemetryRepository.insertQuotaEvent(
                    home.getId(),
                    event.eventId(),
                    "QUOTA_100_REACHED",
                    previousEnergy,
                    currentEnergy,
                    quota,
                    usagePercentage,
                    event.occurredAt());
            telemetryRepository.insertQuotaEvent(
                    home.getId(),
                    event.eventId(),
                    "PENALTY_TARIFF_ACTIVATED",
                    previousEnergy,
                    currentEnergy,
                    quota,
                    usagePercentage,
                    event.occurredAt());
        }
    }

    private void persistApplianceTransitions(
            TelemetryEvent event,
            Appliance appliance,
            ApplianceRuleEvaluation evaluation) {
        if (evaluation.anomalyDetected()) {
            telemetryRepository.insertApplianceEvent(
                    event.homeId(),
                    appliance.getId(),
                    event.eventId(),
                    "APPLIANCE_ANOMALY_DETECTED",
                    event.currentWatt(),
                    appliance.getSafeLimitWatt(),
                    evaluation.consecutiveLimitBreaches(),
                    event.occurredAt());
        }

        if (evaluation.recovered()) {
            telemetryRepository.insertApplianceEvent(
                    event.homeId(),
                    appliance.getId(),
                    event.eventId(),
                    "APPLIANCE_RECOVERED",
                    event.currentWatt(),
                    appliance.getSafeLimitWatt(),
                    evaluation.consecutiveLimitBreaches(),
                    event.occurredAt());
        }
    }

    private long measurementInterval(
            OffsetDateTime previousMeasurementAt,
            OffsetDateTime currentMeasurementAt) {
        if (previousMeasurementAt == null) {
            return defaultIntervalMillis;
        }

        long measuredInterval = Duration.between(
                previousMeasurementAt,
                currentMeasurementAt).toMillis();

        if (measuredInterval <= 0 || measuredInterval > maximumIntervalMillis) {
            return defaultIntervalMillis;
        }

        return measuredInterval;
    }

    private boolean crossed(
            BigDecimal previousValue,
            BigDecimal currentValue,
            BigDecimal threshold) {
        return previousValue.compareTo(threshold) < 0
                && currentValue.compareTo(threshold) >= 0;
    }

    private BigDecimal percentage(
            BigDecimal currentValue,
            BigDecimal quota) {
        return currentValue
                .multiply(ONE_HUNDRED)
                .divide(quota, 2, RoundingMode.HALF_UP);
    }

    private void validate(TelemetryEvent event) {
        if (event == null
                || event.eventId() == null
                || event.occurredAt() == null
                || event.homeId() == null
                || event.applianceId() == null
                || event.currentWatt() == null) {
            throw new IllegalArgumentException(
                    "Telemetry event fields must not be null.");
        }

        if (event.schemaVersion() != 1) {
            throw new IllegalArgumentException(
                    "Unsupported telemetry schema version.");
        }

        if (event.currentWatt().signum() < 0) {
            throw new IllegalArgumentException(
                    "Telemetry current watt must be non-negative.");
        }
    }
}
