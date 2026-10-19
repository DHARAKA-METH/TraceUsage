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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications/{projectId}/endpoints")
public class EndpointLifecycleController {

    private final ApplicationRepository applicationRepository;
    private final EndpointLifecycleService lifecycleService;
    private final EndpointDeprecationService deprecationService;
    private final JdbcTemplate jdbc;

    public EndpointLifecycleController(ApplicationRepository applicationRepository,
                                        EndpointLifecycleService lifecycleService,
                                     EndpointDeprecationService deprecationService, JdbcTemplate jdbc) {
        this.applicationRepository = applicationRepository;
        this.lifecycleService = lifecycleService;
        this.deprecationService = deprecationService;
        this.jdbc = jdbc;
    }

    @GetMapping("/lifecycle")
    public ApiResponse<List<EndpointLifecycleResponse>> listLifecycle(
            @RequestParam(required = false) Long environmentId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId) {
        Application application = requireOwnedApplication(currentUser, projectId);
        validateEnvironment(application, environmentId, null);
        return ApiResponse.success(
                "Endpoint lifecycle retrieved successfully",
                lifecycleService.getAllEndpointLifecycles(application, environmentId));
    }

    @GetMapping("/{endpointId}/lifecycle")
    public ApiResponse<EndpointLifecycleResponse> getLifecycle(
            @RequestParam(required = false) Long environmentId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId,
            @PathVariable Long endpointId) {
        Application application = requireOwnedApplication(currentUser, projectId);
        validateEnvironment(application, environmentId, endpointId);
        return ApiResponse.success(
                "Endpoint lifecycle retrieved successfully",
                lifecycleService.getEndpointLifecycle(application, endpointId));
    }

    @PostMapping("/{endpointId}/deprecations")
    public ResponseEntity<ApiResponse<EndpointLifecycleResponse>> deprecate(
            @RequestParam(required = false) Long environmentId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId,
            @PathVariable Long endpointId,
            @Valid @RequestBody DeprecateEndpointRequest request) {
        Application application = requireOwnedApplication(currentUser, projectId);
        validateEnvironment(application, environmentId, endpointId);
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

    private void validateEnvironment(Application application, Long environmentId, Long endpointId) {
        if (environmentId == null) return;
        boolean valid = Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from environments where id = ? and application_id = ?)",
                Boolean.class, environmentId, application.getId()));
        if (valid && endpointId != null) valid = Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from api_endpoints where id = ? and environment_id = ? and application_id = ?)",
                Boolean.class, endpointId, environmentId, application.getId()));
        if (!valid) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
