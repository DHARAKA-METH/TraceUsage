package com.traceusage.traceusage.analytics.service;

import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsResponse;
import com.traceusage.traceusage.analytics.dto.FieldAnalyticsResponse;

import java.time.Instant;

public interface AnalyticsService {

    EndpointAnalyticsResponse getEndpointAnalytics(
            Long ownerId,
            String projectId,
            Instant from,
            Instant to);

    FieldAnalyticsResponse getFieldAnalytics(
            Long ownerId,
            String projectId,
            String endpoint,
            String clientId,
            String clientVersion,
            Instant from,
            Instant to);
}
