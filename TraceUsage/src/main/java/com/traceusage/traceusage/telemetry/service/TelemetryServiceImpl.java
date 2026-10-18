package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.apikey.service.ApiKeyAuthenticationService;
import com.traceusage.traceusage.apikey.entity.ApiKey;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.environment.entity.Environment;
import com.traceusage.traceusage.messaging.config.IngestionProperties;
import com.traceusage.traceusage.messaging.dto.EndpointTelemetryMessage;
import com.traceusage.traceusage.messaging.exception.KafkaIngestionException;
import com.traceusage.traceusage.messaging.producer.EndpointEventPublisher;
import com.traceusage.traceusage.telemetry.dto.BatchUsageEventRequest;
import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TelemetryServiceImpl implements TelemetryService {

    private final ApiKeyAuthenticationService apiKeyAuthenticationService;
    private final EndpointTelemetryPersistenceService persistenceService;
    private final ObjectProvider<EndpointEventPublisher> endpointEventPublisher;
    private final IngestionProperties ingestionProperties;

    public TelemetryServiceImpl(ApiKeyAuthenticationService apiKeyAuthenticationService,
                                EndpointTelemetryPersistenceService persistenceService,
                                ObjectProvider<EndpointEventPublisher> endpointEventPublisher,
                                IngestionProperties ingestionProperties) {
        this.apiKeyAuthenticationService = apiKeyAuthenticationService;
        this.persistenceService = persistenceService;
        this.endpointEventPublisher = endpointEventPublisher;
        this.ingestionProperties = ingestionProperties;
    }

    @Override
    public void collect(String apiKey, UsageEventRequest request) {
        ApiKey authenticatedKey = apiKeyAuthenticationService.authenticateKey(apiKey);
        Application application = authenticatedKey.getApplication();
        Environment environment = requireEnvironment(authenticatedKey);

        if (ingestionProperties.isKafkaMode()) {
            publishBatch(application, environment, List.of(request));
            return;
        }

        persistenceService.saveIfNew(application, environment, request);
    }

    @Override
    public void collectBatch(String apiKey, BatchUsageEventRequest request) {
        ApiKey authenticatedKey = apiKeyAuthenticationService.authenticateKey(apiKey);
        Application application = authenticatedKey.getApplication();
        Environment environment = requireEnvironment(authenticatedKey);
        LinkedHashMap<UUID, UsageEventRequest> uniqueEventsById = new LinkedHashMap<>();
        request.events().forEach(event -> uniqueEventsById.putIfAbsent(event.eventId(), event));
        List<UsageEventRequest> uniqueRequestEvents = uniqueEventsById.values().stream().toList();

        if (ingestionProperties.isKafkaMode()) {
            publishBatch(application, environment, uniqueRequestEvents);
            return;
        }

        persistenceService.saveBatchIfNew(application, environment, uniqueRequestEvents);
    }

    private void publishBatch(Application application, Environment environment, List<UsageEventRequest> events) {
        CompletableFuture<?>[] futures = events.stream()
                .map(event -> kafkaPublisher().publish(EndpointTelemetryMessage.create(
                        event.eventId(),
                        application.getId(),
                        environment.getId(),
                        event.method().trim().toUpperCase(Locale.ROOT),
                        event.endpoint().trim(),
                        event.statusCode(),
                        event.occurredAt())))
                .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(futures)
                    .orTimeout(5, TimeUnit.SECONDS)
                    .join();
            System.out.println("KAFKA ENDPOINT EVENTS ACCEPTED -> count=" + events.size());
        } catch (RuntimeException exception) {
            throw new KafkaIngestionException("Kafka endpoint telemetry publication failed", exception);
        }
    }

    private Environment requireEnvironment(ApiKey apiKey) {
        if (apiKey.getEnvironment() == null) throw new IllegalStateException("API key has no environment binding");
        return apiKey.getEnvironment();
    }

    private EndpointEventPublisher kafkaPublisher() {
        EndpointEventPublisher publisher = endpointEventPublisher.getIfAvailable();
        if (publisher == null) {
            throw new KafkaIngestionException("Kafka endpoint publisher is not available", null);
        }
        return publisher;
    }
}
