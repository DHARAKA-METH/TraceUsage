package com.traceusage.traceusage.telemetry.controller;

import com.traceusage.traceusage.shared.response.ApiResponse;
import com.traceusage.traceusage.telemetry.dto.IdentifiedApplicationResponse;
import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;
import com.traceusage.traceusage.telemetry.service.TelemetryAuthenticationService;
import com.traceusage.traceusage.telemetry.service.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class TelemetryController {

    private static final String API_KEY_HEADER = "X-TraceUsage-Key";

    private final TelemetryService telemetryService;
    private final TelemetryAuthenticationService telemetryAuthenticationService;

    public TelemetryController(TelemetryService telemetryService,
                               TelemetryAuthenticationService telemetryAuthenticationService) {
        this.telemetryService = telemetryService;
        this.telemetryAuthenticationService = telemetryAuthenticationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> collect(
            @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey,
            @Valid @RequestBody UsageEventRequest request) {
        telemetryService.collect(apiKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event collected successfully", null));
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
