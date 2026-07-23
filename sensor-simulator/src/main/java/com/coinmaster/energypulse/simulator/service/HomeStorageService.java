package com.coinmaster.energypulse.simulator.service;

import com.coinmaster.energypulse.simulator.event.HomeRegistrationEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HomeStorageService {

    private final Map<UUID, HomeRegistrationEvent> homes = new ConcurrentHashMap<>();

    public void addHome(HomeRegistrationEvent home) {
        homes.put(home.homeId(), home);
    }

    public List<HomeRegistrationEvent> getAllHomes() {
        return List.copyOf(homes.values());
    }
}
