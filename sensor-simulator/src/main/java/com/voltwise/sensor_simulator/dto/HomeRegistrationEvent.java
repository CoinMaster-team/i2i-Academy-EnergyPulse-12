package com.voltwise.sensor_simulator.dto;

import lombok.Data;
import java.util.List;

@Data
public class HomeRegistrationEvent {
    private String homeId;
    private double budgetQuota;
    private List<ApplianceDTO> appliances;
}