package com.coinmaster.energypulse.telemetry.service;

import com.coinmaster.energypulse.home.domain.Appliance;
import com.coinmaster.energypulse.home.domain.Home;
import com.coinmaster.energypulse.home.repository.ApplianceRepository;
import com.coinmaster.energypulse.home.repository.HomeRepository;
import com.coinmaster.energypulse.telemetry.event.TelemetryEvent;
import com.coinmaster.energypulse.telemetry.repository.TelemetryJdbcRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryProcessingServiceTest {

    private final HomeRepository homeRepository = mock(HomeRepository.class);
    private final ApplianceRepository applianceRepository =
            mock(ApplianceRepository.class);
    private final TelemetryJdbcRepository telemetryRepository =
            mock(TelemetryJdbcRepository.class);
    private final LiveTelemetryService liveTelemetryService =
            mock(LiveTelemetryService.class);
    private final TelemetryProcessingService service =
            new TelemetryProcessingService(
                    homeRepository,
                    applianceRepository,
                    telemetryRepository,
                    liveTelemetryService,
                    2000,
                    300000);

    @Test
    void persistsUsageAndTheEightyPercentTransition() {
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        Home home = mock(Home.class);
        Appliance appliance = mock(Appliance.class);
        TelemetryEvent event = new TelemetryEvent(
                UUID.randomUUID(),
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                homeId,
                applianceId,
                new BigDecimal("180000"));

        when(homeRepository.findById(homeId)).thenReturn(Optional.of(home));
        when(applianceRepository.findById(applianceId))
                .thenReturn(Optional.of(appliance));
        when(home.getId()).thenReturn(homeId);
        when(home.getName()).thenReturn("Test home");
        when(home.getAccumulatedEnergyKwh())
                .thenReturn(new BigDecimal("79.9"));
        when(home.getAccumulatedCost()).thenReturn(BigDecimal.ZERO);
        when(home.getEnergyQuotaKwh()).thenReturn(new BigDecimal("100"));
        when(home.getBaseTariff()).thenReturn(new BigDecimal("2.5"));
        when(home.getPenaltyTariff()).thenReturn(new BigDecimal("4"));
        when(appliance.getId()).thenReturn(applianceId);
        when(appliance.getHome()).thenReturn(home);
        when(appliance.getName()).thenReturn("Heater");
        when(appliance.getSafeLimitWatt()).thenReturn(new BigDecimal("200000"));
        when(liveTelemetryService.recordTelemetry(
                eq(event),
                eq("Test home"),
                eq("Heater"),
                eq(new BigDecimal("200000"))))
                .thenReturn(new LiveTelemetryUpdate(
                        true,
                        null,
                        new ApplianceRuleEvaluation(
                                new BigDecimal("90"),
                                "WARNING",
                                0,
                                false,
                                false)));
        when(telemetryRepository.insertBillingEntry(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(true);

        boolean processed = service.process(event);

        assertThat(processed).isTrue();
        verify(home).updateAccumulatedUsage(
                new BigDecimal("80.000000000"),
                new BigDecimal("0.250000000"));
        verify(telemetryRepository).insertQuotaEvent(
                eq(homeId),
                eq(event.eventId()),
                eq("QUOTA_80_REACHED"),
                eq(new BigDecimal("79.9")),
                eq(new BigDecimal("80.000000000")),
                eq(new BigDecimal("100")),
                eq(new BigDecimal("80.00")),
                eq(event.occurredAt()));
    }

    @Test
    void skipsAnEventAlreadyPresentInTheBillingLedger() {
        TelemetryEvent event = new TelemetryEvent(
                UUID.randomUUID(),
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ONE);
        when(telemetryRepository.isProcessed(event.eventId()))
                .thenReturn(true);

        boolean processed = service.process(event);

        assertThat(processed).isFalse();
        verify(homeRepository, never()).findById(any());
        verify(liveTelemetryService, never()).recordTelemetry(
                any(),
                any(),
                any(),
                any());
    }
}
