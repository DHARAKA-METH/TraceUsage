package com.traceusage.traceusage.lifecycle.controller;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.auth.security.AuthenticatedUser;
import com.traceusage.traceusage.lifecycle.dto.DeprecateEndpointRequest;
import com.traceusage.traceusage.lifecycle.dto.EndpointLifecycleResponse;
import com.traceusage.traceusage.lifecycle.entity.EndpointDeprecation;
import com.traceusage.traceusage.lifecycle.service.EndpointDeprecationService;
import com.traceusage.traceusage.lifecycle.service.EndpointLifecycleService;
import com.traceusage.traceusage.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications/{projectId}/endpoints")
public class EndpointLifecycleController {

    private final ApplicationRepository applicationRepository;
    private final EndpointLifecycleService lifecycleService;
    private final EndpointDeprecationService deprecationService;

    public EndpointLifecycleController(ApplicationRepository applicationRepository,
                                        EndpointLifecycleService lifecycleService,
                                        EndpointDeprecationService deprecationService) {
        this.applicationRepository = applicationRepository;
        this.lifecycleService = lifecycleService;
        this.deprecationService = deprecationService;
    }

    @GetMapping("/lifecycle")
    public ApiResponse<List<EndpointLifecycleResponse>> listLifecycle(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId) {
        Application application = requireOwnedApplication(currentUser, projectId);
        return ApiResponse.success(
                "Endpoint lifecycle retrieved successfully",
                lifecycleService.getAllEndpointLifecycles(application));
    }

    @GetMapping("/{endpointId}/lifecycle")
    public ApiResponse<EndpointLifecycleResponse> getLifecycle(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId,
            @PathVariable Long endpointId) {
        Application application = requireOwnedApplication(currentUser, projectId);
        return ApiResponse.success(
                "Endpoint lifecycle retrieved successfully",
                lifecycleService.getEndpointLifecycle(application, endpointId));
    }

    @PostMapping("/{endpointId}/deprecations")
    public ResponseEntity<ApiResponse<EndpointLifecycleResponse>> deprecate(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId,
            @PathVariable Long endpointId,
            @Valid @RequestBody DeprecateEndpointRequest request) {
        Application application = requireOwnedApplication(currentUser, projectId);
        deprecationService.deprecate(application, endpointId, request);

        EndpointLifecycleResponse lifecycle =
                lifecycleService.getEndpointLifecycle(application, endpointId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Endpoint deprecated successfully", lifecycle));
    }

    private Application requireOwnedApplication(AuthenticatedUser currentUser, String projectId) {
        return applicationRepository
                .findByProjectIdAndOwnerId(projectId, currentUser.id())
                .orElseThrow(ApplicationNotFoundException::new);
    }
}