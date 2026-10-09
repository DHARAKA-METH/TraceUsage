package com.traceusage.traceusage.analytics.dto;

import java.time.Instant;

public record FieldAnalyticsItem(
        String method,
        String endpoint,
        String schema,
        String fieldPath,
        String clientId,
        String clientVersion,
        long totalAccesses,
        Instant firstSeen,
        Instant lastSeen,
        Instant lastAccessed,
        String status) {
}
