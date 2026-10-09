package com.traceusage.traceusage.lifecycle.evaluator;

import com.traceusage.traceusage.lifecycle.enums.LifecycleStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LifecycleStatusEvaluatorTest {

    private static final int THRESHOLD_DAYS = 90;

    private final LifecycleStatusEvaluator evaluator = new LifecycleStatusEvaluator();

    private static final Instant NOW = Instant.parse("2026-10-09T00:00:00Z");

    @Test
    void shouldReturnNewlyMonitoredWhenMonitoringHistoryIsShorterThanThreshold() {
        LifecycleEvaluationInput input = input(
                NOW.minus(10, ChronoUnit.DAYS),
                NOW.minus(2, ChronoUnit.DAYS),
                false,
                null,
                0,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.NEWLY_MONITORED);
    }

    @Test
    void shouldReturnNewlyMonitoredWhenMonitoringCoverageIsInsufficient() {
        LifecycleEvaluationInput input = input(
                NOW.minus(200, ChronoUnit.DAYS),
                NOW.minus(120, ChronoUnit.DAYS),
                false,
                null,
                0,
                false,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.NEWLY_MONITORED);
    }

    @Test
    void shouldReturnNewlyMonitoredWhenMonitoringStartIsUnknown() {
        LifecycleEvaluationInput input = input(null, null, false, null, 0, true, true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.NEWLY_MONITORED);
    }

    @Test
    void shouldReturnActiveWhenEndpointWasUsedYesterday() {
        LifecycleEvaluationInput input = input(
                NOW.minus(200, ChronoUnit.DAYS),
                NOW.minus(1, ChronoUnit.DAYS),
                false,
                null,
                0,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.ACTIVE);
    }

    @Test
    void shouldReturnActiveAtThresholdBoundary() {
        LifecycleEvaluationInput input = input(
                NOW.minus(200, ChronoUnit.DAYS),
                NOW.minus(90, ChronoUnit.DAYS),
                false,
                null,
                0,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.ACTIVE);
    }

    @Test
    void shouldReturnInactiveJustAfterThreshold() {
        LifecycleEvaluationInput input = input(
                NOW.minus(200, ChronoUnit.DAYS),
                NOW.minus(91, ChronoUnit.DAYS),
                false,
                null,
                0,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.INACTIVE);
    }

    @Test
    void shouldReturnInactiveWhenEndpointWasNeverObserved() {
        LifecycleEvaluationInput input = input(
                NOW.minus(200, ChronoUnit.DAYS),
                null,
                false,
                null,
                0,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.INACTIVE);
    }

    @Test
    void shouldReturnDeprecatedActiveWhenDeprecatedEndpointIsStillUsed() {
        LifecycleEvaluationInput input = input(
                NOW.minus(200, ChronoUnit.DAYS),
                NOW,
                true,
                NOW.minus(30, ChronoUnit.DAYS),
                2,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.DEPRECATED_ACTIVE);
    }

    @Test
    void shouldReturnDeprecatedInactiveWhenDeprecationPeriodIsTooShort() {
        LifecycleEvaluationInput input = input(
                NOW.minus(200, ChronoUnit.DAYS),
                NOW.minus(120, ChronoUnit.DAYS),
                true,
                NOW.minus(1, ChronoUnit.DAYS),
                0,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.DEPRECATED_INACTIVE);
    }

    @Test
    void shouldReturnRemovalCandidateWhenAllEvidenceGatesAreSatisfied() {
        LifecycleEvaluationInput input = input(
                NOW.minus(250, ChronoUnit.DAYS),
                NOW.minus(120, ChronoUnit.DAYS),
                true,
                NOW.minus(150, ChronoUnit.DAYS),
                0,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.REMOVAL_CANDIDATE);
    }

    @Test
    void shouldNotReturnRemovalCandidateWhenClientCoverageIsUnknown() {
        LifecycleEvaluationInput input = input(
                NOW.minus(250, ChronoUnit.DAYS),
                NOW.minus(120, ChronoUnit.DAYS),
                true,
                NOW.minus(150, ChronoUnit.DAYS),
                0,
                true,
                false);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.DEPRECATED_INACTIVE);
    }

    @Test
    void shouldNotReturnRemovalCandidateWhenKnownClientsAreStillActive() {
        LifecycleEvaluationInput input = input(
                NOW.minus(250, ChronoUnit.DAYS),
                NOW.minus(120, ChronoUnit.DAYS),
                true,
                NOW.minus(150, ChronoUnit.DAYS),
                1,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.DEPRECATED_INACTIVE);
    }

    @Test
    void shouldNotReturnRemovalCandidateWhenDeprecationDateIsMissing() {
        LifecycleEvaluationInput input = input(
                NOW.minus(250, ChronoUnit.DAYS),
                NOW.minus(120, ChronoUnit.DAYS),
                true,
                null,
                0,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.DEPRECATED_INACTIVE);
    }

    @Test
    void shouldReturnToActiveWhenInactiveEndpointReceivesNewRequest() {
        LifecycleEvaluationInput input = input(
                NOW.minus(250, ChronoUnit.DAYS),
                NOW.minus(1, ChronoUnit.DAYS),
                false,
                null,
                0,
                true,
                true);

        assertThat(evaluator.evaluate(input, NOW)).isEqualTo(LifecycleStatus.ACTIVE);
    }

    private LifecycleEvaluationInput input(Instant monitoringStartedAt,
                                           Instant lastSeen,
                                           boolean deprecated,
                                           Instant deprecatedAt,
                                           int activeClientCount,
                                           boolean monitoringCoverageSufficient,
                                           boolean clientCoverageSufficient) {
        return new LifecycleEvaluationInput(
                monitoringStartedAt,
                lastSeen,
                THRESHOLD_DAYS,
                deprecated,
                deprecatedAt,
                activeClientCount,
                monitoringCoverageSufficient,
                clientCoverageSufficient);
    }
}