package com.traceusage.traceusage.analytics.dto;

import java.time.Instant;

public record SchemaFieldAnalyticsItem(
        String method,
        String endpoint,
        String responseStatus,
        String contentType,
        String schemaName,
        String fieldPath,
        String fieldType,
        boolean required,
        boolean nullable,
        boolean deprecated,
        long totalAccesses,
        Instant lastAccessed,
        String status) {
}
