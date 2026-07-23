package com.coinmaster.energypulse.telemetry.cache;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiveTelemetryStore {

    Optional<LiveHomeState> findHome(UUID homeId);

    List<LiveHomeState> findAll();

    void save(LiveHomeState home);
}
