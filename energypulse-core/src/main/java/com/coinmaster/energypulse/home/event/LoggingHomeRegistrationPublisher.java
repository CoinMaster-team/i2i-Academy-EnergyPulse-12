package com.coinmaster.energypulse.home.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingHomeRegistrationPublisher
        implements HomeRegistrationPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingHomeRegistrationPublisher.class);

    @Override
    public void publish(HomeRegistrationEvent event) {
        LOGGER.info(
                "Home registration event prepared. eventId={}, homeId={}, applianceCount={}",
                event.eventId(),
                event.homeId(),
                event.appliances().size());
    }
}