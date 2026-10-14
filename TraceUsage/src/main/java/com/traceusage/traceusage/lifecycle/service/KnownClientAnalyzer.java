package com.traceusage.traceusage.lifecycle.service;

import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Known-client analysis based on V2 field telemetry.
 *
 * V1 endpoint events carry no client identity, so client activity can only be
 * asserted when client-level telemetry exists for the endpoint.
 */
@Component
public class KnownClientAnalyzer {

    private final JdbcTemplate jdbcTemplate;

    public KnownClientAnalyzer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public KnownClientActivity analyze(ApiEndpoint endpoint, Instant activeSince) {
        Long totalClientTelemetryRows = jdbcTemplate.queryForObject("""
                select count(*)
                from field_usage_hourly
                where application_id = ?
                  and endpoint_id = ?
                """, Long.class,
                endpoint.getApplication().getId(),
                endpoint.getId());

        if (totalClientTelemetryRows == null || totalClientTelemetryRows == 0) {
            return new KnownClientActivity(0, false);
        }

        Integer activeClientCount = jdbcTemplate.queryForObject("""
                select count(distinct client_id)
                from field_usage_hourly
                where application_id = ?
                  and endpoint_id = ?
                  and last_seen >= ?
                """, Integer.class,
                endpoint.getApplication().getId(),
                endpoint.getId(),
                Timestamp.from(activeSince));

        return new KnownClientActivity(activeClientCount == null ? 0 : activeClientCount, true);
    }

    public record KnownClientActivity(int activeClientCount, boolean clientCoverageSufficient) {
    }
}
