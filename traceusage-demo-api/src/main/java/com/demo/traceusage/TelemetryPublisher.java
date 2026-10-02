package com.demo.traceusage;

public interface TelemetryPublisher {

    void publish(UsageEvent event);
}
