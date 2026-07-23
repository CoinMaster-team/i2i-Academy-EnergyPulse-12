package com.voltwise.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "*") // Allow frontend to read data without CORS errors
public class LiveTelemetryController {

    // NOTE: When we merge with the main project, we will add:
    // private final IgniteCacheService igniteCacheService;
    // to read data directly from RAM.

    // Frontend will call this link every second to show live charts
    @GetMapping("/live")
    public String getLiveTelemetry() {

        // For now, we return a dummy JSON message.
        // Later, this will return the real live data from Apache Ignite.
        return "{ \"status\": \"success\", \"message\": \"Live data streaming from Ignite RAM!\" }";
    }
}