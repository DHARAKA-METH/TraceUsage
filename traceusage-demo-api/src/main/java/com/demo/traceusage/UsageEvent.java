package com.demo.traceusage;

import java.time.Instant;
import java.util.UUID;

public record UsageEvent(
        UUID eventId,
        String method,
        String endpoint,
        Integer statusCode,
        Instant occurredAt) {
}
