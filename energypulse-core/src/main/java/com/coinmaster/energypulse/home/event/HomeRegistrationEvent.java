package com.coinmaster.energypulse.home.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record HomeRegistrationEvent(
        UUID eventId,
        int schemaVersion,
        OffsetDateTime occurredAt,
        UUID homeId,
        String homeName,
        List<ApplianceRegistrationEvent> appliances) {

    public HomeRegistrationEvent {
        appliances = List.copyOf(appliances);
    }
}