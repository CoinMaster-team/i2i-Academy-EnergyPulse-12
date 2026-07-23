package com.coinmaster.energypulse.telemetry.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(
        name = "app.ignite.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class InMemoryLiveTelemetryStore implements LiveTelemetryStore {

    private final Map<UUID, LiveHomeState> homes = new ConcurrentHashMap<>();

    @Override
    public Optional<LiveHomeState> findHome(UUID homeId) {
        return Optional.ofNullable(homes.get(homeId));
    }

    @Override
    public List<LiveHomeState> findAll() {
        return List.copyOf(homes.values());
    }

    @Override
    public void save(LiveHomeState home) {
        homes.put(home.homeId(), home);
    }
}
