package com.demo.traceusage;

import java.util.List;

public record BatchUsageEventRequest(List<UsageEvent> events) {
}
