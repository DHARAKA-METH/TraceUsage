package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.lifecycle.service.EndpointRegistryService;
import com.traceusage.traceusage.messaging.dto.EndpointTelemetryMessage;
import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import com.traceusage.traceusage.telemetry.repository.UsageEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public EndpointTelemetryPersistenceService(UsageEventRepository usageEventRepository,
                                               EndpointRegistryService endpointRegistry,
                                               ApplicationRepository applicationRepository) {
        this.usageEventRepository = usageEventRepository;
        this.endpointRegistry = endpointRegistry;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public void saveIfNew(Application application, UsageEventRequest request) {
        if (usageEventRepository.existsByEventId(request.eventId())) {
            return;
        }

        saveEntityIfNew(toEntity(application, request));
    }

    @Transactional
    public void saveBatchIfNew(Application application, Collection<UsageEventRequest> requests) {
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
        for (UsageEventRequest event : uniqueRequestEvents) {
            if (existingEventIds.contains(event.eventId()) || savedInThisBatch.contains(event.eventId())) {
                continue;
            }

            saveEntityIfNew(toEntity(application, event));
            savedInThisBatch.add(event.eventId());
        }
    }

    @Transactional
    public void saveIfNew(EndpointTelemetryMessage message) {
        if (message.schemaVersion() != EndpointTelemetryMessage.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported endpoint telemetry schema version: "
                    + message.schemaVersion());
        }

        Application application = applicationRepository.findById(message.applicationId())
                .orElseThrow(ApplicationNotFoundException::new);

        saveEntityIfNew(UsageEvent.create(
                message.eventId(),
                application,
                normalizeMethod(message.method()),
                message.endpoint().trim(),
                message.statusCode(),
                message.occurredAt()));
    }

    private void saveEntityIfNew(UsageEvent usageEvent) {
        try {
            UsageEvent saved = usageEventRepository.save(usageEvent);
            endpointRegistry.recordUsage(usageEvent.getApplication(), saved);
        } catch (DataIntegrityViolationException exception) {
            if (!usageEventRepository.existsByEventId(usageEvent.getEventId())) {
                throw exception;
            }
        }
    }

    private UsageEvent toEntity(Application application, UsageEventRequest request) {
        return UsageEvent.create(
                request.eventId(),
                application,
                normalizeMethod(request.method()),
                request.endpoint().trim(),
                request.statusCode(),
                request.occurredAt());
    }

    private String normalizeMethod(String method) {
        return method.trim().toUpperCase(Locale.ROOT);
    }
}
