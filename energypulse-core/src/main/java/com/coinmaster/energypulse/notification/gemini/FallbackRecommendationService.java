package com.coinmaster.energypulse.notification.gemini;

import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import org.springframework.stereotype.Service;

@Service
public class FallbackRecommendationService {

    public String createFallback(NotificationCandidate candidate) {
        String deviceName = candidate.applianceName() != null
                ? candidate.applianceName()
                : "ilgili cihaz";

        return switch (candidate.eventType()) {
            case "QUOTA_80_REACHED" ->
                    "Enerji kullanımınız kotanın %80 seviyesine ulaştı. Yüksek tüketimli cihazları kontrol edin ve gereksiz çalışan cihazları kapatın.";
            case "QUOTA_100_REACHED" ->
                    "Enerji kotanız doldu. Tüketimi azaltmak için yüksek güçlü cihazları sırayla kontrol edin ve zorunlu olmayan kullanımları erteleyin.";
            case "PENALTY_TARIFF_ACTIVATED" ->
                    "Ceza tarifesi etkinleşti. Maliyeti sınırlamak için yüksek tüketimli cihazların kullanım süresini azaltın.";
            case "APPLIANCE_ANOMALY_DETECTED" ->
                    "%s cihazında olağan dışı tüketim algılandı. Cihazı güvenli şekilde kapatın, bağlantısını ve çalışma koşullarını gözle kontrol edin. Sorun devam ederse yetkili servise başvurun."
                            .formatted(deviceName);
            default ->
                    "Olağan dışı bir enerji olayı algılandı. Evdeki yüksek tüketimli cihazları kontrol edin ve risk görürseniz uzman desteği alın.";
        };
    }
}
