package com.traceusage.traceusage.fieldusage.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.aggregation.FieldUsageAggregationService;
import com.traceusage.traceusage.fieldusage.dto.AccessedFieldRequest;
import com.traceusage.traceusage.fieldusage.dto.FieldUsageBatchRequest;
import com.traceusage.traceusage.fieldusage.dto.ObservedFieldsRequest;
import com.traceusage.traceusage.fieldusage.entity.FieldUsageEvent;
import com.traceusage.traceusage.fieldusage.entity.ProcessedFieldUsageBatch;
import com.traceusage.traceusage.fieldusage.repository.FieldUsageRepository;
import com.traceusage.traceusage.fieldusage.repository.ObservedFieldRepository;
import com.traceusage.traceusage.fieldusage.repository.ProcessedFieldUsageBatchRepository;
import com.traceusage.traceusage.messaging.dto.FieldTelemetryBatchMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class FieldUsagePersistenceService {

    private final ObservedFieldRepository observedFieldRepository;
    private final FieldUsageRepository fieldUsageRepository;
    private final ProcessedFieldUsageBatchRepository processedBatchRepository;
    private final ApplicationRepository applicationRepository;
    private final FieldUsageAggregationService aggregationService;

    public FieldUsagePersistenceService(ObservedFieldRepository observedFieldRepository,
                                        FieldUsageRepository fieldUsageRepository,
                                        ProcessedFieldUsageBatchRepository processedBatchRepository,
                                        ApplicationRepository applicationRepository,
                                        FieldUsageAggregationService aggregationService) {
        this.observedFieldRepository = observedFieldRepository;
        this.fieldUsageRepository = fieldUsageRepository;
        this.processedBatchRepository = processedBatchRepository;
        this.applicationRepository = applicationRepository;
        this.aggregationService = aggregationService;
    }

    @Transactional
    public void saveIfNew(Application application, FieldUsageBatchRequest request) {
        if (processedBatchRepository.existsByBatchId(request.batchId())) {
            System.out.println("BATCH ALREADY PROCESSED -> skipping batchId=" + request.batchId());
            return;
        }

        try {
            processedBatchRepository.save(ProcessedFieldUsageBatch.create(request.batchId(), application));
            System.out.println("SAVED -> processed_field_usage_batches batchId=" + request.batchId());

            int observedCount = saveObservedFields(application, request);
            System.out.println("SAVED -> observed_fields rows=" + observedCount);

            int usageCount = saveFieldUsageEvents(application, request);
            System.out.println("SAVED -> field_usage_events rows=" + usageCount);

            boolean aggregateUpdated = aggregationService.aggregateBatchIfNew(application, request);
            System.out.println("SAVED -> field_usage_hourly aggregateUpdated=" + aggregateUpdated);
        } catch (DataIntegrityViolationException exception) {
            if (processedBatchRepository.existsByBatchId(request.batchId())) {
                System.out.println("BATCH ALREADY PROCESSED -> skipping duplicate batchId=" + request.batchId());
                return;
            }
            throw exception;
        }
    }

    @Transactional
    public void saveIfNew(FieldTelemetryBatchMessage message) {
        if (message.schemaVersion() != FieldTelemetryBatchMessage.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported field telemetry schema version: "
                    + message.schemaVersion());
        }

        Application application = applicationRepository.findById(message.applicationId())
                .orElseThrow(ApplicationNotFoundException::new);

        saveIfNew(application, toRequest(message));
    }

    private int saveObservedFields(Application application, FieldUsageBatchRequest request) {
        Instant seenAt = request.observedAt();
        int saved = 0;
        for (ObservedFieldsRequest response : request.responses()) {
            String httpMethod = normalizeMethod(response.method());
            String endpoint = response.endpoint().trim();
            String schemaName = normalizeSchema(response.schema());

            if (response.observedFields() == null) {
                continue;
            }

            for (String rawFieldPath : response.observedFields().stream()
                    .map(String::trim)
                    .distinct()
                    .toList()) {
                observedFieldRepository.upsertObservedField(
                        application.getId(),
                        httpMethod,
                        endpoint,
                        schemaName,
                        rawFieldPath,
                        seenAt);
                saved++;
                System.out.println("OBSERVED_FIELD -> " + httpMethod + " " + endpoint + " " + rawFieldPath);
            }
        }
        return saved;
    }

    private int saveFieldUsageEvents(Application application, FieldUsageBatchRequest request) {
        List<FieldUsageEvent> events = new ArrayList<>();

        for (ObservedFieldsRequest response : request.responses()) {
            String httpMethod = normalizeMethod(response.method());
            String endpoint = response.endpoint().trim();
            String schemaName = normalizeSchema(response.schema());

            for (AccessedFieldRequest field : response.fields()) {
                events.add(FieldUsageEvent.create(
                        application,
                        request.batchId(),
                        httpMethod,
                        endpoint,
                        schemaName,
                        field.fieldPath().trim(),
                        request.clientId().trim(),
                        request.clientVersion().trim(),
                        field.accessCount(),
                        request.observedAt()));
            }
        }

        fieldUsageRepository.saveAll(events);

        events.forEach(event -> System.out.println("FIELD_USAGE_EVENT -> "
                + event.getHttpMethod() + " " + event.getEndpoint()
                + " field=" + event.getFieldPath()
                + " client=" + event.getClientId()
                + " count=" + event.getAccessCount()));

        return events.size();
    }

    private FieldUsageBatchRequest toRequest(FieldTelemetryBatchMessage message) {
        return new FieldUsageBatchRequest(
                message.batchId(),
                message.projectId(),
                message.clientId(),
                message.clientVersion(),
                message.observedAt(),
                message.responses().stream()
                        .map(response -> new ObservedFieldsRequest(
                                response.method(),
                                response.endpoint(),
                                response.schema(),
                                response.observedFields(),
                                response.fields().stream()
                                        .map(field -> new AccessedFieldRequest(
                                                field.fieldPath(),
                                                field.accessCount()))
                                        .toList()))
                        .toList());
    }

    private String normalizeMethod(String method) {
        return method.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSchema(String schema) {
        return schema == null || schema.isBlank() ? "" : schema.trim();
    }
}
