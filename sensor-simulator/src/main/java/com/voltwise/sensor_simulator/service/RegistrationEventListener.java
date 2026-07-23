package com.voltwise.sensor_simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voltwise.sensor_simulator.dto.HomeRegistrationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired; // Ekledik, kırmızılık bitti

@Service
public class RegistrationEventListener {

    private final HomeStorageService homeStorageService;
    private final ObjectMapper objectMapper;

    // Get required tools (Dependency Injection)
    @Autowired
    public RegistrationEventListener(HomeStorageService homeStorageService, ObjectMapper objectMapper) {
        this.homeStorageService = homeStorageService;
        this.objectMapper = objectMapper;
    }

    // Listen to the "registration-topic" channel from Core
    @KafkaListener(topics = "registration-topic", groupId = "sensor-simulator-group")
    public void consumeRegistrationEvent(String message) {
        try {
            // 1. Convert JSON string to Java object
            HomeRegistrationEvent event = objectMapper.readValue(message, HomeRegistrationEvent.class);

            // 2. Save the new home data to memory (RAM)
            homeStorageService.addHome(event);

            System.out.println("New home received from Core and saved! Home ID: " + event.getHomeId());
        } catch (Exception e) {
            System.err.println("Error reading home event: " + e.getMessage());
        }
    }
}