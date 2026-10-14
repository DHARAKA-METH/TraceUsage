package com.traceusage.traceusage.aggregation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelemetryRetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldDeleteRawAggregatesAndDeduplicationRowsUsingConfiguredWindows() {
        TelemetryRetentionProperties properties = properties();
        when(jdbcTemplate.update(anyString(), any(), anyInt())).thenReturn(2);
        TelemetryRetentionService service = service(properties);

        TelemetryRetentionService.RetentionCleanupResult result = service.cleanup();

        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("delete from usage_events"),
                eq(Timestamp.from(NOW.minusSeconds(14L * 24 * 3600))),
                eq(50_000));
        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("delete from field_usage_events"),
                eq(Timestamp.from(NOW.minusSeconds(14L * 24 * 3600))),
                eq(50_000));
        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("delete from endpoint_usage_hourly"),
                eq(Timestamp.from(NOW.minusSeconds(730L * 24 * 3600))),
                eq(50_000));
        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("delete from field_usage_hourly"),
                eq(Timestamp.from(NOW.minusSeconds(730L * 24 * 3600))),
                eq(50_000));
        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("delete from processed_endpoint_aggregation_events"),
                eq(Timestamp.from(NOW.minusSeconds(30L * 24 * 3600))),
                eq(50_000));
        verify(jdbcTemplate).update(
                org.mockito.ArgumentMatchers.contains("delete from processed_field_aggregation_batches"),
                eq(Timestamp.from(NOW.minusSeconds(30L * 24 * 3600))),
                eq(50_000));

        assertThat(result.total()).isEqualTo(12);
    }

    @Test
    void shouldKeepRawRowsInRawOnlyMode() {
        TelemetryRetentionProperties properties = properties();
        properties.setStorageMode(TelemetryRetentionProperties.StorageMode.RAW_ONLY);
        TelemetryRetentionService service = service(properties);

        service.cleanup();

        verify(jdbcTemplate, never()).update(
                org.mockito.ArgumentMatchers.contains("delete from usage_events"), any(), anyInt());
        verify(jdbcTemplate, never()).update(
                org.mockito.ArgumentMatchers.contains("delete from field_usage_events"), any(), anyInt());
    }

    @Test
    void shouldNotRunWhenCleanupIsDisabled() {
        TelemetryRetentionProperties properties = properties();
        properties.setEnabled(false);
        TelemetryRetentionService service = service(properties);

        service.runScheduledCleanup();

        verify(jdbcTemplate, never()).update(anyString(), any(), anyInt());
    }

    @Test
    void shouldAlwaysTrimDeduplicationRecordsEvenInRawOnlyMode() {
        TelemetryRetentionProperties properties = properties();
        properties.setStorageMode(TelemetryRetentionProperties.StorageMode.RAW_ONLY);
        TelemetryRetentionService service = service(properties);

        service.cleanup();

        List<String> statements = sqlStatements();
        assertThat(statements).anyMatch(sql -> sql.contains("processed_endpoint_aggregation_events"));
        assertThat(statements).anyMatch(sql -> sql.contains("processed_field_aggregation_batches"));
    }

    private List<String> sqlStatements() {
        return org.mockito.Mockito.mockingDetails(jdbcTemplate).getInvocations().stream()
                .filter(invocation -> "update".equals(invocation.getMethod().getName()))
                .map(invocation -> (String) invocation.getArguments()[0])
                .toList();
    }

    private TelemetryRetentionService service(TelemetryRetentionProperties properties) {
        return new TelemetryRetentionService(jdbcTemplate, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private TelemetryRetentionProperties properties() {
        TelemetryRetentionProperties properties = new TelemetryRetentionProperties();
        properties.setEnabled(true);
        properties.setRawDays(14);
        properties.setAggregateDays(730);
        properties.setDeduplicationDays(30);
        properties.setDeleteBatchLimit(50_000);
        return properties;
    }
}