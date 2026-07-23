package com.coinmaster.energypulse.notification.mail;

import com.coinmaster.energypulse.notification.dto.GeneratedRecommendation;
import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmtpEmailDeliveryService implements EmailDeliveryService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public SmtpEmailDeliveryService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.notifications.mail-from:}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendNotification(
            NotificationCandidate candidate,
            GeneratedRecommendation recommendation) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalStateException("SMTP mail sender is not configured.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(fromAddress)) {
            message.setFrom(fromAddress);
        }

        message.setTo(candidate.recipientEmail());
        message.setSubject(createSubject(candidate));
        message.setText(createBody(candidate, recommendation));

        mailSender.send(message);
    }

    private String createSubject(NotificationCandidate candidate) {
        String target = candidate.applianceName() != null
                ? candidate.applianceName()
                : candidate.homeName();

        return "[EnergyPulse] Enerji uyarısı - " + target;
    }

    private String createBody(
            NotificationCandidate candidate,
            GeneratedRecommendation recommendation) {
        String target = candidate.applianceName() != null
                ? candidate.applianceName()
                : "Ev geneli";
        String sourceNote = "FALLBACK".equals(recommendation.status())
                ? "Gemini servisine ulaşılamadığı için güvenli varsayılan öneri kullanıldı."
                : "Öneri Gemini tarafından oluşturuldu.";

        return """
                EnergyPulse enerji uyarısı

                Ev: %s
                Cihaz: %s
                Olay: %s
                Zaman: %s

                Öneri:
                %s

                %s

                Bu mesaj otomatik olarak oluşturulmuştur.
                """.formatted(
                candidate.homeName(),
                target,
                candidate.eventType(),
                candidate.occurredAt(),
                recommendation.text(),
                sourceNote);
    }
}
