package com.coinmaster.energypulse.telemetry.controller;

import com.coinmaster.energypulse.telemetry.dto.LiveTelemetryResponse;
import com.coinmaster.energypulse.telemetry.service.LiveTelemetryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telemetry")
public class LiveTelemetryController {

    private final LiveTelemetryService liveTelemetryService;

    public LiveTelemetryController(LiveTelemetryService liveTelemetryService) {
        this.liveTelemetryService = liveTelemetryService;
    }

    @GetMapping("/live")
    public LiveTelemetryResponse getLiveTelemetry() {
        return liveTelemetryService.getLiveTelemetry();
    }
}
