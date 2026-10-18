package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.environment.entity.Environment;
import com.traceusage.traceusage.environment.repository.EnvironmentRepository;
import com.traceusage.traceusage.aggregation.EndpointUsageAggregationService;
import com.traceusage.traceusage.aggregation.EndpointUsageAggregationService.PendingEndpointEvent;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.lifecycle.service.EndpointRegistryService;
import com.traceusage.traceusage.messaging.dto.EndpointTelemetryMessage;
import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import com.traceusage.traceusage.telemetry.repository.UsageEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EndpointTelemetryPersistenceService {

    private final UsageEventRepository usageEventRepository;
    private final EndpointRegistryService endpointRegistry;
    private final ApplicationRepository applicationRepository;
    private final EnvironmentRepository environmentRepository;
    private final EndpointUsageAggregationService aggregationService;

    public EndpointTelemetryPersistenceService(UsageEventRepository usageEventRepository,
                                               EndpointRegistryService endpointRegistry,
                                               ApplicationRepository applicationRepository,
                                               EnvironmentRepository environmentRepository,
                                               EndpointUsageAggregationService aggregationService) {
        this.usageEventRepository = usageEventRepository;
        this.endpointRegistry = endpointRegistry;
        this.applicationRepository = applicationRepository;
        this.environmentRepository = environmentRepository;
        this.aggregationService = aggregationService;
    }

    @Transactional
    public void saveIfNew(Application application, UsageEventRequest request) {
        saveIfNew(application, defaultEnvironment(application), request);
    }

    @Transactional
    public void saveIfNew(Application application, Environment environment, UsageEventRequest request) {
        if (usageEventRepository.existsByEventId(request.eventId())) {
            return;
        }

        saveEntityIfNew(toEntity(application, environment, request));
    }

    @Transactional
    public void saveBatchIfNew(Application application, Collection<UsageEventRequest> requests) {
        saveBatchIfNew(application, defaultEnvironment(application), requests);
    }

    @Transactional
    public void saveBatchIfNew(Application application, Environment environment, Collection<UsageEventRequest> requests) {
        LinkedHashMap<UUID, UsageEventRequest> uniqueEventsById = new LinkedHashMap<>();
        requests.forEach(event -> uniqueEventsById.putIfAbsent(event.eventId(), event));
        List<UsageEventRequest> uniqueRequestEvents = uniqueEventsById.values().stream().toList();

        Set<UUID> requestEventIds = uniqueRequestEvents.stream()
                .map(UsageEventRequest::eventId)
                .collect(Collectors.toSet());
        Set<UUID> existingEventIds = requestEventIds.isEmpty()
                ? Set.of()
                : usageEventRepository.findExistingEventIds(requestEventIds);

        Set<UUID> savedInThisBatch = new HashSet<>();
        List<PendingEndpointEvent> newlyAggregated = new ArrayList<>();
        for (UsageEventRequest event : uniqueRequestEvents) {
            if (existingEventIds.contains(event.eventId()) || savedInThisBatch.contains(event.eventId())) {
                continue;
            }

            savedInThisBatch.add(event.eventId());

            UsageEvent saved;
            try {
                saved = usageEventRepository.save(toEntity(application, environment, event));
            } catch (DataIntegrityViolationException exception) {
                if (!usageEventRepository.existsByEventId(event.eventId())) {
                    throw exception;
                }
                continue;
            }

            newlyAggregated.add(new PendingEndpointEvent(
                    endpointRegistry.recordUsage(application, environment, saved), saved));
        }

        aggregationService.aggregateAllIfNew(newlyAggregated);
    }

    @Transactional
    public void saveIfNew(EndpointTelemetryMessage message) {
        if (message.schemaVersion() != EndpointTelemetryMessage.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported endpoint telemetry schema version: "
                    + message.schemaVersion());
        }

        Application application = applicationRepository.findById(message.applicationId())
                .orElseThrow(ApplicationNotFoundException::new);
        Environment environment = message.environmentId() == null
                ? defaultEnvironment(application)
                : environmentRepository.findById(message.environmentId())
                .filter(candidate -> candidate.getApplication().getId().equals(application.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Telemetry environment does not belong to application"));

        saveEntityIfNew(UsageEvent.create(
                message.eventId(),
                application, environment,
                normalizeMethod(message.method()),
                message.endpoint().trim(),
                message.statusCode(),
                message.occurredAt()));
    }

    private void saveEntityIfNew(UsageEvent usageEvent) {
        try {
            UsageEvent saved = usageEventRepository.save(usageEvent);
            ApiEndpoint endpoint = endpointRegistry.recordUsage(usageEvent.getApplication(), saved);
            aggregationService.aggregateIfNew(endpoint, saved);
        } catch (DataIntegrityViolationException exception) {
            if (!usageEventRepository.existsByEventId(usageEvent.getEventId())) {
                throw exception;
            }
        }
    }

    private UsageEvent toEntity(Application application, Environment environment, UsageEventRequest request) {
        return UsageEvent.create(
                request.eventId(),
                application, environment,
                normalizeMethod(request.method()),
                request.endpoint().trim(),
                request.statusCode(),
                request.occurredAt());
    }

    private Environment defaultEnvironment(Application application) {
        return environmentRepository.findByApplicationIdAndName(application.getId(), application.getEnvironment())
                .orElseThrow(() -> new IllegalStateException("Application has no default environment"));
    }

    private String normalizeMethod(String method) {
        return method.trim().toUpperCase(Locale.ROOT);
    }
}
