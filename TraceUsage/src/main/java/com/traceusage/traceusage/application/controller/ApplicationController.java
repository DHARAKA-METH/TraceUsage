package com.traceusage.traceusage.application.controller;

import com.traceusage.traceusage.application.dto.ApplicationResponse;
import com.traceusage.traceusage.application.dto.CreateApplicationRequest;
import com.traceusage.traceusage.application.dto.CreateApplicationResponse;
import com.traceusage.traceusage.application.service.ApplicationService;
import com.traceusage.traceusage.auth.security.AuthenticatedUser;
import com.traceusage.traceusage.shared.response.ApiResponse;
import com.traceusage.traceusage.shared.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateApplicationResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateApplicationRequest request) {
        CreateApplicationResponse response = applicationService.create(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application created successfully", response));
    }

    @GetMapping
    public ApiResponse<PageResponse<ApplicationResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return ApiResponse.success(
                "Applications retrieved successfully",
                applicationService.list(currentUser.id(), pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationResponse> getById(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable @Positive Long id) {
        return ApiResponse.success(
                "Application retrieved successfully",
                applicationService.getById(currentUser.id(), id));
    }
}
