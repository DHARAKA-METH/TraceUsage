package com.demo.traceusage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpTelemetryPublisher implements TelemetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(HttpTelemetryPublisher.class);

    private final RestClient restClient;
    private final TraceUsageProperties properties;

    public HttpTelemetryPublisher(TraceUsageProperties properties) {
        this.restClient = RestClient.builder().build();
        this.properties = properties;
    }

    @Override
    public void publish(UsageEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.info("TraceUsage event captured but not sent because traceusage.api-key is not configured: {}", event);
            return;
        }

        restClient.post()
                .uri(properties.getEndpoint())
                .header("X-TraceUsage-Key", properties.getApiKey())
                .body(event)
                .retrieve()
                .toBodilessEntity();
    }
}
