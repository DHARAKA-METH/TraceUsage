package com.traceusage.traceusage.openapi.dto;

import java.time.Instant;

public record OpenApiImportResponse(
        String fileName,
        String contentType,
        long sizeBytes,
        Instant importedAt,
        long endpointsDiscovered,
        long fieldsDiscovered,
        long deprecatedEndpoints,
        long deprecatedFields,
        String status) {
}
