package com.traceusage.traceusage.openapi.parser;

import com.traceusage.traceusage.openapi.exception.InvalidOpenApiImportException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OpenApiParserService {

    private static final String COMPONENT_SCHEMA_REF_PREFIX = "#/components/schemas/";

    public ParsedOpenApiDocument parse(String rawDocument) {
        if (rawDocument == null || rawDocument.isBlank()) {
            throw new InvalidOpenApiImportException("OpenAPI document is empty");
        }

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(false);
        parseOptions.setResolveFully(false);

        SwaggerParseResult result = new OpenAPIV3Parser()
                .readContents(rawDocument, null, parseOptions);

        if (result == null || result.getOpenAPI() == null) {
            String message = result == null || result.getMessages() == null || result.getMessages().isEmpty()
                    ? "Invalid OpenAPI document"
                    : "Invalid OpenAPI document: " + String.join(", ", result.getMessages());
            throw new InvalidOpenApiImportException(message);
        }

        OpenAPI openAPI = result.getOpenAPI();
        Map<String, Schema> componentSchemas = openAPI.getComponents() == null
                || openAPI.getComponents().getSchemas() == null
                ? Map.of()
                : openAPI.getComponents().getSchemas();

        List<ParsedOpenApiEndpoint> endpoints = new ArrayList<>();
        List<ParsedOpenApiField> fields = new ArrayList<>();

        if (openAPI.getPaths() == null) {
            return new ParsedOpenApiDocument(List.copyOf(endpoints), List.copyOf(fields));
        }

        openAPI.getPaths().forEach((path, pathItem) -> {
            extractOperation("GET", path, pathItem.getGet(), componentSchemas, endpoints, fields);
            extractOperation("POST", path, pathItem.getPost(), componentSchemas, endpoints, fields);
            extractOperation("PUT", path, pathItem.getPut(), componentSchemas, endpoints, fields);
            extractOperation("PATCH", path, pathItem.getPatch(), componentSchemas, endpoints, fields);
            extractOperation("DELETE", path, pathItem.getDelete(), componentSchemas, endpoints, fields);
            extractOperation("HEAD", path, pathItem.getHead(), componentSchemas, endpoints, fields);
            extractOperation("OPTIONS", path, pathItem.getOptions(), componentSchemas, endpoints, fields);
            extractOperation("TRACE", path, pathItem.getTrace(), componentSchemas, endpoints, fields);
        });

        return new ParsedOpenApiDocument(List.copyOf(endpoints), List.copyOf(fields));
    }

    private void extractOperation(String method,
                                  String path,
                                  Operation operation,
                                  Map<String, Schema> componentSchemas,
                                  List<ParsedOpenApiEndpoint> endpoints,
                                  List<ParsedOpenApiField> fields) {
        if (operation == null) {
            return;
        }

        endpoints.add(new ParsedOpenApiEndpoint(
                method,
                path,
                operation.getOperationId(),
                operation.getSummary(),
                Boolean.TRUE.equals(operation.getDeprecated())));

        if (operation.getResponses() == null) {
            return;
        }

        operation.getResponses().forEach((responseStatus, response) -> {
            if (response == null || response.getContent() == null) {
                return;
            }

            response.getContent().forEach((contentType, mediaType) -> {
                if (mediaType == null || mediaType.getSchema() == null) {
                    return;
                }

                flattenSchema(
                        method,
                        path,
                        responseStatus,
                        contentType,
                        schemaNameFromRef(mediaType.getSchema()),
                        "",
                        mediaType.getSchema(),
                        List.of(),
                        false,
                        componentSchemas,
                        new HashSet<>(),
                        fields);
            });
        });
    }

    private void flattenSchema(String method,
                               String endpointPath,
                               String responseStatus,
                               String contentType,
                               String schemaName,
                               String prefix,
                               Schema<?> schema,
                               List<String> requiredFields,
                               boolean inheritedDeprecated,
                               Map<String, Schema> componentSchemas,
                               Set<String> visitedRefs,
                               List<ParsedOpenApiField> fields) {
        if (schema == null) {
            return;
        }

        String refName = schemaNameFromRef(schema);
        String resolvedSchemaName = schemaName != null ? schemaName : refName;
        Schema<?> resolved = resolveRef(schema, componentSchemas, visitedRefs);

        if (resolved == null) {
            return;
        }

        boolean deprecated = inheritedDeprecated || Boolean.TRUE.equals(resolved.getDeprecated());

        if (isArraySchema(resolved)) {
            String arrayPrefix = prefix.isBlank() ? "[]" : prefix + "[]";
            flattenSchema(
                    method,
                    endpointPath,
                    responseStatus,
                    contentType,
                    resolvedSchemaName,
                    arrayPrefix,
                    resolved.getItems(),
                    List.of(),
                    deprecated,
                    componentSchemas,
                    new HashSet<>(visitedRefs),
                    fields);
            return;
        }

        Map<String, Schema> properties = resolved.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }

        List<String> objectRequiredFields = resolved.getRequired() == null
                ? requiredFields
                : resolved.getRequired();

        properties.forEach((fieldName, fieldSchema) -> {
            String fieldPath = prefix.isBlank() ? fieldName : prefix + "." + fieldName;
            String fieldSchemaName = schemaNameFromRef(fieldSchema);
            Schema<?> resolvedFieldSchema = resolveRef(
                    fieldSchema,
                    componentSchemas,
                    new HashSet<>(visitedRefs));

            Schema<?> fieldMetadata = resolvedFieldSchema == null ? fieldSchema : resolvedFieldSchema;
            boolean fieldDeprecated = deprecated || Boolean.TRUE.equals(fieldMetadata.getDeprecated());

            fields.add(new ParsedOpenApiField(
                    method,
                    endpointPath,
                    responseStatus,
                    contentType,
                    resolvedSchemaName,
                    fieldPath,
                    fieldType(fieldSchema, resolvedFieldSchema),
                    objectRequiredFields.contains(fieldName),
                    Boolean.TRUE.equals(fieldMetadata.getNullable()),
                    fieldDeprecated,
                    fieldMetadata.getDescription()));

            flattenSchema(
                    method,
                    endpointPath,
                    responseStatus,
                    contentType,
                    fieldSchemaName == null ? resolvedSchemaName : fieldSchemaName,
                    fieldPath,
                    fieldSchema,
                    fieldMetadata.getRequired() == null ? List.of() : fieldMetadata.getRequired(),
                    fieldDeprecated,
                    componentSchemas,
                    new HashSet<>(visitedRefs),
                    fields);
        });
    }

    private Schema<?> resolveRef(Schema<?> schema,
                                 Map<String, Schema> componentSchemas,
                                 Set<String> visitedRefs) {
        if (schema == null || schema.get$ref() == null) {
            return schema;
        }

        String ref = schema.get$ref();
        if (!ref.startsWith(COMPONENT_SCHEMA_REF_PREFIX)) {
            return schema;
        }

        if (!visitedRefs.add(ref)) {
            return null;
        }

        String schemaName = ref.substring(COMPONENT_SCHEMA_REF_PREFIX.length());
        return componentSchemas.getOrDefault(schemaName, schema);
    }

    private String schemaNameFromRef(Schema<?> schema) {
        if (schema == null || schema.get$ref() == null) {
            return null;
        }

        String ref = schema.get$ref();
        if (!ref.startsWith(COMPONENT_SCHEMA_REF_PREFIX)) {
            return null;
        }

        return ref.substring(COMPONENT_SCHEMA_REF_PREFIX.length());
    }

    private boolean isArraySchema(Schema<?> schema) {
        return "array".equals(schema.getType()) || schema.getItems() != null;
    }

    private String fieldType(Schema<?> originalSchema, Schema<?> resolvedSchema) {
        Schema<?> schema = resolvedSchema == null ? originalSchema : resolvedSchema;

        if (schema == null) {
            return null;
        }

        if (isArraySchema(schema)) {
            return "array";
        }

        if (schema.getType() != null) {
            return schema.getType();
        }

        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            return "object";
        }

        if (originalSchema != null && originalSchema.get$ref() != null) {
            return "object";
        }

        return null;
    }
}
