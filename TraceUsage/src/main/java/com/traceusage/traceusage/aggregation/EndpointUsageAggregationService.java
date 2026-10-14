package com.traceusage.traceusage.aggregation;

import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Idempotent hourly aggregation of endpoint telemetry.
 *
 * <p>Exactly-once semantics come from {@code processed_endpoint_aggregation_events}:
 * the deduplication inserts and the aggregate upserts run inside the same
 * PostgreSQL transaction. A redelivered Kafka record finds its event id already
 * present, is treated as a duplicate, and contributes no extra counts.
 *
 * <p>Aggregation happens inside the bounded batch that was consumed, never in a
 * long-lived in-memory buffer, so a crash can only lose work Kafka will replay.
 */
@Service
public class EndpointUsageAggregationService {

    private static final String DEDUPLICATION_SQL = """
            insert into processed_endpoint_aggregation_events (event_id, endpoint_id)
            select claimed.event_id, claimed.endpoint_id
            from unnest(?::uuid[], ?::bigint[]) as claimed(event_id, endpoint_id)
            on conflict (event_id) do nothing
            returning event_id
            """;

    private static final int DEDUPLICATION_CHUNK_SIZE = 500;

    private static final String AGGREGATE_SQL = """
            insert into endpoint_usage_hourly (
                endpoint_id,
                hour_bucket,
                request_count,
                success_count,
                error_count,
                first_seen,
                last_seen
            ) values (?, ?, ?, ?, ?, ?, ?)
            on conflict (endpoint_id, hour_bucket)
            do update set
                request_count = endpoint_usage_hourly.request_count + excluded.request_count,
                success_count = endpoint_usage_hourly.success_count + excluded.success_count,
                error_count = endpoint_usage_hourly.error_count + excluded.error_count,
                first_seen = least(endpoint_usage_hourly.first_seen, excluded.first_seen),
                last_seen = greatest(endpoint_usage_hourly.last_seen, excluded.last_seen)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Counter acceptedCounter;
    private final Counter duplicateCounter;
    private final Counter errorCounter;
    private final Timer upsertTimer;

    public EndpointUsageAggregationService(JdbcTemplate jdbcTemplate,
                                           ObjectProvider<MeterRegistry> meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.acceptedCounter = counter(meterRegistry, "traceusage.aggregation.endpoint.events", "accepted");
        this.duplicateCounter = counter(meterRegistry, "traceusage.aggregation.endpoint.events", "duplicate");
        this.errorCounter = counter(meterRegistry, "traceusage.aggregation.endpoint.events", "error");
        this.upsertTimer = timer(meterRegistry, "traceusage.aggregation.endpoint.upsert");
    }

    private static Counter counter(ObjectProvider<MeterRegistry> provider, String name, String result) {
        MeterRegistry registry = provider.getIfAvailable();
        if (registry == null) {
            return null;
        }
        return Counter.builder(name).tag("result", result).register(registry);
    }

    private static Timer timer(ObjectProvider<MeterRegistry> provider, String name) {
        MeterRegistry registry = provider.getIfAvailable();
        if (registry == null) {
            return null;
        }
        return Timer.builder(name).publishPercentiles(0.5, 0.95).register(registry);
    }

    /**
     * Applies aggregate effects exactly once per usage event id.
     *
     * @return true when the event was newly aggregated, false when it was a duplicate
     */
    @Transactional
    public boolean aggregateIfNew(ApiEndpoint endpoint, UsageEvent event) {
        return aggregateAllIfNew(List.of(new PendingEndpointEvent(endpoint, event))) == 1;
    }

    /**
     * Applies aggregate effects for a bounded batch of events.
     *
     * <p>Deduplication and aggregation each need a constant number of round trips
     * per batch rather than two per event: the deduplication insert returns the
     * event ids that were actually claimed, and only those contribute counts.
     *
     * @return number of newly aggregated events
     */
    @Transactional
    public int aggregateAllIfNew(List<PendingEndpointEvent> pendingEvents) {
        if (pendingEvents.isEmpty()) {
            return 0;
        }

        long startedAt = System.nanoTime();
        Set<UUID> claimedEventIds = claimEventIds(pendingEvents);

        Map<AggregateKey, Aggregate> aggregates = new LinkedHashMap<>();
        int accepted = 0;

        for (PendingEndpointEvent pending : pendingEvents) {
            if (!claimedEventIds.contains(pending.event().getEventId())) {
                increment(duplicateCounter);
                continue;
            }

            accepted++;
            UsageEvent event = pending.event();
            aggregates.computeIfAbsent(
                    new AggregateKey(
                            pending.endpoint().getId(),
                            TimeBucket.hour(event.getOccurredAt())),
                    key -> new Aggregate(event.getOccurredAt()))
                    .add(event.getStatusCode(), event.getOccurredAt());
        }

        if (!aggregates.isEmpty()) {
            List<Object[]> aggregateArgs = aggregates.entrySet().stream()
                    .map(entry -> entry.getValue().toRow(entry.getKey()))
                    .toList();
            jdbcTemplate.batchUpdate(
                    AGGREGATE_SQL,
                    aggregateArgs,
                    aggregateArgs.size(),
                    (statement, args) -> {
                        for (int i = 0; i < args.length; i++) {
                            statement.setObject(i + 1, args[i]);
                        }
                    });
        }

        if (accepted > 0) {
            increment(acceptedCounter, accepted);
        }
        long errorTotal = aggregates.values().stream()
                .mapToLong(Aggregate::errorCount)
                .sum();
        if (errorTotal > 0) {
            increment(errorCounter, errorTotal);
        }
        record(upsertTimer, startedAt);
        return accepted;
    }

    private Set<UUID> claimEventIds(List<PendingEndpointEvent> pendingEvents) {
        Set<UUID> claimed = new HashSet<>();

        for (int start = 0; start < pendingEvents.size(); start += DEDUPLICATION_CHUNK_SIZE) {
            List<PendingEndpointEvent> chunk = pendingEvents.subList(
                    start, Math.min(start + DEDUPLICATION_CHUNK_SIZE, pendingEvents.size()));

            List<UUID> accepted = jdbcTemplate.query(
                    DEDUPLICATION_SQL,
                    statement -> {
                        Array eventIds = statement.getConnection()
                                .createArrayOf("uuid", chunk.stream()
                                        .map(pending -> pending.event().getEventId())
                                        .toArray());
                        Array endpointIds = statement.getConnection()
                                .createArrayOf("bigint", chunk.stream()
                                        .map(pending -> pending.endpoint().getId())
                                        .toArray());
                        statement.setArray(1, eventIds);
                        statement.setArray(2, endpointIds);
                    },
                    (resultSet, rowNum) -> resultSet.getObject(1, UUID.class));

            claimed.addAll(accepted);
        }

        return claimed;
    }

    private void increment(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }

    private void increment(Counter counter, long amount) {
        if (counter != null) {
            counter.increment(amount);
        }
    }

    private void record(Timer timer, long startedAtNanos) {
        if (timer != null) {
            timer.record(System.nanoTime() - startedAtNanos, TimeUnit.NANOSECONDS);
        }
    }

    /** An endpoint event waiting to be folded into the hourly aggregates. */
    public record PendingEndpointEvent(ApiEndpoint endpoint, UsageEvent event) {
    }

    private record AggregateKey(Long endpointId, Instant hourBucket) {
    }

    private static final class Aggregate {

        private long requestCount;
        private long successCount;
        private long errorCount;
        private Instant firstSeen;
        private Instant lastSeen;

        private Aggregate(Instant firstSeen) {
            this.firstSeen = firstSeen;
            this.lastSeen = firstSeen;
        }

        private void add(int statusCode, Instant occurredAt) {
            requestCount++;
            if (statusCode >= 400) {
                errorCount++;
            } else if (statusCode >= 200 && statusCode <= 399) {
                successCount++;
            }

            if (occurredAt.isBefore(firstSeen)) {
                firstSeen = occurredAt;
            }
            if (occurredAt.isAfter(lastSeen)) {
                lastSeen = occurredAt;
            }
        }

        private long errorCount() {
            return errorCount;
        }

        private Object[] toRow(AggregateKey key) {
            return new Object[]{
                    key.endpointId(),
                    Timestamp.from(key.hourBucket()),
                    requestCount,
                    successCount,
                    errorCount,
                    Timestamp.from(firstSeen),
                    Timestamp.from(lastSeen)};
        }
    }
}