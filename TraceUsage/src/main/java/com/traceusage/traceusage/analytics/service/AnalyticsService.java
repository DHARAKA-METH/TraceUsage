package com.traceusage.traceusage.analytics.service;

import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsResponse;
import com.traceusage.traceusage.analytics.dto.FieldAnalyticsResponse;
import com.traceusage.traceusage.analytics.dto.SchemaFieldAnalyticsResponse;

import java.time.Instant;

public interface AnalyticsService {

    EndpointAnalyticsResponse getEndpointAnalytics(
            Long ownerId,
            String projectId,
            Instant from,
            Instant to, Long environmentId);

    FieldAnalyticsResponse getFieldAnalytics(
            Long ownerId,
            String projectId,
            String endpoint,
            String clientId,
            String clientVersion,
            Instant from,
            Instant to, Long environmentId);

    SchemaFieldAnalyticsResponse getSchemaFieldAnalytics(
            Long ownerId,
            String projectId,
            String endpoint,
            Instant from,
            Instant to, Long environmentId);

    default EndpointAnalyticsResponse getEndpointAnalytics(Long ownerId, String projectId, Instant from, Instant to) {
        return getEndpointAnalytics(ownerId, projectId, from, to, null);
    }
    default FieldAnalyticsResponse getFieldAnalytics(Long ownerId, String projectId, String endpoint, String clientId, String clientVersion, Instant from, Instant to) {
        return getFieldAnalytics(ownerId, projectId, endpoint, clientId, clientVersion, from, to, null);
    }
    default SchemaFieldAnalyticsResponse getSchemaFieldAnalytics(Long ownerId, String projectId, String endpoint, Instant from, Instant to) {
        return getSchemaFieldAnalytics(ownerId, projectId, endpoint, from, to, null);
    }
}
