package com.coinmaster.energypulse.notification.repository;

import com.coinmaster.energypulse.notification.dto.NotificationResponse;
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
}
