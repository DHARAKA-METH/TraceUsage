package com.demo.traceusage;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
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

        restClient.post()
                .uri(properties.getBatchEndpoint())
                .header("X-TraceUsage-Key", properties.getApiKey())
                .body(new BatchUsageEventRequest(events))
                .retrieve()
                .toBodilessEntity();
    }
}
