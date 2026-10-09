package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.apikey.service.ApiKeyAuthenticationService;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.lifecycle.service.EndpointRegistryService;
import com.traceusage.traceusage.telemetry.dto.BatchUsageEventRequest;
import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import com.traceusage.traceusage.telemetry.repository.UsageEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TelemetryServiceImpl implements TelemetryService {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final UsageEventRepository usageEventRepository;
    private final EndpointRegistryService endpointRegistry;

    public TelemetryServiceImpl(ApiKeyAuthenticationService apiKeyAuthenticationService,
                                UsageEventRepository usageEventRepository,
                                EndpointRegistryService endpointRegistry) {
        this.apiKeyAuthenticationService = apiKeyAuthenticationService;
        this.usageEventRepository = usageEventRepository;
        this.endpointRegistry = endpointRegistry;
    }

    @Override
    @Transactional
    public void collect(String apiKey, UsageEventRequest request) {
        Application application = apiKeyAuthenticationService.authenticate(apiKey);

        if (usageEventRepository.existsByEventId(request.eventId())) {
            return;
        }

        UsageEvent usageEvent = toEntity(application, request);

        try {
            UsageEvent saved = usageEventRepository.save(usageEvent);
            endpointRegistry.recordUsage(application, saved);
        } catch (DataIntegrityViolationException exception) {
            if (usageEventRepository.existsByEventId(request.eventId())) {
                return;
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public void collectBatch(String apiKey, BatchUsageEventRequest request) {
        Application application = apiKeyAuthenticationService.authenticate(apiKey);

        LinkedHashMap<UUID, UsageEventRequest> uniqueEventsById = new LinkedHashMap<>();
        request.events().forEach(event -> uniqueEventsById.putIfAbsent(event.eventId(), event));
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

            try {
                UsageEvent saved = usageEventRepository.save(toEntity(application, event));
                endpointRegistry.recordUsage(application, saved);
                savedInThisBatch.add(event.eventId());
            } catch (DataIntegrityViolationException exception) {
                if (!usageEventRepository.existsByEventId(event.eventId())) {
                    throw exception;
                }
            }
        }
    }

    private UsageEvent toEntity(Application application, UsageEventRequest request) {
        return UsageEvent.create(
                request.eventId(),
                application,
                request.method().trim().toUpperCase(Locale.ROOT),
                request.endpoint().trim(),
                request.statusCode(),
                request.occurredAt());
    }
}
