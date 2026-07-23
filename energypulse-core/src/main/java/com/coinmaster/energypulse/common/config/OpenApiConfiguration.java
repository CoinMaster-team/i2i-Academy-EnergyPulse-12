package com.coinmaster.energypulse.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI energyPulseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EnergyPulse Core API")
                        .version("1.0.0")
                        .description(
                                "REST API for home registration, "
                                        + "energy monitoring and historical "
                                        + "consumption analysis."));
    }
}