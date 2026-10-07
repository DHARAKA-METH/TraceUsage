package com.traceusage.traceusage.analytics.dto;

import java.time.Instant;

public record EndpointAnalyticsItem(
        String method,
        String endpoint,
        long totalRequests,
        long successCount,
        long failureCount,
        Instant lastSeen) {
}
