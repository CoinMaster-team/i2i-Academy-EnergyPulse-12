package com.voltwise.sensor_simulator.service;

import com.voltwise.sensor_simulator.dto.HomeRegistrationEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HomeStorageService {

    // Thread-safe memory to store registered homes
    // Kaydedilen evleri tutmak için eşzamanlı çalışmaya uygun hafıza
    private final Map<String, HomeRegistrationEvent> homeStorage = new ConcurrentHashMap<>();

    // Save incoming home data to memory
    // Gelen ev verisini hafızaya kaydet
    public void addHome(HomeRegistrationEvent home) {
        homeStorage.put(home.getHomeId(), home);
        System.out.println("New home added to memory. Home ID: " + home.getHomeId());
    }

    // Return all homes from memory for the simulation engine
    // Simülasyon motoru için hafızadaki tüm evleri geri döndür
    public List<HomeRegistrationEvent> getAllHomes() {
        return new ArrayList<>(homeStorage.values());
    }
}