package com.traceusage.sdk.telemetry;

import com.traceusage.sdk.config.TraceUsageProperties;
import org.springframework.web.client.RestClient;

import java.util.List;

public class TelemetryHttpClient {

    private final RestClient restClient;
    private final TraceUsageProperties properties;

    public TelemetryHttpClient(TraceUsageProperties properties) {
        this.restClient = RestClient.builder().build();
        this.properties = properties;
    }

    public void sendBatch(List<UsageEvent> events) {
        if (!properties.isEnabled() || events.isEmpty()) {
            return;
        }

        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return;
        }

        String batchEndpoint = properties.getBatchEndpoint();
        if (batchEndpoint == null || batchEndpoint.isBlank()) {
            return;
        }

        restClient.post()
                .uri(batchEndpoint)
                .header("X-TraceUsage-Key", properties.getApiKey())
                .body(new BatchUsageEventRequest(events))
                .retrieve()
                .toBodilessEntity();
    }
}
