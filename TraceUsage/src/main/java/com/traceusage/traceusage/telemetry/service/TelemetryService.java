package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;

public interface TelemetryService {

    void collect(String apiKey, UsageEventRequest request);
}
