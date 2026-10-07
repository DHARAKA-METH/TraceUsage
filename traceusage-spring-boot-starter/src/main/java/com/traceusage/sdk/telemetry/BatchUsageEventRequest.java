package com.traceusage.sdk.telemetry;

import java.util.List;

public record BatchUsageEventRequest(List<UsageEvent> events) {
}
