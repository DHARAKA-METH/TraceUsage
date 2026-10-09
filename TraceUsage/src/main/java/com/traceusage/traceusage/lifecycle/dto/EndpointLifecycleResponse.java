package com.traceusage.traceusage.lifecycle.dto;

import com.traceusage.traceusage.lifecycle.enums.LifecycleStatus;

import java.time.Instant;
import java.time.LocalDate;

public record EndpointLifecycleResponse(
        Long endpointId,
        String method,
        String endpoint,
        long requestCount,
        Instant firstSeen,
        Instant lastSeen,
        Instant monitoringStartedAt,
        int inactivityThresholdDays,
        boolean deprecated,
        Instant deprecatedAt,
        String deprecationReason,
        String replacementEndpoint,
        LocalDate targetRemovalDate,
        int activeClientCount,
        boolean monitoringCoverageSufficient,
        boolean clientCoverageSufficient,
        LifecycleStatus status,
        String reason) {
}