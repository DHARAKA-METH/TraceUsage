package com.traceusage.traceusage.analytics.dto;

import java.time.Instant;

public record EndpointAnalyticsItem(
        String method,
        String endpoint,
        Long totalRequests,
        Long successCount,
        Long failureCount,
        Instant lastSeen) {
}
