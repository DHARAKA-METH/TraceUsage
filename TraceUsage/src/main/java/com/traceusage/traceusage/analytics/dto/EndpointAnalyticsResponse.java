package com.traceusage.traceusage.analytics.dto;

import java.util.List;

public record EndpointAnalyticsResponse(
        String projectId,
        String applicationName,
        String environment,
        List<EndpointAnalyticsItem> endpoints) {
}
