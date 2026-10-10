package com.traceusage.traceusage.analytics.controller;

import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsResponse;
import com.traceusage.traceusage.analytics.dto.FieldAnalyticsResponse;
import com.traceusage.traceusage.analytics.dto.SchemaFieldAnalyticsResponse;
import com.traceusage.traceusage.analytics.service.AnalyticsService;
import com.traceusage.traceusage.auth.security.AuthenticatedUser;
import com.traceusage.traceusage.shared.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/applications/{projectId}/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/endpoints")
    public ApiResponse<EndpointAnalyticsResponse> getEndpointAnalytics(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to) {
        EndpointAnalyticsResponse response = analyticsService.getEndpointAnalytics(
                currentUser.id(),
                projectId,
                from,
                to);

        return ApiResponse.success("Endpoint analytics retrieved successfully", response);
    }

    @GetMapping("/fields")
    public ApiResponse<FieldAnalyticsResponse> getFieldAnalytics(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String clientVersion,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to) {
        FieldAnalyticsResponse response = analyticsService.getFieldAnalytics(
                currentUser.id(),
                projectId,
                endpoint,
                clientId,
                clientVersion,
                from,
                to);

        return ApiResponse.success("Field analytics retrieved successfully", response);
    }

    @GetMapping("/schema-fields")
    public ApiResponse<SchemaFieldAnalyticsResponse> getSchemaFieldAnalytics(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable String projectId,
            @RequestParam(required = false) String endpoint,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to) {
        SchemaFieldAnalyticsResponse response = analyticsService.getSchemaFieldAnalytics(
                currentUser.id(),
                projectId,
                endpoint,
                from,
                to);

        return ApiResponse.success("Schema field analytics retrieved successfully", response);
    }
}
