package com.traceusage.traceusage.telemetry.controller;

import com.traceusage.traceusage.shared.response.ApiResponse;
import com.traceusage.traceusage.telemetry.dto.IdentifiedApplicationResponse;
import com.traceusage.traceusage.telemetry.service.TelemetryAuthenticationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class TelemetryController {

    private static final String API_KEY_HEADER = "X-TraceUsage-Key";

    private final TelemetryAuthenticationService telemetryAuthenticationService;

    public TelemetryController(TelemetryAuthenticationService telemetryAuthenticationService) {
        this.telemetryAuthenticationService = telemetryAuthenticationService;
    }

    @PostMapping
    public ApiResponse<IdentifiedApplicationResponse> identifySingleEventApplication(
            @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey) {
        return identify(apiKey);
    }

    @PostMapping("/batch")
    public ApiResponse<IdentifiedApplicationResponse> identifyBatchEventApplication(
            @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey) {
        return identify(apiKey);
    }

    private ApiResponse<IdentifiedApplicationResponse> identify(String apiKey) {
        return ApiResponse.success(
                "Application identified successfully",
                telemetryAuthenticationService.identifyApplication(apiKey));
    }
}
