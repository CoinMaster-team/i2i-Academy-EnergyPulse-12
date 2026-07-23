package com.coinmaster.energypulse.home.event;

import com.coinmaster.energypulse.telemetry.service.LiveTelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class KafkaHomeRegistrationPublisher implements HomeRegistrationPublisher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KafkaHomeRegistrationPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final LiveTelemetryService liveTelemetryService;
    private final String registrationTopic;

    public KafkaHomeRegistrationPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            LiveTelemetryService liveTelemetryService,
            @Value("${app.kafka.topics.home-registration}") String registrationTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.liveTelemetryService = liveTelemetryService;
        this.registrationTopic = registrationTopic;
    }

    @Override
    public void publish(HomeRegistrationEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            dispatch(event);
                        }
                    });
            return;
        }

        dispatch(event);
    }

    private void dispatch(HomeRegistrationEvent event) {
        liveTelemetryService.registerHome(event);

        kafkaTemplate.send(
                        registrationTopic,
                        event.homeId().toString(),
                        event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        LOGGER.error(
                                "Home registration could not be published. eventId={}, homeId={}",
                                event.eventId(),
                                event.homeId(),
                                error);
                        return;
                    }

                    LOGGER.info(
                            "Home registration published. eventId={}, homeId={}, topic={}, partition={}, offset={}",
                            event.eventId(),
                            event.homeId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
