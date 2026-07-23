package com.coinmaster.energypulse.telemetry.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record LiveTelemetryResponse(
        OffsetDateTime generatedAt,
        List<LiveHomeResponse> homes) {

    public LiveTelemetryResponse {
        homes = List.copyOf(homes);
    }
}
