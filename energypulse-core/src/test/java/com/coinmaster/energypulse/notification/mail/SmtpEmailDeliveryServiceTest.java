package com.coinmaster.energypulse.notification.mail;

import com.coinmaster.energypulse.notification.dto.GeneratedRecommendation;
import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailDeliveryServiceTest {

    @Test
    void shouldSendTurkishEmail() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        SmtpEmailDeliveryService service = new SmtpEmailDeliveryService(
                provider,
                "energypulse@example.com");

        service.sendNotification(
                candidate(),
                GeneratedRecommendation.generated("Cihazı kontrol edin."));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldFailClearlyWhenSmtpIsNotConfigured() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        SmtpEmailDeliveryService service = new SmtpEmailDeliveryService(provider, "");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.sendNotification(
                        candidate(),
                        GeneratedRecommendation.generated("Öneri")));

        assertTrue(exception.getMessage().contains("SMTP"));
    }

    private NotificationCandidate candidate() {
        return new NotificationCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "QA Smart Home",
                "qa@energypulse.local",
                UUID.randomUUID(),
                "Air Conditioner",
                "APPLIANCE_ANOMALY_DETECTED",
                "{}",
                OffsetDateTime.parse("2026-07-23T10:30:00Z"));
    }
}
