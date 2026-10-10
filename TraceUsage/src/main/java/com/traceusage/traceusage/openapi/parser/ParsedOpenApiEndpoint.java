package com.traceusage.traceusage.openapi.parser;

public record ParsedOpenApiEndpoint(
        String method,
        String path,
        String operationId,
        String summary,
        boolean deprecated) {
}
