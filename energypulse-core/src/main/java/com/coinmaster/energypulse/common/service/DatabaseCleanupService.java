package com.coinmaster.energypulse.common.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@ConditionalOnProperty(
        name = "app.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DatabaseCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCleanupService.class);

    private final JdbcTemplate jdbcTemplate;
    private final int billingLedgerRetentionHours;
    private final int operationalEventsRetentionDays;
    private final int notificationsRetentionDays;
    private final int snapshotsRetentionDays;

    public DatabaseCleanupService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.cleanup.billing-ledger-hours:24}") int billingLedgerRetentionHours,
            @Value("${app.cleanup.operational-events-days:7}") int operationalEventsRetentionDays,
            @Value("${app.cleanup.notifications-days:7}") int notificationsRetentionDays,
            @Value("${app.cleanup.snapshots-days:30}") int snapshotsRetentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.billingLedgerRetentionHours = Math.max(1, billingLedgerRetentionHours);
        this.operationalEventsRetentionDays = Math.max(1, operationalEventsRetentionDays);
        this.notificationsRetentionDays = Math.max(1, notificationsRetentionDays);
        this.snapshotsRetentionDays = Math.max(1, snapshotsRetentionDays);
    }

    @PostConstruct
    public void initCleanup() {
        LOGGER.info("Executing initial database cleanup on startup...");
        purgeOldData();
    }

    @Scheduled(fixedRateString = "${app.cleanup.interval-ms:900000}")
    public void purgeOldData() {
        try {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            // 1. Delete old billing ledger entries (high volume telemetry data)
            OffsetDateTime billingThreshold = now.minusHours(billingLedgerRetentionHours);
            int deletedBilling = jdbcTemplate.update(
                    "DELETE FROM billing_ledger WHERE recorded_at < ?",
                    billingThreshold);

            // 2. Delete old AI notifications
            OffsetDateTime notificationThreshold = now.minusDays(notificationsRetentionDays);
            int deletedNotifications = jdbcTemplate.update(
                    "DELETE FROM ai_notifications WHERE created_at < ?",
                    notificationThreshold);

            // 3. Delete old operational events (referencing notifications must be deleted first)
            OffsetDateTime eventThreshold = now.minusDays(operationalEventsRetentionDays);
            int deletedEvents = jdbcTemplate.update(
                    """
                            DELETE FROM operational_events 
                            WHERE occurred_at < ? 
                              AND id NOT IN (SELECT operational_event_id FROM ai_notifications)
                            """,
                    eventThreshold);

            // 4. Delete old consumption snapshots
            OffsetDateTime snapshotThreshold = now.minusDays(snapshotsRetentionDays);
            int deletedSnapshots = jdbcTemplate.update(
                    "DELETE FROM consumption_snapshots WHERE captured_at < ?",
                    snapshotThreshold);

            LOGGER.info("Database cleanup completed: deleted {} billing ledger entries, {} notifications, {} operational events, {} snapshots",
                    deletedBilling, deletedNotifications, deletedEvents, deletedSnapshots);
        } catch (Exception e) {
            LOGGER.error("Failed to execute database cleanup purge", e);
        }
    }
}
