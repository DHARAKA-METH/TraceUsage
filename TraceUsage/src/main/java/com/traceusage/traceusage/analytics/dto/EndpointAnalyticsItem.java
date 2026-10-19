package com.traceusage.traceusage.analytics.dto;

import java.time.Instant;

public record EndpointAnalyticsItem(
        String method,
        String endpoint,
        Long totalRequests,
        Long successCount,
        Long failureCount,
        Instant lastSeen,
        java.util.Map<Integer, Long> statusCounts) {
    public EndpointAnalyticsItem(String method, String endpoint, Long totalRequests, Long successCount,
                                 Long failureCount, Instant lastSeen) {
        this(method, endpoint, totalRequests, successCount, failureCount, lastSeen, java.util.Map.of());
    }
}
