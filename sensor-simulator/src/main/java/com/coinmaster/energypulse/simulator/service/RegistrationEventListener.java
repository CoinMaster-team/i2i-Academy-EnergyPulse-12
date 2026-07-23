package com.coinmaster.energypulse.simulator.service;

import com.coinmaster.energypulse.simulator.event.HomeRegistrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class RegistrationEventListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RegistrationEventListener.class);

    private final HomeStorageService homeStorageService;

    public RegistrationEventListener(HomeStorageService homeStorageService) {
        this.homeStorageService = homeStorageService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.home-registration}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRegistrationEvent(HomeRegistrationEvent event) {
        homeStorageService.addHome(event);
        LOGGER.info(
                "Registered home received. eventId={}, homeId={}, applianceCount={}",
                event.eventId(),
                event.homeId(),
                event.appliances().size());
    }
}
