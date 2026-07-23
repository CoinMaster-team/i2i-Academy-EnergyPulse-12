package com.coinmaster.energypulse.notification.service;

import com.coinmaster.energypulse.common.exception.ResourceNotFoundException;
import com.coinmaster.energypulse.notification.dto.NotificationResponse;
import com.coinmaster.energypulse.notification.repository.NotificationQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationQueryRepository notificationRepository;

    public NotificationService(NotificationQueryRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationResponse> getNotifications() {
        return notificationRepository.findAll();
    }

    public NotificationResponse getNotification(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NOTIFICATION_NOT_FOUND",
                        "Notification could not be found."));
    }
}
