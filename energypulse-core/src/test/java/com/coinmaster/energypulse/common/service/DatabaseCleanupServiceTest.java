package com.coinmaster.energypulse.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseCleanupServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DatabaseCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new DatabaseCleanupService(
                jdbcTemplate,
                24,
                7,
                7,
                30);
    }

    @Test
    void purgeOldData_ExecutesDeleteQueries() {
        when(jdbcTemplate.update(eq("DELETE FROM billing_ledger WHERE recorded_at < ?"), any(OffsetDateTime.class)))
                .thenReturn(150);
        when(jdbcTemplate.update(eq("DELETE FROM ai_notifications WHERE created_at < ?"), any(OffsetDateTime.class)))
                .thenReturn(5);
        when(jdbcTemplate.update(any(String.class), any(OffsetDateTime.class)))
                .thenReturn(10);

        cleanupService.purgeOldData();

        verify(jdbcTemplate).update(eq("DELETE FROM billing_ledger WHERE recorded_at < ?"), any(OffsetDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM ai_notifications WHERE created_at < ?"), any(OffsetDateTime.class));
        verify(jdbcTemplate).update(eq("DELETE FROM consumption_snapshots WHERE captured_at < ?"), any(OffsetDateTime.class));
    }

    @Test
    void initCleanup_RunsOnStartup() {
        cleanupService.initCleanup();
        verify(jdbcTemplate).update(eq("DELETE FROM billing_ledger WHERE recorded_at < ?"), any(OffsetDateTime.class));
    }
}
