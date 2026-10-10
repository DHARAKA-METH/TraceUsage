package com.traceusage.traceusage.openapi.parser;

import com.traceusage.traceusage.openapi.exception.InvalidOpenApiImportException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenApiParserServiceTest {

    private final OpenApiParserService parserService = new OpenApiParserService();

    @Test
    void shouldParseEndpointAndResponseFieldsFromYamlOpenApi() {
        String openApi = """
                openapi: 3.0.0
                info:
                  title: Product API
                  version: 1.0.0
                paths:
                  /api/products/{id}:
                    get:
                      operationId: getProduct
                      summary: Get product
                      responses:
                        '200':
                          description: OK
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Product'
                components:
                  schemas:
                    Product:
                      type: object
                      required:
                        - id
                        - name
                      properties:
                        id:
                          type: string
                          description: Product id
                        name:
                          type: string
                        price:
                          type: number
                        legacyCode:
                          type: string
                          deprecated: true
                """;

        ParsedOpenApiDocument result = parserService.parse(openApi);

        assertThat(result.endpoints()).hasSize(1);
        assertThat(result.endpoints().get(0))
                .extracting(
                        ParsedOpenApiEndpoint::method,
                        ParsedOpenApiEndpoint::path,
                        ParsedOpenApiEndpoint::operationId,
                        ParsedOpenApiEndpoint::summary,
                        ParsedOpenApiEndpoint::deprecated)
                .containsExactly("GET", "/api/products/{id}", "getProduct", "Get product", false);

        assertThat(result.fields())
                .extracting(ParsedOpenApiField::fieldPath)
                .containsExactlyInAnyOrder("id", "name", "price", "legacyCode");

        ParsedOpenApiField id = field(result, "id");
        assertThat(id.schemaName()).isEqualTo("Product");
        assertThat(id.fieldType()).isEqualTo("string");
        assertThat(id.required()).isTrue();
        assertThat(id.deprecated()).isFalse();
        assertThat(id.description()).isEqualTo("Product id");

        ParsedOpenApiField legacyCode = field(result, "legacyCode");
        assertThat(legacyCode.deprecated()).isTrue();
        assertThat(legacyCode.required()).isFalse();
    }

    @Test
    void shouldFlattenNestedObjectsAndArrays() {
        String openApi = """
                openapi: 3.0.0
                info:
                  title: Product API
                  version: 1.0.0
                paths:
                  /api/products:
                    get:
                      deprecated: true
                      responses:
                        '200':
                          description: OK
                          content:
                            application/json:
                              schema:
                                type: array
                                items:
                                  $ref: '#/components/schemas/Product'
                components:
                  schemas:
                    Product:
                      type: object
                      properties:
                        id:
                          type: string
                        metadata:
                          type: object
                          properties:
                            brand:
                              type: string
                        variants:
                          type: array
                          items:
                            type: object
                            properties:
                              sku:
                                type: string
                """;

        ParsedOpenApiDocument result = parserService.parse(openApi);

        assertThat(result.endpoints()).hasSize(1);
        assertThat(result.endpoints().get(0).deprecated()).isTrue();

        assertThat(result.fields())
                .extracting(ParsedOpenApiField::fieldPath)
                .contains("[].id", "[].metadata", "[].metadata.brand", "[].variants", "[].variants[].sku");

        assertThat(field(result, "[].variants").fieldType()).isEqualTo("array");
        assertThat(field(result, "[].metadata").fieldType()).isEqualTo("object");
    }

    @Test
    void shouldParseJsonOpenApi() {
        String openApi = """
                {
                  "openapi": "3.0.0",
                  "info": { "title": "Product API", "version": "1.0.0" },
                  "paths": {
                    "/api/products": {
                      "post": {
                        "operationId": "createProduct",
                        "responses": {
                          "201": {
                            "description": "Created",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "type": "object",
                                  "properties": {
                                    "id": { "type": "string" },
                                    "name": { "type": "string" }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """;

        ParsedOpenApiDocument result = parserService.parse(openApi);

        assertThat(result.endpoints()).hasSize(1);
        assertThat(result.endpoints().get(0).method()).isEqualTo("POST");
        assertThat(result.fields())
                .extracting(ParsedOpenApiField::fieldPath)
                .containsExactlyInAnyOrder("id", "name");
    }

    @Test
    void shouldRejectEmptyDocument() {
        assertThatThrownBy(() -> parserService.parse(" "))
                .isInstanceOf(InvalidOpenApiImportException.class)
                .hasMessage("OpenAPI document is empty");
    }

    @Test
    void shouldRejectInvalidOpenApiDocument() {
        assertThatThrownBy(() -> parserService.parse("not: openapi"))
                .isInstanceOf(InvalidOpenApiImportException.class)
                .hasMessageContaining("Invalid OpenAPI document");
    }

    private ParsedOpenApiField field(ParsedOpenApiDocument document, String fieldPath) {
        return document.fields().stream()
                .filter(field -> field.fieldPath().equals(fieldPath))
                .findFirst()
                .orElseThrow();
    }
}
