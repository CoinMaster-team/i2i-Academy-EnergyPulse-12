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

        OffsetDateTime rangeStart = from.atStartOfDay().atOffset(ZoneOffset.UTC);

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

        return lastSnapshotByDay.entrySet()
                .stream()
                .map(entry -> new DailyConsumptionResponse(
                        entry.getKey(),
                        entry.getValue().getTotalEnergyKwh(),
                        entry.getValue().getTotalCost()))
                .toList();
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