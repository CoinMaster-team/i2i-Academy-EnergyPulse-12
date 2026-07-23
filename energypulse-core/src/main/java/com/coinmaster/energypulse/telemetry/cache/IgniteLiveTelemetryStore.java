package com.coinmaster.energypulse.telemetry.cache;

import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCacheConfiguration;
import org.apache.ignite.client.IgniteClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.cache.Cache;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.ignite.enabled", havingValue = "true")
public class IgniteLiveTelemetryStore implements LiveTelemetryStore {

    private static final String CACHE_NAME = "energypulse-live-telemetry-v1";

    private final ClientCache<UUID, LiveHomeState> cache;

    public IgniteLiveTelemetryStore(IgniteClient igniteClient) {
        this.cache = igniteClient.getOrCreateCache(
                new ClientCacheConfiguration()
                        .setName(CACHE_NAME)
                        .setBackups(1));
    }

    @Override
    public Optional<LiveHomeState> findHome(UUID homeId) {
        return Optional.ofNullable(cache.get(homeId));
    }

    @Override
    public List<LiveHomeState> findAll() {
        return cache.query(new ScanQuery<UUID, LiveHomeState>())
                .getAll()
                .stream()
                .map(Cache.Entry::getValue)
                .toList();
    }

    @Override
    public void save(LiveHomeState home) {
        cache.put(home.homeId(), home);
    }
}
