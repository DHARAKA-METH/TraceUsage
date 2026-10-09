package com.traceusage.traceusage.lifecycle.evaluator;

import com.traceusage.traceusage.lifecycle.enums.LifecycleStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Pure business logic. Does not access the database, which keeps it fully unit testable.
 */
@Component
public class LifecycleStatusEvaluator {

    public LifecycleStatus evaluate(LifecycleEvaluationInput input, Instant now) {
        int thresholdDays = input.inactivityThresholdDays();

        if (input.monitoringStartedAt() == null || !input.monitoringCoverageSufficient()) {
            return LifecycleStatus.NEWLY_MONITORED;
        }

        long monitoringAgeDays = ChronoUnit.DAYS.between(input.monitoringStartedAt(), now);

        if (monitoringAgeDays < thresholdDays) {
            return LifecycleStatus.NEWLY_MONITORED;
        }

        boolean recentlyUsed = isRecentlyUsed(input.lastSeen(), now, thresholdDays);

        if (!input.deprecated()) {
            return recentlyUsed ? LifecycleStatus.ACTIVE : LifecycleStatus.INACTIVE;
        }

        if (recentlyUsed) {
            return LifecycleStatus.DEPRECATED_ACTIVE;
        }

        boolean deprecationPeriodComplete = isDeprecationPeriodComplete(
                input.deprecatedAt(), now, thresholdDays);

        if (deprecationPeriodComplete
                && input.clientCoverageSufficient()
                && input.activeClientCount() == 0) {
            return LifecycleStatus.REMOVAL_CANDIDATE;
        }

        return LifecycleStatus.DEPRECATED_INACTIVE;
    }

    private boolean isRecentlyUsed(Instant lastSeen, Instant now, int thresholdDays) {
        if (lastSeen == null) {
            return false;
        }

        return !lastSeen.isBefore(now.minus(thresholdDays, ChronoUnit.DAYS));
    }

    private boolean isDeprecationPeriodComplete(Instant deprecatedAt, Instant now, int thresholdDays) {
        if (deprecatedAt == null) {
            return false;
        }

        return !deprecatedAt.isAfter(now.minus(thresholdDays, ChronoUnit.DAYS));
    }
}