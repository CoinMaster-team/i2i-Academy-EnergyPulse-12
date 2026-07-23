package com.voltwise.core.service;

import com.voltwise.core.model.Home;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class HomeRegistrationPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Inject Kafka tool
    @Autowired
    public HomeRegistrationPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Send the registered home to Kafka topic
    public void publish(Home home) {
        try {
            // Send home data to "registration-topic"
            kafkaTemplate.send("registration-topic", home);

            System.out.println("Success: Home data sent to Kafka! Home ID: " + home.getId());
        } catch (Exception e) {
            System.err.println("Error: Could not send home to Kafka: " + e.getMessage());
        }
    }
}