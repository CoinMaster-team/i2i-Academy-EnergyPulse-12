package com.coinmaster.energypulse.notification.mail;

import com.coinmaster.energypulse.notification.dto.GeneratedRecommendation;
import com.coinmaster.energypulse.notification.dto.NotificationCandidate;

public interface EmailDeliveryService {

    void sendNotification(
            NotificationCandidate candidate,
            GeneratedRecommendation recommendation);
}
