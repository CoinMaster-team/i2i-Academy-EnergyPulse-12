package com.coinmaster.energypulse.simulator.service;

import com.coinmaster.energypulse.simulator.event.ApplianceRegistrationEvent;
import com.coinmaster.energypulse.simulator.event.HomeRegistrationEvent;
import com.coinmaster.energypulse.simulator.event.TelemetryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.UUID;

@Service
public class TelemetrySimulationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TelemetrySimulationService.class);

    private final HomeStorageService homeStorageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String telemetryTopic;
    private final Random random;

    @Autowired
    public TelemetrySimulationService(
            HomeStorageService homeStorageService,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.telemetry}") String telemetryTopic) {
        this(homeStorageService, kafkaTemplate, telemetryTopic, new Random());
    }

    TelemetrySimulationService(
            HomeStorageService homeStorageService,
            KafkaTemplate<String, Object> kafkaTemplate,
            String telemetryTopic,
            Random random) {
        this.homeStorageService = homeStorageService;
        this.kafkaTemplate = kafkaTemplate;
        this.telemetryTopic = telemetryTopic;
        this.random = random;
    }

    @Scheduled(fixedRateString = "${app.simulator.interval-ms:2000}")
    public void generateAndSendTelemetry() {
        for (HomeRegistrationEvent home : homeStorageService.getAllHomes()) {
            for (ApplianceRegistrationEvent appliance : home.appliances()) {
                TelemetryEvent event = createTelemetryEvent(home, appliance);

                kafkaTemplate.send(
                        telemetryTopic,
                        home.homeId().toString(),
                        event);

                LOGGER.debug(
                        "Telemetry sent. eventId={}, homeId={}, applianceId={}, currentWatt={}",
                        event.eventId(),
                        event.homeId(),
                        event.applianceId(),
                        event.currentWatt());
            }
        }
    }

    private TelemetryEvent createTelemetryEvent(
            HomeRegistrationEvent home,
            ApplianceRegistrationEvent appliance) {
        BigDecimal range = appliance.simulationMaxWatt()
                .subtract(appliance.simulationMinWatt());
        BigDecimal randomOffset = range.multiply(BigDecimal.valueOf(random.nextDouble()));
        BigDecimal currentWatt = appliance.simulationMinWatt()
                .add(randomOffset)
                .setScale(2, RoundingMode.HALF_UP);

        return new TelemetryEvent(
                UUID.randomUUID(),
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                home.homeId(),
                appliance.applianceId(),
                currentWatt);
    }
}
