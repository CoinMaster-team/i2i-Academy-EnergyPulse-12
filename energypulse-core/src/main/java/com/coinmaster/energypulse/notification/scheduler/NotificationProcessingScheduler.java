package com.coinmaster.energypulse.notification.scheduler;

import com.coinmaster.energypulse.notification.service.NotificationProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.notifications",
        name = "processing-enabled",
        havingValue = "true")
public class NotificationProcessingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProcessingScheduler.class);

    private final NotificationProcessingService processingService;

    public NotificationProcessingScheduler(NotificationProcessingService processingService) {
        this.processingService = processingService;
    }

    @Scheduled(fixedDelayString = "${app.notifications.polling-interval-ms:5000}")
    public void processNotifications() {
        int processedCount = processingService.processPendingNotifications();
        if (processedCount > 0) {
            LOGGER.info("Processed {} pending notification event(s).", processedCount);
        }
    }
}
