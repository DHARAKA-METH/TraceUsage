package com.traceusage.traceusage.lifecycle.evaluator;

import java.time.Instant;

/**
 * All facts required to evaluate an endpoint lifecycle.
 *
 * @param monitoringStartedAt          when reliable telemetry for this application began, may be null
 * @param lastSeen                     last observed request, may be null when never observed
 * @param inactivityThresholdDays      configured inactivity window in days
 * @param deprecated                   whether a developer recorded a deprecation
 * @param deprecatedAt                 when the deprecation was recorded, may be null
 * @param activeClientCount            distinct known clients with recent requests
 * @param monitoringCoverageSufficient true when monitoring history covers the whole threshold window
 * @param clientCoverageSufficient     true when client-level identity data exists for this endpoint
 */
public record LifecycleEvaluationInput(
        Instant monitoringStartedAt,
        Instant lastSeen,
        int inactivityThresholdDays,
        boolean deprecated,
        Instant deprecatedAt,
        int activeClientCount,
        boolean monitoringCoverageSufficient,
        boolean clientCoverageSufficient) {
}