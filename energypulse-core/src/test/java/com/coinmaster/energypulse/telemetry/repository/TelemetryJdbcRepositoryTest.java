package com.coinmaster.energypulse.telemetry.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldStoreBillingEntryAndMinuteSnapshot() {
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenReturn(1);
        TelemetryJdbcRepository repository =
                new TelemetryJdbcRepository(jdbcTemplate);

        boolean inserted = repository.insertBillingEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("0.001000"),
                new BigDecimal("2.250000"),
                new BigDecimal("0.002250"),
                new BigDecimal("12.001000"),
                new BigDecimal("27.002250"),
                "NORMAL",
                OffsetDateTime.parse("2026-07-24T12:34:56Z"));

        assertTrue(inserted);
        verify(jdbcTemplate, times(2))
                .update(anyString(), any(Object[].class));
    }
}
