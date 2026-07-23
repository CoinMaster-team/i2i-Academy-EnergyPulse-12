package com.voltwise.core.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TelemetryConsumerService {

    // Listen to high-frequency data from "telemetry-topic"
    @KafkaListener(topics = "telemetry-topic", groupId = "core-telemetry-group")
    public void consumeTelemetry(String message) {
        try {
            // NOTE: When we merge this with the main project, we will do 2 things here:
            // 1. Convert "message" (JSON string) to TelemetryEvent object
            // 2. Save it to Apache Ignite (RAM) database

            System.out.println("Success: Telemetry data received from sensor! Data: " + message);
        } catch (Exception e) {
            System.err.println("Error: Could not read telemetry data: " + e.getMessage());
        }
    }
}