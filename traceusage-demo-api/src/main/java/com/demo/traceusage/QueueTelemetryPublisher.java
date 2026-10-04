package com.demo.traceusage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QueueTelemetryPublisher implements TelemetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(QueueTelemetryPublisher.class);

    private final TelemetryQueue queue;
    private final TraceUsageProperties properties;

    public QueueTelemetryPublisher(TelemetryQueue queue, TraceUsageProperties properties) {
        this.queue = queue;
        this.properties = properties;
    }

    @Override
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
