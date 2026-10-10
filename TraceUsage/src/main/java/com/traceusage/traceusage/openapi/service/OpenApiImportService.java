package com.traceusage.traceusage.openapi.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.lifecycle.repository.ApiEndpointRepository;
import com.traceusage.traceusage.openapi.dto.OpenApiImportResponse;
import com.traceusage.traceusage.openapi.entity.OpenApiEndpoint;
import com.traceusage.traceusage.openapi.entity.OpenApiResponseField;
import com.traceusage.traceusage.openapi.entity.OpenApiSpec;
import com.traceusage.traceusage.openapi.exception.InvalidOpenApiImportException;
import com.traceusage.traceusage.openapi.parser.OpenApiParserService;
import com.traceusage.traceusage.openapi.parser.ParsedOpenApiDocument;
import com.traceusage.traceusage.openapi.parser.ParsedOpenApiEndpoint;
import com.traceusage.traceusage.openapi.parser.ParsedOpenApiField;
import com.traceusage.traceusage.openapi.repository.OpenApiEndpointRepository;
import com.traceusage.traceusage.openapi.repository.OpenApiResponseFieldRepository;
import com.traceusage.traceusage.openapi.repository.OpenApiSpecRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HexFormat;

@Service
public class OpenApiImportService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    private final OpenApiParserService parserService;
    private final OpenApiSpecRepository specRepository;
    private final OpenApiEndpointRepository openApiEndpointRepository;
    private final OpenApiResponseFieldRepository responseFieldRepository;
    private final ApiEndpointRepository apiEndpointRepository;

    public OpenApiImportService(OpenApiParserService parserService,
                                OpenApiSpecRepository specRepository,
                                OpenApiEndpointRepository openApiEndpointRepository,
                                OpenApiResponseFieldRepository responseFieldRepository,
                                ApiEndpointRepository apiEndpointRepository) {
        this.parserService = parserService;
        this.specRepository = specRepository;
        this.openApiEndpointRepository = openApiEndpointRepository;
        this.responseFieldRepository = responseFieldRepository;
        this.apiEndpointRepository = apiEndpointRepository;
    }

    @Transactional
    public OpenApiImportResponse importOpenApi(Application application, MultipartFile file) {
        validate(file);

        String rawDocument = readFile(file);
        String documentHash = sha256(rawDocument);

        OpenApiSpec existingSpec = specRepository
                .findByApplicationIdAndDocumentHash(application.getId(), documentHash)
                .orElse(null);

        if (existingSpec != null) {
            return new OpenApiImportResponse(
                    existingSpec.getFileName(),
                    existingSpec.getContentType(),
                    file.getSize(),
                    existingSpec.getImportedAt(),
                    openApiEndpointRepository.countBySpecId(existingSpec.getId()),
                    responseFieldRepository.countBySpecId(existingSpec.getId()),
                    openApiEndpointRepository.countBySpecIdAndDeprecatedTrue(existingSpec.getId()),
                    responseFieldRepository.countBySpecIdAndDeprecatedTrue(existingSpec.getId()),
                    "ALREADY_IMPORTED");
        }

        ParsedOpenApiDocument parsed = parserService.parse(rawDocument);

        OpenApiSpec spec = specRepository.save(OpenApiSpec.create(
                application,
                file.getOriginalFilename(),
                file.getContentType(),
                documentHash,
                rawDocument));

        Map<String, OpenApiEndpoint> savedEndpoints = saveEndpoints(application, spec, parsed);
        long fieldsDiscovered = saveFields(application, spec, parsed, savedEndpoints);

        return new OpenApiImportResponse(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                spec.getImportedAt(),
                savedEndpoints.size(),
                fieldsDiscovered,
                parsed.endpoints().stream().filter(ParsedOpenApiEndpoint::deprecated).count(),
                parsed.fields().stream().filter(ParsedOpenApiField::deprecated).count(),
                "IMPORTED");
    }

    private Map<String, OpenApiEndpoint> saveEndpoints(Application application,
                                                       OpenApiSpec spec,
                                                       ParsedOpenApiDocument parsed) {
        Map<String, OpenApiEndpoint> savedEndpoints = new HashMap<>();

        for (ParsedOpenApiEndpoint parsedEndpoint : parsed.endpoints()) {
            ApiEndpoint apiEndpoint = apiEndpointRepository
                    .findByApplicationIdAndHttpMethodAndEndpointPath(
                            application.getId(),
                            parsedEndpoint.method(),
                            parsedEndpoint.path())
                    .orElseGet(() -> apiEndpointRepository.save(ApiEndpoint.register(
                            application,
                            parsedEndpoint.method(),
                            parsedEndpoint.path())));

            OpenApiEndpoint openApiEndpoint = openApiEndpointRepository.save(OpenApiEndpoint.create(
                    application,
                    spec,
                    apiEndpoint,
                    parsedEndpoint.method(),
                    parsedEndpoint.path(),
                    parsedEndpoint.operationId(),
                    parsedEndpoint.summary(),
                    parsedEndpoint.deprecated()));

            savedEndpoints.put(endpointKey(parsedEndpoint.method(), parsedEndpoint.path()), openApiEndpoint);
        }

        return savedEndpoints;
    }

    private long saveFields(Application application,
                            OpenApiSpec spec,
                            ParsedOpenApiDocument parsed,
                            Map<String, OpenApiEndpoint> savedEndpoints) {
        Set<String> savedFieldKeys = new HashSet<>();
        long fieldsDiscovered = 0;

        for (ParsedOpenApiField parsedField : parsed.fields()) {
            OpenApiEndpoint endpoint = savedEndpoints.get(
                    endpointKey(parsedField.method(), parsedField.endpointPath()));

            if (endpoint == null) {
                continue;
            }

            String fieldKey = endpoint.getId()
                    + "|" + parsedField.responseStatus()
                    + "|" + parsedField.contentType()
                    + "|" + parsedField.fieldPath();

            if (!savedFieldKeys.add(fieldKey)) {
                continue;
            }

            responseFieldRepository.save(OpenApiResponseField.create(
                    application,
                    spec,
                    endpoint,
                    parsedField.responseStatus(),
                    parsedField.contentType(),
                    parsedField.schemaName(),
                    parsedField.fieldPath(),
                    parsedField.fieldType(),
                    parsedField.required(),
                    parsedField.nullable(),
                    parsedField.deprecated(),
                    parsedField.description()));
            fieldsDiscovered++;
        }

        return fieldsDiscovered;
    }

    private String readFile(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new InvalidOpenApiImportException("Unable to read OpenAPI file");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String endpointKey(String method, String endpointPath) {
        return method + " " + endpointPath;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOpenApiImportException("OpenAPI file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidOpenApiImportException("OpenAPI file must be 5 MB or smaller");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new InvalidOpenApiImportException("OpenAPI file name is required");
        }

        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);

        if (!normalizedFileName.endsWith(".json")
                && !normalizedFileName.endsWith(".yaml")
                && !normalizedFileName.endsWith(".yml")) {
            throw new InvalidOpenApiImportException(
                    "OpenAPI file must be .json, .yaml, or .yml");
        }
    }
}
