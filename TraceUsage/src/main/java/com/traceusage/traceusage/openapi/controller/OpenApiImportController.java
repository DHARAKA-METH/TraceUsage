package com.traceusage.traceusage.openapi.controller;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.auth.security.AuthenticatedUser;
import com.traceusage.traceusage.openapi.dto.OpenApiImportResponse;
import com.traceusage.traceusage.openapi.service.OpenApiImportService;
import com.traceusage.traceusage.shared.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/applications/{projectId}/openapi")
public class OpenApiImportController {

    private final ApplicationRepository applicationRepository;
    private final OpenApiImportService openApiImportService;

    public OpenApiImportController(ApplicationRepository applicationRepository,
                                   OpenApiImportService openApiImportService) {
        this.applicationRepository = applicationRepository;
        this.openApiImportService = openApiImportService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OpenApiImportResponse>> importOpenApi(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId,
            @RequestPart("file") MultipartFile file) {
        Application application = applicationRepository
                .findByProjectIdAndOwnerId(projectId, currentUser.id())
                .orElseThrow(ApplicationNotFoundException::new);

        OpenApiImportResponse response = openApiImportService.importOpenApi(application, file);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("OpenAPI import accepted", response));
    }
}
