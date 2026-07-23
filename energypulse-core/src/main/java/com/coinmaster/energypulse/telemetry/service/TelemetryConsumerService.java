package com.coinmaster.energypulse.telemetry.service;

import com.coinmaster.energypulse.common.exception.ResourceNotFoundException;
import com.coinmaster.energypulse.telemetry.event.TelemetryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TelemetryConsumerService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TelemetryConsumerService.class);

    private final TelemetryProcessingService processingService;

    public TelemetryConsumerService(
            TelemetryProcessingService processingService) {
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.telemetry}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTelemetry(TelemetryEvent event) {
        try {
            boolean processed = processingService.process(event);

            if (processed) {
                LOGGER.debug(
                        "Telemetry processed. eventId={}, homeId={}, applianceId={}",
                        event.eventId(),
                        event.homeId(),
                        event.applianceId());
            }
        } catch (IllegalArgumentException | ResourceNotFoundException exception) {
            LOGGER.warn(
                    "Telemetry discarded. eventId={}, reason={}",
                    event == null ? null : event.eventId(),
                    exception.getMessage());
        }
    }
}
