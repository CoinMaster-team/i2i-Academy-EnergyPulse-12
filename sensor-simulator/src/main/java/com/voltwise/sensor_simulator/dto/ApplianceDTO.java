package com.voltwise.sensor_simulator.dto;

import lombok.Data;

@Data
public class ApplianceDTO {
    private String id;
    private String name;
    private double safeLimitWatt;
    private double simulationMinWatt;
    private double simulationMaxWatt;
}