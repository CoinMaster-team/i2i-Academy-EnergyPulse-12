package com.coinmaster.energypulse.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SensorSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorSimulatorApplication.class, args);
    }
}
