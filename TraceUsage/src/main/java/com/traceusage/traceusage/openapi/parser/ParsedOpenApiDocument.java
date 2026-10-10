package com.traceusage.traceusage.openapi.parser;

import java.util.List;

public record ParsedOpenApiDocument(
        List<ParsedOpenApiEndpoint> endpoints,
        List<ParsedOpenApiField> fields) {
}
