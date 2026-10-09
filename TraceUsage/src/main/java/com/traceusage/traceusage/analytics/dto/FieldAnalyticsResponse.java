package com.traceusage.traceusage.analytics.dto;

import java.util.List;

public record FieldAnalyticsResponse(
        String projectId,
        String applicationName,
        String environment,
        List<FieldAnalyticsItem> fields) {
}
