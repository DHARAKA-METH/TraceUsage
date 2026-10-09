package com.traceusage.traceusage.lifecycle.service;

import com.traceusage.traceusage.fieldusage.entity.FieldUsageEvent;
import com.traceusage.traceusage.fieldusage.repository.FieldUsageRepository;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Known-client analysis based on V2 field telemetry.
 *
 * V1 endpoint events carry no client identity, so client activity can only be
 * asserted when client-level telemetry exists for the endpoint.
 */
@Component
public class KnownClientAnalyzer {

    private final FieldUsageRepository fieldUsageRepository;

    public KnownClientAnalyzer(FieldUsageRepository fieldUsageRepository) {
        this.fieldUsageRepository = fieldUsageRepository;
    }

    @Transactional(readOnly = true)
    public KnownClientActivity analyze(ApiEndpoint endpoint, Instant activeSince) {
        List<FieldUsageEvent> events = fieldUsageRepository.findAllByEndpoint(
                endpoint.getApplication().getId(),
                endpoint.getHttpMethod(),
                endpoint.getEndpointPath());

        if (events.isEmpty()) {
            return new KnownClientActivity(0, false);
        }

        List<FieldUsageEvent> recentEvents = events.stream()
                .filter(event -> !event.getObservedAt().isBefore(activeSince))
                .toList();

        int activeClientCount = (int) recentEvents.stream()
                .map(FieldUsageEvent::getClientId)
                .distinct()
                .count();

        return new KnownClientActivity(activeClientCount, true);
    }

    public record KnownClientActivity(int activeClientCount, boolean clientCoverageSufficient) {
    }
}