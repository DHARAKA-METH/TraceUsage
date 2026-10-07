package com.traceusage.sdk.telemetry;

import com.traceusage.sdk.config.TraceUsageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

public class TelemetryHttpClient {

    private static final Logger log = LoggerFactory.getLogger(TelemetryHttpClient.class);

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
            log.warn("TraceUsage telemetry skipped: api key is not configured");
            return;
        }

        String batchEndpoint = properties.getBatchEndpoint();
        if (batchEndpoint == null || batchEndpoint.isBlank()) {
            log.warn("TraceUsage telemetry skipped: server url is not configured");
            return;
        }

        log.info("TraceUsage sending {} event(s) to {} using api key {}",
                events.size(), batchEndpoint, maskApiKey(properties.getApiKey()));

        restClient.post()
                .uri(batchEndpoint)
                .header("X-TraceUsage-Key", properties.getApiKey())
                .body(new BatchUsageEventRequest(events))
                .retrieve()
                .toBodilessEntity();
    }

    private String maskApiKey(String apiKey) {
        String trimmedApiKey = apiKey.trim();
        if (trimmedApiKey.length() <= 12) {
            return "***";
        }

        return trimmedApiKey.substring(0, 10) + "..." + trimmedApiKey.substring(trimmedApiKey.length() - 4);
    }
}
