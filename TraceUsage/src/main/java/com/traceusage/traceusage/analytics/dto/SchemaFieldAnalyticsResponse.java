package com.traceusage.traceusage.analytics.dto;

import java.util.List;

public record SchemaFieldAnalyticsResponse(
        String projectId,
        String applicationName,
        String environment,
        List<SchemaFieldAnalyticsItem> fields) {
}
