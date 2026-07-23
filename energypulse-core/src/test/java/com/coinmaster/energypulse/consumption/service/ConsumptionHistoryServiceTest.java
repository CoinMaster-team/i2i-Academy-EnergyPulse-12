package com.coinmaster.energypulse.consumption.service;

import com.coinmaster.energypulse.common.exception.BusinessRuleException;
import com.coinmaster.energypulse.common.exception.ResourceNotFoundException;
import com.coinmaster.energypulse.consumption.domain.ConsumptionSnapshot;
import com.coinmaster.energypulse.consumption.dto.DailyConsumptionResponse;
import com.coinmaster.energypulse.consumption.repository.ConsumptionSnapshotRepository;
import com.coinmaster.energypulse.home.domain.Home;
import com.coinmaster.energypulse.home.repository.HomeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumptionHistoryServiceTest {

    private static final UUID HOME_ID = UUID.fromString("191858ab-328d-45be-bfda-883e9f9bc2a2");

    @Mock
    private ConsumptionSnapshotRepository snapshotRepository;

    @Mock
    private HomeRepository homeRepository;

    @InjectMocks
    private ConsumptionHistoryService consumptionHistoryService;

    @Test
    void shouldReturnLastSnapshotOfEachDay() {
        Home home = mock(Home.class);

        List<ConsumptionSnapshot> snapshots = List.of(
                snapshot(
                        home,
                        "12.500000",
                        "31.250000",
                        "2026-07-19T18:00:00Z"),
                snapshot(
                        home,
                        "27.800000",
                        "69.500000",
                        "2026-07-20T18:00:00Z"),
                snapshot(
                        home,
                        "32.000000",
                        "80.000000",
                        "2026-07-21T08:00:00Z"),
                snapshot(
                        home,
                        "35.100000",
                        "87.750000",
                        "2026-07-21T18:00:00Z"));

        when(homeRepository.existsById(HOME_ID))
                .thenReturn(true);

        when(snapshotRepository
                .findAllByHome_IdAndCapturedAtGreaterThanEqualAndCapturedAtLessThanOrderByCapturedAtAsc(
                        HOME_ID,
                        OffsetDateTime.parse("2026-07-19T00:00:00Z"),
                        OffsetDateTime.parse("2026-07-22T00:00:00Z")))
                .thenReturn(snapshots);

        List<DailyConsumptionResponse> response = consumptionHistoryService.getDailyHistory(
                HOME_ID,
                LocalDate.parse("2026-07-19"),
                LocalDate.parse("2026-07-21"));

        assertEquals(3, response.size());
        assertEquals(
                LocalDate.parse("2026-07-21"),
                response.get(2).date());
        assertEquals(
                new BigDecimal("35.100000"),
                response.get(2).totalEnergyKwh());
        assertEquals(
                new BigDecimal("87.750000"),
                response.get(2).totalCost());
    }

    @Test
    void shouldRejectInvalidDateRange() {
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> consumptionHistoryService.getDailyHistory(
                        HOME_ID,
                        LocalDate.parse("2026-07-21"),
                        LocalDate.parse("2026-07-19")));

        assertEquals("INVALID_DATE_RANGE", exception.getCode());

        verifyNoInteractions(homeRepository, snapshotRepository);
    }

    @Test
    void shouldRejectDateRangeLargerThanThirtyOneDays() {
        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> consumptionHistoryService.getDailyHistory(
                        HOME_ID,
                        LocalDate.parse("2026-07-01"),
                        LocalDate.parse("2026-08-02")));

        assertEquals("DATE_RANGE_TOO_LARGE", exception.getCode());

        verifyNoInteractions(homeRepository, snapshotRepository);
    }

    @Test
    void shouldReturnNotFoundWhenHomeDoesNotExist() {
        when(homeRepository.existsById(HOME_ID))
                .thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> consumptionHistoryService.getDailyHistory(
                        HOME_ID,
                        LocalDate.parse("2026-07-19"),
                        LocalDate.parse("2026-07-21")));

        assertEquals("HOME_NOT_FOUND", exception.getCode());

        verifyNoInteractions(snapshotRepository);
    }

    private ConsumptionSnapshot snapshot(
            Home home,
            String totalEnergyKwh,
            String totalCost,
            String capturedAt) {
        return new ConsumptionSnapshot(
                home,
                new BigDecimal(totalEnergyKwh),
                new BigDecimal(totalCost),
                OffsetDateTime.parse(capturedAt));
    }
}