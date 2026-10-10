package com.traceusage.traceusage.analytics.service;

import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsItem;
import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsResponse;
import com.traceusage.traceusage.analytics.dto.FieldAnalyticsItem;
import com.traceusage.traceusage.analytics.dto.FieldAnalyticsResponse;
import com.traceusage.traceusage.analytics.dto.SchemaFieldAnalyticsItem;
import com.traceusage.traceusage.analytics.dto.SchemaFieldAnalyticsResponse;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.telemetry.repository.UsageEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ApplicationRepository applicationRepository;
    private final UsageEventRepository usageEventRepository;
    private final JdbcTemplate jdbcTemplate;

    public AnalyticsServiceImpl(ApplicationRepository applicationRepository,
                                UsageEventRepository usageEventRepository,
                                JdbcTemplate jdbcTemplate) {
        this.applicationRepository = applicationRepository;
        this.usageEventRepository = usageEventRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public EndpointAnalyticsResponse getEndpointAnalytics(Long ownerId,
                                                          String projectId,
                                                          Instant from,
                                                          Instant to) {
        Instant effectiveFrom = from == null ? Instant.EPOCH : from;
        Instant effectiveTo = to == null ? Instant.now() : to;

        Application application = applicationRepository
                .findByProjectIdAndOwnerId(projectId, ownerId)
                .orElseThrow(ApplicationNotFoundException::new);

        List<EndpointAnalyticsItem> endpoints = usageEventRepository.findEndpointAnalytics(
                application.getId(),
                effectiveFrom,
                effectiveTo);

        return new EndpointAnalyticsResponse(
                application.getProjectId(),
                application.getName(),
                application.getEnvironment(),
                endpoints);
    }

    @Override
    public FieldAnalyticsResponse getFieldAnalytics(Long ownerId,
                                                    String projectId,
                                                    String endpoint,
                                                    String clientId,
                                                    String clientVersion,
                                                    Instant from,
                                                    Instant to) {
        Instant effectiveFrom = from == null ? Instant.EPOCH : from;
        Instant effectiveTo = to == null ? Instant.now() : to;

        Application application = applicationRepository
                .findByProjectIdAndOwnerId(projectId, ownerId)
                .orElseThrow(ApplicationNotFoundException::new);

        List<FieldAnalyticsItem> fields = queryFieldAnalytics(
                application.getId(),
                normalizeBlank(endpoint),
                normalizeBlank(clientId),
                normalizeBlank(clientVersion),
                effectiveFrom,
                effectiveTo);

        return new FieldAnalyticsResponse(
                application.getProjectId(),
                application.getName(),
                application.getEnvironment(),
                fields);
    }

    @Override
    public SchemaFieldAnalyticsResponse getSchemaFieldAnalytics(Long ownerId,
                                                               String projectId,
                                                               String endpoint,
                                                               Instant from,
                                                               Instant to) {
        Instant effectiveFrom = from == null ? Instant.EPOCH : from;
        Instant effectiveTo = to == null ? Instant.now() : to;

        Application application = applicationRepository
                .findByProjectIdAndOwnerId(projectId, ownerId)
                .orElseThrow(ApplicationNotFoundException::new);

        List<SchemaFieldAnalyticsItem> fields = querySchemaFieldAnalytics(
                application.getId(),
                normalizeBlank(endpoint),
                effectiveFrom,
                effectiveTo);

        return new SchemaFieldAnalyticsResponse(
                application.getProjectId(),
                application.getName(),
                application.getEnvironment(),
                fields);
    }

    private List<FieldAnalyticsItem> queryFieldAnalytics(Long applicationId,
                                                         String endpoint,
                                                         String clientId,
                                                         String clientVersion,
                                                         Instant from,
                                                         Instant to) {
        StringBuilder sql = new StringBuilder("""
                select
                    o.http_method,
                    o.endpoint,
                    nullif(o.schema_name, '') as schema_name,
                    o.field_path,
                    f.client_id,
                    f.client_version,
                    coalesce(sum(f.access_count), 0) as total_accesses,
                    min(o.first_seen) as first_seen,
                    max(o.last_seen) as last_seen,
                    max(f.observed_at) as last_accessed
                from observed_fields o
                left join field_usage_events f
                  on f.application_id = o.application_id
                 and f.http_method = o.http_method
                 and f.endpoint = o.endpoint
                 and coalesce(f.schema_name, '') = coalesce(o.schema_name, '')
                 and f.field_path = o.field_path
                 and f.observed_at >= ?
                 and f.observed_at <= ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(Timestamp.from(from));
        params.add(Timestamp.from(to));

        if (clientId != null) {
            sql.append(" and f.client_id = ?\n");
            params.add(clientId);
        }
        if (clientVersion != null) {
            sql.append(" and f.client_version = ?\n");
            params.add(clientVersion);
        }

        sql.append("""
                where o.application_id = ?
                  and o.first_seen <= ?
                  and o.last_seen >= ?
                """);
        params.add(applicationId);
        params.add(Timestamp.from(to));
        params.add(Timestamp.from(from));

        if (endpoint != null) {
            sql.append(" and o.endpoint = ?\n");
            params.add(endpoint);
        }

        sql.append("""
                group by
                    o.http_method,
                    o.endpoint,
                    o.schema_name,
                    o.field_path,
                    f.client_id,
                    f.client_version
                order by o.endpoint, o.field_path, total_accesses desc
                """);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            long totalAccesses = rs.getLong("total_accesses");
            Timestamp firstSeen = rs.getTimestamp("first_seen");
            Timestamp lastSeen = rs.getTimestamp("last_seen");
            Timestamp lastAccessed = rs.getTimestamp("last_accessed");

            return new FieldAnalyticsItem(
                    rs.getString("http_method"),
                    rs.getString("endpoint"),
                    rs.getString("schema_name"),
                    rs.getString("field_path"),
                    rs.getString("client_id"),
                    rs.getString("client_version"),
                    totalAccesses,
                    firstSeen == null ? null : firstSeen.toInstant(),
                    lastSeen == null ? null : lastSeen.toInstant(),
                    lastAccessed == null ? null : lastAccessed.toInstant(),
                    totalAccesses > 0 ? "ACCESS_OBSERVED" : "NO_ACCESS_OBSERVED");
        }, params.toArray());
    }

    private List<SchemaFieldAnalyticsItem> querySchemaFieldAnalytics(Long applicationId,
                                                                    String endpoint,
                                                                    Instant from,
                                                                    Instant to) {
        StringBuilder sql = new StringBuilder("""
                with latest_spec as (
                    select id
                    from openapi_specs
                    where application_id = ?
                    order by imported_at desc, id desc
                    limit 1
                ), runtime_usage as (
                    select
                        application_id,
                        http_method,
                        endpoint,
                        field_path,
                        sum(access_count) as total_accesses,
                        max(observed_at) as last_accessed
                    from field_usage_events
                    where application_id = ?
                      and observed_at >= ?
                      and observed_at <= ?
                    group by application_id, http_method, endpoint, field_path
                )
                select
                    oe.http_method,
                    oe.endpoint_path,
                    f.response_status,
                    f.content_type,
                    f.schema_name,
                    f.field_path,
                    f.field_type,
                    f.required,
                    f.nullable,
                    f.deprecated,
                    coalesce(r.total_accesses, 0) as total_accesses,
                    r.last_accessed
                from openapi_response_fields f
                join openapi_endpoints oe on oe.id = f.openapi_endpoint_id
                join latest_spec ls on ls.id = f.spec_id
                left join runtime_usage r
                  on r.application_id = f.application_id
                 and r.http_method = oe.http_method
                 and r.endpoint = oe.endpoint_path
                 and r.field_path = f.field_path
                where f.application_id = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(applicationId);
        params.add(applicationId);
        params.add(Timestamp.from(from));
        params.add(Timestamp.from(to));
        params.add(applicationId);

        if (endpoint != null) {
            sql.append(" and oe.endpoint_path = ?\n");
            params.add(endpoint);
        }

        sql.append("""
                order by oe.endpoint_path, oe.http_method, f.field_path
                """);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            long totalAccesses = rs.getLong("total_accesses");
            boolean deprecated = rs.getBoolean("deprecated");
            Timestamp lastAccessed = rs.getTimestamp("last_accessed");

            return new SchemaFieldAnalyticsItem(
                    rs.getString("http_method"),
                    rs.getString("endpoint_path"),
                    rs.getString("response_status"),
                    rs.getString("content_type"),
                    rs.getString("schema_name"),
                    rs.getString("field_path"),
                    rs.getString("field_type"),
                    rs.getBoolean("required"),
                    rs.getBoolean("nullable"),
                    deprecated,
                    totalAccesses,
                    lastAccessed == null ? null : lastAccessed.toInstant(),
                    schemaFieldStatus(totalAccesses, deprecated));
        }, params.toArray());
    }

    private String schemaFieldStatus(long totalAccesses, boolean deprecated) {
        if (totalAccesses > 0) {
            return "ACCESS_OBSERVED";
        }

        return deprecated
                ? "DEPRECATED_NO_ACCESS_OBSERVED"
                : "NO_ACCESS_OBSERVED";
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
