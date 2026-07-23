package com.voltwise.sensor_simulator.dto;

import lombok.Data;

@Data
public class TelemetryEvent {
    private String homeId;
    private String applianceId;
    private double currentWatt;
    private long timestamp;
}