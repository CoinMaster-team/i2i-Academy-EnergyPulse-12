package com.coinmaster.energypulse.telemetry.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public class TelemetryJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public TelemetryJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isProcessed(UUID telemetryEventId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM billing_ledger WHERE telemetry_event_id = ?",
                Integer.class,
                telemetryEventId);

        return count != null && count > 0;
    }

    public boolean insertBillingEntry(
            UUID homeId,
            UUID telemetryEventId,
            BigDecimal energyDeltaKwh,
            BigDecimal tariffRate,
            BigDecimal costDelta,
            BigDecimal totalEnergyKwh,
            BigDecimal totalCost,
            String tariffState,
            OffsetDateTime recordedAt) {
        return jdbcTemplate.update(
                """
                        INSERT INTO billing_ledger (
                            home_id,
                            telemetry_event_id,
                            energy_delta_kwh,
                            tariff_rate,
                            cost_delta,
                            total_energy_kwh,
                            total_cost,
                            tariff_state,
                            recorded_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (telemetry_event_id) DO NOTHING
                        """,
                homeId,
                telemetryEventId,
                energyDeltaKwh,
                tariffRate,
                costDelta,
                totalEnergyKwh,
                totalCost,
                tariffState,
                recordedAt) == 1;
    }

    public void insertQuotaEvent(
            UUID homeId,
            UUID sourceEventId,
            String eventType,
            BigDecimal previousEnergyKwh,
            BigDecimal currentEnergyKwh,
            BigDecimal quotaKwh,
            BigDecimal usagePercentage,
            OffsetDateTime occurredAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO operational_events (
                            home_id,
                            appliance_id,
                            source_event_id,
                            event_type,
                            details,
                            occurred_at
                        ) VALUES (
                            ?,
                            NULL,
                            ?,
                            ?,
                            jsonb_build_object(
                                'previousEnergyKwh', ?,
                                'currentEnergyKwh', ?,
                                'quotaKwh', ?,
                                'usagePercentage', ?
                            ),
                            ?
                        )
                        ON CONFLICT (home_id, source_event_id, event_type) DO NOTHING
                        """,
                homeId,
                sourceEventId,
                eventType,
                previousEnergyKwh,
                currentEnergyKwh,
                quotaKwh,
                usagePercentage,
                occurredAt);
    }

    public void insertApplianceEvent(
            UUID homeId,
            UUID applianceId,
            UUID sourceEventId,
            String eventType,
            BigDecimal currentWatt,
            BigDecimal safeLimitWatt,
            int consecutiveLimitBreaches,
            OffsetDateTime occurredAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO operational_events (
                            home_id,
                            appliance_id,
                            source_event_id,
                            event_type,
                            details,
                            occurred_at
                        ) VALUES (
                            ?,
                            ?,
                            ?,
                            ?,
                            jsonb_build_object(
                                'currentWatt', ?,
                                'safeLimitWatt', ?,
                                'consecutiveLimitBreaches', ?
                            ),
                            ?
                        )
                        ON CONFLICT (home_id, source_event_id, event_type) DO NOTHING
                        """,
                homeId,
                applianceId,
                sourceEventId,
                eventType,
                currentWatt,
                safeLimitWatt,
                consecutiveLimitBreaches,
                occurredAt);
    }
}
