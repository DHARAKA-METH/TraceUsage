package com.traceusage.traceusage.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record EndpointTelemetryMessage(
        int schemaVersion,
        UUID eventId,
        Long applicationId,
        Long environmentId,
        String method,
        String endpoint,
        int statusCode,
        Instant occurredAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static EndpointTelemetryMessage create(UUID eventId,
                                                   Long applicationId,
                                                   Long environmentId,
                                                  String method,
                                                  String endpoint,
                                                  int statusCode,
                                                  Instant occurredAt) {
        return new EndpointTelemetryMessage(
                CURRENT_SCHEMA_VERSION,
                eventId,
                applicationId,
                environmentId,
                method,
                endpoint,
                statusCode,
                occurredAt);
    }
}
