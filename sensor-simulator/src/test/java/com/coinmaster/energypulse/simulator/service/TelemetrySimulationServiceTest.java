package com.coinmaster.energypulse.simulator.service;

import com.coinmaster.energypulse.simulator.event.ApplianceRegistrationEvent;
import com.coinmaster.energypulse.simulator.event.HomeRegistrationEvent;
import com.coinmaster.energypulse.simulator.event.TelemetryEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TelemetrySimulationServiceTest {

    @Test
    void publishesTelemetryWithinTheConfiguredSimulationRange() {
        HomeStorageService storage = new HomeStorageService();
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        UUID homeId = UUID.randomUUID();
        UUID applianceId = UUID.randomUUID();
        storage.addHome(new HomeRegistrationEvent(
                UUID.randomUUID(),
                1,
                OffsetDateTime.now(),
                homeId,
                "Test home",
                List.of(new ApplianceRegistrationEvent(
                        applianceId,
                        "Kettle",
                        new BigDecimal("2000"),
                        new BigDecimal("1000"),
                        new BigDecimal("1500")))));

        TelemetrySimulationService service = new TelemetrySimulationService(
                storage,
                kafkaTemplate,
                "telemetry-topic",
                new Random(42));

        service.generateAndSendTelemetry();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("telemetry-topic"),
                org.mockito.ArgumentMatchers.eq(homeId.toString()),
                eventCaptor.capture());

        TelemetryEvent event = (TelemetryEvent) eventCaptor.getValue();
        assertThat(event.homeId()).isEqualTo(homeId);
        assertThat(event.applianceId()).isEqualTo(applianceId);
        assertThat(event.currentWatt())
                .isBetween(new BigDecimal("1000"), new BigDecimal("1500"));
    }
}
