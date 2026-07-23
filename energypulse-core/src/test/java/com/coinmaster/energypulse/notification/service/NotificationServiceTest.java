package com.coinmaster.energypulse.notification.service;

import com.coinmaster.energypulse.common.exception.ResourceNotFoundException;
import com.coinmaster.energypulse.notification.dto.NotificationResponse;
import com.coinmaster.energypulse.notification.repository.NotificationQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final UUID NOTIFICATION_ID = UUID.fromString("1f5c9279-a2fc-48d2-8a6d-2700e26ad130");

    @Mock
    private NotificationQueryRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldReturnNotifications() {
        NotificationResponse notification = notification();
        when(notificationRepository.findAll()).thenReturn(List.of(notification));

        List<NotificationResponse> response = notificationService.getNotifications();

        assertEquals(1, response.size());
        assertEquals(NOTIFICATION_ID, response.get(0).id());
    }

    @Test
    void shouldReturnNotificationDetails() {
        NotificationResponse notification = notification();
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.getNotification(NOTIFICATION_ID);

        assertEquals("SENT", response.emailStatus());
        assertEquals("GENERATED", response.generationStatus());
    }

    @Test
    void shouldReturnNotFoundForUnknownNotification() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> notificationService.getNotification(NOTIFICATION_ID));

        assertEquals("NOTIFICATION_NOT_FOUND", exception.getCode());
    }

    private NotificationResponse notification() {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-22T18:42:00+03:00");

        return new NotificationResponse(
                NOTIFICATION_ID,
                UUID.fromString("1ae10440-3268-4961-a340-677f08129db5"),
                "Home A",
                UUID.fromString("43f8d1ec-53ce-430c-a2c3-c50df62959a7"),
                "Air Conditioner",
                "APPLIANCE_ANOMALY_DETECTED",
                "{\"powerWatts\":2350}",
                "user@example.com",
                "GEMINI",
                "tr",
                "Prompt",
                "Cihazı kontrol edin.",
                "GENERATED",
                "SENT",
                null,
                null,
                now.minusMinutes(1),
                now,
                now.plusSeconds(2));
    }
}
