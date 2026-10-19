package com.traceusage.traceusage.messaging.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FieldTelemetryBatchMessage(
        int schemaVersion,
        UUID batchId,
        Long applicationId,
        Long environmentId,
        String projectId,
        String clientId,
        String clientVersion,
        Instant observedAt,
        List<ResponseFieldsMessage> responses) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public record ResponseFieldsMessage(
            String method,
            String endpoint,
            String schema,
            List<String> observedFields,
            List<AccessedFieldMessage> fields) {
    }

    public record AccessedFieldMessage(
            String fieldPath,
            Integer accessCount) {
    }

    public static FieldTelemetryBatchMessage create(UUID batchId,
                                                     Long applicationId,
                                                     Long environmentId,
                                                    String projectId,
                                                    String clientId,
                                                    String clientVersion,
                                                    Instant observedAt,
                                                    List<ResponseFieldsMessage> responses) {
        return new FieldTelemetryBatchMessage(
                CURRENT_SCHEMA_VERSION,
                batchId,
                applicationId,
                environmentId,
                projectId,
                clientId,
                clientVersion,
                observedAt,
                responses);
    }
}
