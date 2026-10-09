package com.traceusage.traceusage.fieldusage.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.fieldusage.dto.AccessedFieldRequest;
import com.traceusage.traceusage.fieldusage.dto.FieldUsageBatchRequest;
import com.traceusage.traceusage.fieldusage.dto.ObservedFieldsRequest;
import com.traceusage.traceusage.fieldusage.entity.FieldUsageEvent;
import com.traceusage.traceusage.fieldusage.entity.ProcessedFieldUsageBatch;
import com.traceusage.traceusage.fieldusage.repository.FieldUsageRepository;
import com.traceusage.traceusage.fieldusage.repository.ObservedFieldRepository;
import com.traceusage.traceusage.fieldusage.repository.ProcessedFieldUsageBatchRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class FieldUsageService {

    private final BrowserTelemetryAuthenticationService authenticationService;
    private final ObservedFieldRepository observedFieldRepository;
    private final FieldUsageRepository fieldUsageRepository;
    private final ProcessedFieldUsageBatchRepository processedBatchRepository;

    public FieldUsageService(BrowserTelemetryAuthenticationService authenticationService,
                             ObservedFieldRepository observedFieldRepository,
                             FieldUsageRepository fieldUsageRepository,
                             ProcessedFieldUsageBatchRepository processedBatchRepository) {
        this.authenticationService = authenticationService;
        this.observedFieldRepository = observedFieldRepository;
        this.fieldUsageRepository = fieldUsageRepository;
        this.processedBatchRepository = processedBatchRepository;
    }

    @Transactional
    public void collectBatch(String publicIngestKey, FieldUsageBatchRequest request) {
        Application application = authenticationService.authenticate(publicIngestKey);
        System.out.println("AUTHENTICATED -> applicationId=" + application.getId()
                + " projectId=" + application.getProjectId());

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
        } catch (DataIntegrityViolationException exception) {
            if (processedBatchRepository.existsByBatchId(request.batchId())) {
                System.out.println("BATCH ALREADY PROCESSED -> skipping duplicate batchId=" + request.batchId());
                return;
            }
            throw exception;
        }
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

    private String normalizeMethod(String method) {
        return method.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSchema(String schema) {
        return schema == null || schema.isBlank() ? "" : schema.trim();
    }
}
