package com.traceusage.sdk.service;

import com.traceusage.sdk.config.TraceUsageProperties;
import com.traceusage.sdk.telemetry.TelemetryQueue;
import com.traceusage.sdk.telemetry.UsageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TelemetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(TelemetryPublisher.class);

    private final TelemetryQueue queue;
    private final TraceUsageProperties properties;

    public TelemetryPublisher(TelemetryQueue queue, TraceUsageProperties properties) {
        this.queue = queue;
        this.properties = properties;
    }

    public void publish(UsageEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        boolean accepted = queue.publish(event);
        if (!accepted) {
            log.warn("TraceUsage telemetry queue is full; dropping event {}", event.eventId());
        }
    }
}
