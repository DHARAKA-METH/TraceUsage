package com.traceusage.traceusage.aggregation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled retention cleanup for raw telemetry, hourly aggregates and
 * idempotency records.
 *
 * <p>Deletion happens in bounded batches so cleanup never holds a long-running
 * transaction or a large lock footprint. Cleanup is restartable: every run just
 * continues from the current time window.
 *
 * <p>Deduplication records are retained for longer than raw events on purpose.
 * They must outlive the Kafka topic retention window, otherwise a replayed
 * event from Kafka could be counted twice.
 */
@Component
public class TelemetryRetentionService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryRetentionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final TelemetryRetentionProperties properties;
    private final Clock clock;

    @Autowired
    public TelemetryRetentionService(JdbcTemplate jdbcTemplate,
                                     TelemetryRetentionProperties properties) {
        this(jdbcTemplate, properties, Clock.systemUTC());
    }

    TelemetryRetentionService(JdbcTemplate jdbcTemplate,
                              TelemetryRetentionProperties properties,
                              Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${traceusage.telemetry.retention.initial-delay-ms:600000}",
            fixedDelayString = "${traceusage.telemetry.retention.cleanup-interval-ms:3600000}")
    public void runScheduledCleanup() {
        if (!properties.isEnabled()) {
            return;
        }
        cleanup();
    }

    @Transactional
    public RetentionCleanupResult cleanup() {
        Instant now = Instant.now(clock);

        int rawUsageEvents = 0;
        int rawFieldEvents = 0;
        if (properties.deletesRawEvents()) {
            rawUsageEvents = deleteBatched(
                    "delete from usage_events where id in ("
                            + "  select id from usage_events where occurred_at < ? order by id limit ?)",
                    rawCutoff(now), properties.getDeleteBatchLimit());
            rawFieldEvents = deleteBatched(
                    "delete from field_usage_events where id in ("
                            + "  select id from field_usage_events where observed_at < ? order by id limit ?)",
                    rawCutoff(now), properties.getDeleteBatchLimit());
        }

        int aggregateRows = deleteBatched(
                "delete from endpoint_usage_hourly where (endpoint_id, hour_bucket) in ("
                        + "  select endpoint_id, hour_bucket from endpoint_usage_hourly"
                        + "  where hour_bucket < ? order by hour_bucket limit ?)",
                aggregateCutoff(now), properties.getDeleteBatchLimit());

        aggregateRows += deleteBatched(
                "delete from field_usage_hourly where id in ("
                        + "  select id from field_usage_hourly where hour_bucket < ? order by id limit ?)",
                aggregateCutoff(now), properties.getDeleteBatchLimit());

        int deduplicationRows = deleteBatched(
                "delete from processed_endpoint_aggregation_events where event_id in ("
                        + "  select event_id from processed_endpoint_aggregation_events"
                        + "  where processed_at < ? order by processed_at limit ?)",
                deduplicationCutoff(now), properties.getDeleteBatchLimit());

        deduplicationRows += deleteBatched(
                "delete from processed_field_aggregation_batches where batch_id in ("
                        + "  select batch_id from processed_field_aggregation_batches"
                        + "  where processed_at < ? order by processed_at limit ?)",
                deduplicationCutoff(now), properties.getDeleteBatchLimit());

        RetentionCleanupResult result = new RetentionCleanupResult(
                rawUsageEvents,
                rawFieldEvents,
                aggregateRows,
                deduplicationRows);

        if (result.total() > 0) {
            log.info("RETENTION CLEANUP -> {}", result);
        }

        return result;
    }

    private int deleteBatched(String sql, Instant cutoff, int limit) {
        return jdbcTemplate.update(sql, Timestamp.from(cutoff), limit);
    }

    private Instant rawCutoff(Instant now) {
        return now.minus(properties.getRawDays(), ChronoUnit.DAYS);
    }

    private Instant aggregateCutoff(Instant now) {
        return now.minus(properties.getAggregateDays(), ChronoUnit.DAYS);
    }

    private Instant deduplicationCutoff(Instant now) {
        return now.minus(properties.getDeduplicationDays(), ChronoUnit.DAYS);
    }

    public record RetentionCleanupResult(
            int rawUsageEventsDeleted,
            int rawFieldEventsDeleted,
            int aggregateRowsDeleted,
            int deduplicationRowsDeleted) {

        public int total() {
            return rawUsageEventsDeleted
                    + rawFieldEventsDeleted
                    + aggregateRowsDeleted
                    + deduplicationRowsDeleted;
        }
    }
}