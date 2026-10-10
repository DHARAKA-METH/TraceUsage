package com.traceusage.traceusage.openapi.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.openapi.dto.OpenApiImportResponse;
import com.traceusage.traceusage.openapi.exception.InvalidOpenApiImportException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Locale;

@Service
public class OpenApiImportService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    public OpenApiImportResponse importOpenApi(Application application, MultipartFile file) {
        validate(file);

        return new OpenApiImportResponse(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                Instant.now(),
                "ACCEPTED_FOR_IMPORT");
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
