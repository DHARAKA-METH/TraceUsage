package com.traceusage.traceusage.aggregation;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.fieldusage.dto.FieldUsageBatchRequest;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.lifecycle.repository.ApiEndpointRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Idempotent hourly aggregation of browser field telemetry.
 *
 * <p>Exactly-once semantics come from {@code processed_field_aggregation_batches},
 * keyed by the browser SDK's stable batch id. The deduplication insert and every
 * aggregate upsert for the batch run inside one PostgreSQL transaction.
 *
 * <p>The aggregation key keeps application, endpoint, HTTP method, response
 * schema, field path and client identity separate, so identical field names on
 * different endpoints are never merged and client/version attribution stays
 * accurate.
 */
@Service
public class FieldUsageAggregationService {

    private final JdbcTemplate jdbcTemplate;
    private final ApiEndpointRepository endpointRepository;
    private final Counter acceptedCounter;
    private final Counter duplicateCounter;
    private final Counter accessCounter;

    public FieldUsageAggregationService(JdbcTemplate jdbcTemplate,
                                        ApiEndpointRepository endpointRepository,
                                        ObjectProvider<MeterRegistry> meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.endpointRepository = endpointRepository;
        this.acceptedCounter = counter(meterRegistry, "traceusage.aggregation.field.batches", "accepted");
        this.duplicateCounter = counter(meterRegistry, "traceusage.aggregation.field.batches", "duplicate");
        this.accessCounter = counter(meterRegistry, "traceusage.aggregation.field.accesses", "counted");
    }

    private static Counter counter(ObjectProvider<MeterRegistry> provider, String name, String result) {
        MeterRegistry registry = provider.getIfAvailable();
        if (registry == null) {
            return null;
        }
        return Counter.builder(name).tag("result", result).register(registry);
    }

    private void increment(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }

    /**
     * Applies aggregate effects exactly once per browser batch id.
     *
     * @return true when the batch was newly aggregated, false when it was a duplicate
     */
    @Transactional
    public boolean aggregateBatchIfNew(Application application, FieldUsageBatchRequest request) {
        return aggregateBatchIfNew(application, null, request);
    }

    @Transactional
    public boolean aggregateBatchIfNew(Application application, com.traceusage.traceusage.environment.entity.Environment environment, FieldUsageBatchRequest request) {
        int accepted = jdbcTemplate.update("""
                insert into processed_field_aggregation_batches (batch_id, application_id)
                values (?, ?)
                on conflict (batch_id) do nothing
                """, request.batchId(), application.getId());

        if (accepted == 0) {
            increment(duplicateCounter);
            return false;
        }

        Map<FieldAggregateKey, Long> aggregates = new LinkedHashMap<>();
        Map<FieldAggregateKey, Long> observations = new LinkedHashMap<>();
        Instant observedAt = request.observedAt();
        Instant hourBucket = TimeBucket.hour(observedAt);
        String clientId = request.clientId().trim();
        String clientVersion = request.clientVersion().trim();

        request.responses().forEach(response -> {
            String method = response.method().trim().toUpperCase(Locale.ROOT);
            String endpointPath = response.endpoint().trim();
            String schemaName = response.schema() == null || response.schema().isBlank()
                    ? ""
                    : response.schema().trim();
            ApiEndpoint endpoint = environment == null ? endpointRepository
                    .findByApplicationIdAndHttpMethodAndEndpointPath(
                            application.getId(), method, endpointPath)
                    .orElseGet(() -> endpointRepository.save(ApiEndpoint.register(
                            application, method, endpointPath))) : endpointRepository
                    .findByEnvironmentIdAndHttpMethodAndEndpointPath(environment.getId(), method, endpointPath)
                    .orElseGet(() -> endpointRepository.save(ApiEndpoint.register(application, environment, method, endpointPath)));

            if (response.observedFields() != null) response.observedFields().stream().map(String::trim).distinct().forEach(path -> {
                FieldAggregateKey key = new FieldAggregateKey(application.getId(), endpoint.getId(), method, endpointPath,
                        schemaName, path, clientId, clientVersion, hourBucket, observedAt);
                observations.merge(key, 1L, Long::sum);
                aggregates.putIfAbsent(key, 0L);
            });
            response.fields().forEach(field -> {
                FieldAggregateKey key = new FieldAggregateKey(
                        application.getId(),
                        endpoint.getId(),
                        method,
                        endpointPath,
                        schemaName,
                        field.fieldPath().trim(),
                        clientId,
                        clientVersion,
                        hourBucket,
                        observedAt);
                aggregates.merge(key, field.accessCount().longValue(), Long::sum);
            });
        });

        aggregates.forEach((key, count) -> upsertAggregate(key, count, observations.getOrDefault(key, 0L)));

        long totalAccesses = aggregates.values().stream().mapToLong(Long::longValue).sum();
        if (accessCounter != null) {
            accessCounter.increment(totalAccesses);
        }
        increment(acceptedCounter);
        return true;
    }

    private void upsertAggregate(FieldAggregateKey key, Long accessCount, Long observedCount) {
        Timestamp seenAt = Timestamp.from(key.observedAt());
        Long fieldId = jdbcTemplate.queryForObject("""
                insert into api_fields(endpoint_id, schema_name, field_path, first_seen_at, last_seen_at)
                values (?, ?, ?, ?, ?)
                on conflict(endpoint_id, schema_name, field_path) do update set
                first_seen_at = least(api_fields.first_seen_at, excluded.first_seen_at),
                last_seen_at = greatest(api_fields.last_seen_at, excluded.last_seen_at)
                returning id
                """, Long.class, key.endpointId(), key.schemaName(), key.fieldPath(), seenAt, seenAt);
        jdbcTemplate.update("""
                insert into field_usage_hourly (
                    application_id,
                    endpoint_id,
                    http_method,
                    endpoint,
                    schema_name,
                    field_path,
                    client_id,
                    client_version,
                    hour_bucket,
                    access_count,
                    first_seen,
                    last_seen, field_id, observed_count, last_accessed
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (
                    application_id,
                    endpoint_id,
                    schema_name,
                    field_path,
                    client_id,
                    client_version,
                    hour_bucket
                ) do update set
                    access_count = field_usage_hourly.access_count + excluded.access_count,
                    field_id = excluded.field_id,
                    observed_count = field_usage_hourly.observed_count + excluded.observed_count,
                    last_accessed = greatest(field_usage_hourly.last_accessed, excluded.last_accessed),
                    first_seen = least(field_usage_hourly.first_seen, excluded.first_seen),
                    last_seen = greatest(field_usage_hourly.last_seen, excluded.last_seen)
                """,
                key.applicationId(),
                key.endpointId(),
                key.httpMethod(),
                key.endpoint(),
                key.schemaName(),
                key.fieldPath(),
                key.clientId(),
                key.clientVersion(),
                Timestamp.from(key.hourBucket()),
                accessCount,
                seenAt,
                seenAt, fieldId, observedCount, accessCount > 0 ? seenAt : null);
    }

    private record FieldAggregateKey(
            Long applicationId,
            Long endpointId,
            String httpMethod,
            String endpoint,
            String schemaName,
            String fieldPath,
            String clientId,
            String clientVersion,
            Instant hourBucket,
            Instant observedAt) {
    }
}
