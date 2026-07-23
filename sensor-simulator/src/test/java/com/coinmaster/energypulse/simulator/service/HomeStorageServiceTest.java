package com.coinmaster.energypulse.simulator.service;

import com.coinmaster.energypulse.simulator.event.HomeRegistrationEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HomeStorageServiceTest {

    @Test
    void replacesAnExistingHomeRegistration() {
        HomeStorageService storage = new HomeStorageService();
        UUID homeId = UUID.randomUUID();

        storage.addHome(event(homeId, "First name"));
        storage.addHome(event(homeId, "Updated name"));

        assertThat(storage.getAllHomes())
                .singleElement()
                .extracting(HomeRegistrationEvent::homeName)
                .isEqualTo("Updated name");
    }

    private HomeRegistrationEvent event(UUID homeId, String homeName) {
        return new HomeRegistrationEvent(
                UUID.randomUUID(),
                1,
                OffsetDateTime.now(),
                homeId,
                homeName,
                new BigDecimal("100"),
                new BigDecimal("500"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of());
    }
}
