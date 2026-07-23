package com.voltwise.core.service;

import org.springframework.stereotype.Service;

@Service
public class EnergyRuleEngineService {

    // Check the power usage and apply rules
    public void processApplianceRules(String applianceId, double currentWatt, double limitWatt) {

        // Calculate usage percentage
        double usagePercentage = (currentWatt / limitWatt) * 100;

        if (usagePercentage > 100.0) {
            // 100% Exceeded -> Apply Penalty Tariff
            System.out.println("ALERT: 100% limit exceeded! Applying Penalty Tariff.");

            // Send to anomaly checker
            checkAndMarkAnomaly(applianceId);
        }
        else if (usagePercentage >= 80.0) {
            // 80% Reached -> Just Warning
            System.out.println("WARNING: 80% usage reached! Approaching limit.");
        }
    }

    // If appliance fails 3 times in a row, mark as anomaly (broken)
    private void checkAndMarkAnomaly(String applianceId) {

        // NOTE: When we merge with the main project, we will do this:
        // 1. Get the current "fail count" from Apache Ignite (RAM).
        // 2. Add +1 to the count.
        // 3. If count == 3 -> Mark the appliance as "ANOMALOUS" (bozuk/tehlikeli).

        System.out.println("Checking anomaly count for appliance: " + applianceId);
    }
}