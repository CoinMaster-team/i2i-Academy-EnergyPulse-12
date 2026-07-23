package com.voltwise.sensor_simulator.service;

import com.voltwise.sensor_simulator.dto.ApplianceDTO;
import com.voltwise.sensor_simulator.dto.HomeRegistrationEvent;
import com.voltwise.sensor_simulator.dto.TelemetryEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class TelemetrySimulationService {

    private final HomeStorageService homeStorageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    // Constructor Injection (Dependency Injection)
    public TelemetrySimulationService(HomeStorageService homeStorageService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.homeStorageService = homeStorageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Run this method repeatedly every 2000 milliseconds (2 seconds)
    @Scheduled(fixedRate = 2000)
    public void generateAndSendTelemetry() {
        // 1. Get all registered homes from memory
        List<HomeRegistrationEvent> homes = homeStorageService.getAllHomes();

        // 2. Loop through each home and its appliances
        for (HomeRegistrationEvent home : homes) {
            for (ApplianceDTO appliance : home.getAppliances()) {

                // 3. Generate a random wattage between min and max limits
                double min = appliance.getSimulationMinWatt();
                double max = appliance.getSimulationMaxWatt();
                double randomWatt = min + (max - min) * random.nextDouble();

                // 4. Create the telemetry event payload
                TelemetryEvent event = new TelemetryEvent();
                event.setHomeId(home.getHomeId());
                event.setApplianceId(appliance.getId());
                // Keep only 2 decimal places for realism
                event.setCurrentWatt(Math.round(randomWatt * 100.0) / 100.0);
                event.setTimestamp(System.currentTimeMillis());

                // 5. Send the event to the Kafka topic named "telemetry-topic"
                kafkaTemplate.send("telemetry-topic", event);

                System.out.println("Telemetry sent -> Home: " + home.getHomeId() +
                        " | Appliance: " + appliance.getName() +
                        " | Power: " + event.getCurrentWatt() + " W");
            }
        }
    }
}