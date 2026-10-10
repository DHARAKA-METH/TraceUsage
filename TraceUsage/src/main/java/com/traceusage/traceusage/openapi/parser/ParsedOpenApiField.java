package com.traceusage.traceusage.openapi.parser;

public record ParsedOpenApiField(
        String method,
        String endpointPath,
        String responseStatus,
        String contentType,
        String schemaName,
        String fieldPath,
        String fieldType,
        boolean required,
        boolean nullable,
        boolean deprecated,
        String description) {
}
