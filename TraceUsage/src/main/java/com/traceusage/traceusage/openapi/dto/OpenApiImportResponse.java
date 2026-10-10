package com.traceusage.traceusage.openapi.dto;

import java.time.Instant;

public record OpenApiImportResponse(
        String fileName,
        String contentType,
        long sizeBytes,
        Instant importedAt,
        String status) {
}
