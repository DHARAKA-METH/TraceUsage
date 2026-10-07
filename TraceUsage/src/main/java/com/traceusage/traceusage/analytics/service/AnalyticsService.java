package com.traceusage.traceusage.analytics.service;

import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsResponse;

import java.time.Instant;

public interface AnalyticsService {

    EndpointAnalyticsResponse getEndpointAnalytics(
            Long ownerId,
            String projectId,
            Instant from,
            Instant to);
}
