package com.traceusage.traceusage.fieldusage.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.fieldusage.dto.FieldUsageBatchRequest;
import com.traceusage.traceusage.messaging.config.IngestionProperties;
import com.traceusage.traceusage.messaging.dto.FieldTelemetryBatchMessage;
import com.traceusage.traceusage.messaging.exception.KafkaIngestionException;
import com.traceusage.traceusage.messaging.producer.FieldEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class FieldUsageService {

    private final BrowserTelemetryAuthenticationService authenticationService;
    private final FieldUsagePersistenceService persistenceService;
    private final ObjectProvider<FieldEventPublisher> fieldEventPublisher;
    private final IngestionProperties ingestionProperties;

    public FieldUsageService(BrowserTelemetryAuthenticationService authenticationService,
                             FieldUsagePersistenceService persistenceService,
                             ObjectProvider<FieldEventPublisher> fieldEventPublisher,
                             IngestionProperties ingestionProperties) {
        this.authenticationService = authenticationService;
        this.persistenceService = persistenceService;
        this.fieldEventPublisher = fieldEventPublisher;
        this.ingestionProperties = ingestionProperties;
    }

    public void collectBatch(String publicIngestKey, FieldUsageBatchRequest request) {
        Application application = authenticationService.authenticate(publicIngestKey);
        System.out.println("AUTHENTICATED -> applicationId=" + application.getId()
                + " projectId=" + application.getProjectId());

        if (ingestionProperties.isKafkaMode()) {
            publishBatch(application, request);
            return;
        }

        persistenceService.saveIfNew(application, request);
    }

    private void publishBatch(Application application, FieldUsageBatchRequest request) {
        FieldTelemetryBatchMessage message = FieldTelemetryBatchMessage.create(
                request.batchId(),
                application.getId(),
                request.projectId(),
                request.clientId().trim(),
                request.clientVersion().trim(),
                request.observedAt(),
                request.responses().stream()
                        .map(response -> new FieldTelemetryBatchMessage.ResponseFieldsMessage(
                                response.method().trim(),
                                response.endpoint().trim(),
                                response.schema(),
                                response.observedFields(),
                                response.fields().stream()
                                        .map(field -> new FieldTelemetryBatchMessage.AccessedFieldMessage(
                                                field.fieldPath().trim(),
                                                field.accessCount()))
                                        .toList()))
                        .toList());

        try {
            kafkaPublisher().publish(message)
                    .orTimeout(5, TimeUnit.SECONDS)
                    .join();
            System.out.println("KAFKA FIELD BATCH ACCEPTED -> batchId=" + request.batchId());
        } catch (RuntimeException exception) {
            throw new KafkaIngestionException("Kafka field telemetry publication failed", exception);
        }
    }

    private FieldEventPublisher kafkaPublisher() {
        FieldEventPublisher publisher = fieldEventPublisher.getIfAvailable();
        if (publisher == null) {
            throw new KafkaIngestionException("Kafka field publisher is not available", null);
        }
        return publisher;
    }
}
