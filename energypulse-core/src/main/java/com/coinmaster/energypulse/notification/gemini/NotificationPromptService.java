package com.coinmaster.energypulse.notification.gemini;

import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import org.springframework.stereotype.Service;

@Service
public class NotificationPromptService {

    public String createPrompt(NotificationCandidate candidate) {
        String deviceName = candidate.applianceName() != null
                ? candidate.applianceName()
                : "Ev geneli";

        return """
                Sen EnergyPulse enerji güvenliği asistanısın.
                Aşağıdaki enerji olayını değerlendir ve kullanıcıya yalnızca Türkçe yanıt ver.

                Kurallar:
                - Yanıt kısa, açık ve uygulanabilir olsun.
                - En fazla 4 kısa madde kullan.
                - Önce güvenli ve düşük riskli kontrolleri öner.
                - Elektrik panosuna veya cihazın iç aksamına müdahale önerme.
                - Yangın, yanık kokusu veya elektrik çarpması riski varsa cihazın kapatılmasını
                  ve yetkili teknik servis ya da elektrik uzmanıyla iletişime geçilmesini söyle.
                - Kesin arıza teşhisi koyma ve gereksiz teknik terim kullanma.

                Olay bilgileri:
                Ev: %s
                Cihaz: %s
                Olay tipi: %s
                Olay detayları: %s
                Olay zamanı: %s
                """.formatted(
                candidate.homeName(),
                deviceName,
                candidate.eventType(),
                candidate.eventDetails(),
                candidate.occurredAt());
    }
}
