package com.coinmaster.energypulse.telemetry.config;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.ignite.enabled", havingValue = "true")
public class IgniteClientConfiguration {

    @Bean(destroyMethod = "close")
    IgniteClient igniteClient(
            @Value("${app.ignite.addresses:localhost:10800}") String addresses) {
        String[] configuredAddresses = Arrays.stream(addresses.split(","))
                .map(String::trim)
                .filter(address -> !address.isBlank())
                .toArray(String[]::new);

        return Ignition.startClient(
                new ClientConfiguration().setAddresses(configuredAddresses));
    }
}
