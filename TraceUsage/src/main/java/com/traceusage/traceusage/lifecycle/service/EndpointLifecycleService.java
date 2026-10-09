package com.traceusage.traceusage.lifecycle.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.lifecycle.dto.EndpointLifecycleResponse;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.lifecycle.entity.EndpointDeprecation;
import com.traceusage.traceusage.lifecycle.enums.LifecycleStatus;
import com.traceusage.traceusage.lifecycle.evaluator.LifecycleEvaluationInput;
import com.traceusage.traceusage.lifecycle.evaluator.LifecycleStatusEvaluator;
import com.traceusage.traceusage.lifecycle.exception.EndpointNotFoundException;
import com.traceusage.traceusage.lifecycle.repository.ApiEndpointRepository;
import com.traceusage.traceusage.lifecycle.repository.EndpointDeprecationRepository;
import com.traceusage.traceusage.lifecycle.service.KnownClientAnalyzer.KnownClientActivity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EndpointLifecycleService {

    private final ApiEndpointRepository endpointRepository;
    private final EndpointDeprecationRepository deprecationRepository;
    private final LifecycleStatusEvaluator statusEvaluator;
    private final KnownClientAnalyzer knownClientAnalyzer;
    private final Clock clock;

    public EndpointLifecycleService(ApiEndpointRepository endpointRepository,
                                    EndpointDeprecationRepository deprecationRepository,
                                    LifecycleStatusEvaluator statusEvaluator,
                                    KnownClientAnalyzer knownClientAnalyzer) {
        this.endpointRepository = endpointRepository;
        this.deprecationRepository = deprecationRepository;
        this.statusEvaluator = statusEvaluator;
        this.knownClientAnalyzer = knownClientAnalyzer;
        this.clock = Clock.systemUTC();
    }

    public List<EndpointLifecycleResponse> getAllEndpointLifecycles(Application application) {
        return endpointRepository
                .findAllByApplicationIdOrderByEndpointPathAscHttpMethodAsc(application.getId())
                .stream()
                .map(endpoint -> evaluate(application, endpoint))
                .toList();
    }

    public EndpointLifecycleResponse getEndpointLifecycle(Application application, Long endpointId) {
        ApiEndpoint endpoint = endpointRepository
                .findByIdAndApplicationId(endpointId, application.getId())
                .orElseThrow(EndpointNotFoundException::new);

        return evaluate(application, endpoint);
    }

    private EndpointLifecycleResponse evaluate(Application application, ApiEndpoint endpoint) {
        Instant now = Instant.now(clock);
        int thresholdDays = application.getInactivityThresholdDays() == null
                ? 90
                : application.getInactivityThresholdDays();

        EndpointDeprecation deprecation = deprecationRepository
                .findByEndpointId(endpoint.getId())
                .orElse(null);

        Instant lastSeen = endpoint.getLastSeenAt();

        boolean monitoringCoverageSufficient = coversFullThresholdWindow(
                application, now, thresholdDays);

        KnownClientActivity clientActivity = knownClientAnalyzer.analyze(
                endpoint,
                now.minus(thresholdDays, ChronoUnit.DAYS));

        LifecycleEvaluationInput input = new LifecycleEvaluationInput(
                application.getMonitoringStartedAt(),
                lastSeen,
                thresholdDays,
                deprecation != null,
                deprecation == null ? null : deprecation.getDeprecatedAt(),
                clientActivity.activeClientCount(),
                monitoringCoverageSufficient,
                clientActivity.clientCoverageSufficient());

        LifecycleStatus status = statusEvaluator.evaluate(input, now);

        return new EndpointLifecycleResponse(
                endpoint.getId(),
                endpoint.getHttpMethod(),
                endpoint.getEndpointPath(),
                endpoint.getRequestCount(),
                endpoint.getFirstSeenAt(),
                lastSeen,
                application.getMonitoringStartedAt(),
                thresholdDays,
                deprecation != null,
                deprecation == null ? null : deprecation.getDeprecatedAt(),
                deprecation == null ? null : deprecation.getReason(),
                deprecation == null ? null : deprecation.getReplacementEndpoint(),
                deprecation == null ? null : deprecation.getTargetRemovalDate(),
                clientActivity.activeClientCount(),
                monitoringCoverageSufficient,
                clientActivity.clientCoverageSufficient(),
                status,
                buildReason(status, monitoringCoverageSufficient, clientActivity));
    }

    private boolean coversFullThresholdWindow(Application application, Instant now, int thresholdDays) {
        Instant monitoringStartedAt = application.getMonitoringStartedAt();

        if (monitoringStartedAt == null) {
            return false;
        }

        return ChronoUnit.DAYS.between(monitoringStartedAt, now) >= thresholdDays;
    }

    private String buildReason(LifecycleStatus status,
                               boolean monitoringCoverageSufficient,
                               KnownClientActivity clientActivity) {
        return switch (status) {
            case NEWLY_MONITORED -> "Insufficient monitoring history to draw a conclusion";
            case ACTIVE -> "Requests observed inside the inactivity threshold";
            case INACTIVE -> "No requests observed inside the inactivity threshold";
            case DEPRECATED_ACTIVE -> "Deprecated but still receiving requests";
            case DEPRECATED_INACTIVE -> buildInactiveReason(
                    monitoringCoverageSufficient, clientActivity);
            case REMOVAL_CANDIDATE ->
                    "Eligible for manual removal review. This is evidence, not an instruction to delete";
        };
    }

    private String buildInactiveReason(boolean monitoringCoverageSufficient,
                                       KnownClientActivity clientActivity) {
        if (!monitoringCoverageSufficient) {
            return "Insufficient monitoring coverage";
        }

        if (!clientActivity.clientCoverageSufficient()) {
            return "No client-level telemetry observed for this endpoint";
        }

        if (clientActivity.activeClientCount() > 0) {
            return "Known clients are still using this endpoint";
        }

        return "Deprecation waiting period not complete";
    }
}