package com.coinmaster.energypulse.consumption.service;

import com.coinmaster.energypulse.common.exception.BusinessRuleException;
import com.coinmaster.energypulse.common.exception.ResourceNotFoundException;
import com.coinmaster.energypulse.consumption.domain.ConsumptionSnapshot;
import com.coinmaster.energypulse.consumption.dto.DailyConsumptionResponse;
import com.coinmaster.energypulse.consumption.repository.ConsumptionSnapshotRepository;
import com.coinmaster.energypulse.home.repository.HomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConsumptionHistoryService {

    private final ConsumptionSnapshotRepository snapshotRepository;
    private final HomeRepository homeRepository;

    public ConsumptionHistoryService(
            ConsumptionSnapshotRepository snapshotRepository,
            HomeRepository homeRepository) {
        this.snapshotRepository = snapshotRepository;
        this.homeRepository = homeRepository;
    }

    @Transactional(readOnly = true)
    public List<DailyConsumptionResponse> getDailyHistory(
            UUID homeId,
            LocalDate from,
            LocalDate to) {
        validateDateRange(from, to);

        if (!homeRepository.existsById(homeId)) {
            throw new ResourceNotFoundException(
                    "HOME_NOT_FOUND",
                    "Home not found: " + homeId);
        }

        OffsetDateTime rangeStart = from.minusDays(1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        OffsetDateTime rangeEnd = to.plusDays(1)
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);

        List<ConsumptionSnapshot> snapshots = snapshotRepository
                .findAllByHome_IdAndCapturedAtGreaterThanEqualAndCapturedAtLessThanOrderByCapturedAtAsc(
                        homeId,
                        rangeStart,
                        rangeEnd);

        Map<LocalDate, ConsumptionSnapshot> lastSnapshotByDay = new LinkedHashMap<>();

        for (ConsumptionSnapshot snapshot : snapshots) {
            LocalDate snapshotDate = snapshot.getCapturedAt()
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDate();

            lastSnapshotByDay.put(snapshotDate, snapshot);
        }

        ConsumptionSnapshot previousSnapshot = lastSnapshotByDay.get(from.minusDays(1));
        List<DailyConsumptionResponse> response = new java.util.ArrayList<>();

        boolean hasRequestedHistory = lastSnapshotByDay.keySet().stream()
                .anyMatch(date -> !date.isBefore(from) && !date.isAfter(to));
        if (!hasRequestedHistory) {
            return response;
        }

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            ConsumptionSnapshot currentSnapshot = lastSnapshotByDay.get(date);
            if (currentSnapshot == null) {
                BigDecimal totalEnergy = previousSnapshot == null
                        ? BigDecimal.ZERO
                        : previousSnapshot.getTotalEnergyKwh();
                BigDecimal totalCost = previousSnapshot == null
                        ? BigDecimal.ZERO
                        : previousSnapshot.getTotalCost();

                response.add(new DailyConsumptionResponse(
                        date,
                        totalEnergy,
                        totalCost,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO));
                continue;
            }

            BigDecimal previousEnergy = previousSnapshot == null
                    ? BigDecimal.ZERO
                    : previousSnapshot.getTotalEnergyKwh();
            BigDecimal previousCost = previousSnapshot == null
                    ? BigDecimal.ZERO
                    : previousSnapshot.getTotalCost();

            response.add(new DailyConsumptionResponse(
                    date,
                    currentSnapshot.getTotalEnergyKwh(),
                    currentSnapshot.getTotalCost(),
                    nonNegativeDifference(
                            currentSnapshot.getTotalEnergyKwh(),
                            previousEnergy),
                    nonNegativeDifference(
                            currentSnapshot.getTotalCost(),
                            previousCost)));
            previousSnapshot = currentSnapshot;
        }

        return response;
    }

    private BigDecimal nonNegativeDifference(
            BigDecimal current,
            BigDecimal previous) {
        return current.subtract(previous).max(BigDecimal.ZERO);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException(
                    "INVALID_DATE_RANGE",
                    "'to' date must be equal to or later than 'from' date.");
        }

        if (from.plusDays(31).isBefore(to)) {
            throw new BusinessRuleException(
                    "DATE_RANGE_TOO_LARGE",
                    "Consumption history can be requested for at most 31 days.");
        }
    }
}
