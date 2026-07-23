package com.voltwise.core.service;

import org.springframework.stereotype.Service;

@Service
public class IgniteCacheService {

    // NOTE: When we merge with the main project, we will inject the actual Ignite tool here.
    // private final Ignite ignite;

    // 1. Save high-frequency data to RAM instantly
    public void saveTelemetry(Object telemetryEvent) {

        // This will put the data into the Ignite Cache memory instead of hard drive
        // cache.put(eventId, telemetryEvent);

        System.out.println("Ignite RAM: Telemetry data saved to cache memory instantly.");
    }

    // 2. Read the latest data from RAM (Called by Frontend REST API)
    public Object getLatestTelemetry() {

        // This will fetch the freshest data directly from memory for the frontend
        // return cache.get("latest_data");

        return null; // Dummy return for now
    }
}