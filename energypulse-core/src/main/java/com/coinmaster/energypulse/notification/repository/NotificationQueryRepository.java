package com.coinmaster.energypulse.notification.repository;

import com.coinmaster.energypulse.notification.dto.NotificationResponse;
import com.coinmaster.energypulse.notification.dto.GeneratedRecommendation;
import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NotificationQueryRepository {

    private static final String SELECT_CANDIDATE = """
            SELECT
                event.id AS operational_event_id,
                event.home_id,
                home.name AS home_name,
                home.contact_email AS recipient_email,
                event.appliance_id,
                appliance.name AS appliance_name,
                event.event_type,
                event.details::text AS event_details,
                event.occurred_at
            FROM operational_events event
            JOIN homes home ON home.id = event.home_id
            LEFT JOIN appliances appliance ON appliance.id = event.appliance_id
            LEFT JOIN ai_notifications notification
                ON notification.operational_event_id = event.id
            WHERE notification.id IS NULL
              AND event.event_type IN (
                  'QUOTA_80_REACHED',
                  'QUOTA_100_REACHED',
                  'PENALTY_TARIFF_ACTIVATED',
                  'APPLIANCE_ANOMALY_DETECTED'
              )
            """;

    private static final String SELECT_NOTIFICATION = """
            SELECT
                notification.id,
                notification.home_id,
                home.name AS home_name,
                event.appliance_id,
                appliance.name AS appliance_name,
                event.event_type,
                event.details::text AS event_details,
                notification.recipient_email,
                notification.provider,
                notification.language_code,
                notification.prompt_text,
                notification.recommendation_text,
                notification.generation_status,
                notification.email_status,
                notification.generation_error,
                notification.email_error,
                event.occurred_at,
                notification.created_at,
                notification.sent_at
            FROM ai_notifications notification
            JOIN homes home ON home.id = notification.home_id
            JOIN operational_events event ON event.id = notification.operational_event_id
            LEFT JOIN appliances appliance ON appliance.id = event.appliance_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public NotificationQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<NotificationResponse> findAll() {
        return jdbcTemplate.query(
                SELECT_NOTIFICATION + " ORDER BY notification.created_at DESC",
                this::mapNotification);
    }

    public Optional<NotificationResponse> findById(UUID notificationId) {
        List<NotificationResponse> notifications = jdbcTemplate.query(
                SELECT_NOTIFICATION + " WHERE notification.id = ?",
                this::mapNotification,
                notificationId);

        return notifications.stream().findFirst();
    }

    public List<NotificationCandidate> findPendingCandidates(int limit) {
        return jdbcTemplate.query(
                SELECT_CANDIDATE + " ORDER BY event.occurred_at ASC LIMIT ?",
                this::mapCandidate,
                limit);
    }

    public Optional<NotificationCandidate> findPendingCandidate(UUID operationalEventId) {
        List<NotificationCandidate> candidates = jdbcTemplate.query(
                SELECT_CANDIDATE + " AND event.id = ?",
                this::mapCandidate,
                operationalEventId);

        return candidates.stream().findFirst();
    }

    public Optional<UUID> createPendingNotification(
            NotificationCandidate candidate,
            String prompt,
            GeneratedRecommendation recommendation) {
        List<UUID> notificationIds = jdbcTemplate.query(
                """
                        INSERT INTO ai_notifications (
                            home_id,
                            operational_event_id,
                            recipient_email,
                            provider,
                            language_code,
                            prompt_text,
                            recommendation_text,
                            generation_status,
                            email_status,
                            generation_error
                        ) VALUES (?, ?, ?, 'GEMINI', 'tr', ?, ?, ?, 'PENDING', ?)
                        ON CONFLICT (operational_event_id) DO NOTHING
                        RETURNING id
                        """,
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class),
                candidate.homeId(),
                candidate.operationalEventId(),
                candidate.recipientEmail(),
                prompt,
                recommendation.text(),
                recommendation.status(),
                recommendation.error());

        return notificationIds.stream().findFirst();
    }

    public void markEmailSent(UUID notificationId) {
        jdbcTemplate.update(
                """
                        UPDATE ai_notifications
                        SET email_status = 'SENT', sent_at = CURRENT_TIMESTAMP, email_error = NULL
                        WHERE id = ?
                        """,
                notificationId);
    }

    public void markEmailFailed(UUID notificationId, String error) {
        jdbcTemplate.update(
                """
                        UPDATE ai_notifications
                        SET email_status = 'FAILED', sent_at = NULL, email_error = ?
                        WHERE id = ?
                        """,
                error,
                notificationId);
    }

    private NotificationResponse mapNotification(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new NotificationResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("home_id", UUID.class),
                resultSet.getString("home_name"),
                resultSet.getObject("appliance_id", UUID.class),
                resultSet.getString("appliance_name"),
                resultSet.getString("event_type"),
                resultSet.getString("event_details"),
                resultSet.getString("recipient_email"),
                resultSet.getString("provider"),
                resultSet.getString("language_code"),
                resultSet.getString("prompt_text"),
                resultSet.getString("recommendation_text"),
                resultSet.getString("generation_status"),
                resultSet.getString("email_status"),
                resultSet.getString("generation_error"),
                resultSet.getString("email_error"),
                resultSet.getObject("occurred_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("sent_at", OffsetDateTime.class));
    }

    private NotificationCandidate mapCandidate(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new NotificationCandidate(
                resultSet.getObject("operational_event_id", UUID.class),
                resultSet.getObject("home_id", UUID.class),
                resultSet.getString("home_name"),
                resultSet.getString("recipient_email"),
                resultSet.getObject("appliance_id", UUID.class),
                resultSet.getString("appliance_name"),
                resultSet.getString("event_type"),
                resultSet.getString("event_details"),
                resultSet.getObject("occurred_at", OffsetDateTime.class));
    }
}
