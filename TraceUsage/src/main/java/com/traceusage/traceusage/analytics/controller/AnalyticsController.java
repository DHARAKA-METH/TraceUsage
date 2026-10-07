package com.traceusage.traceusage.analytics.controller;

import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsResponse;
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
}
