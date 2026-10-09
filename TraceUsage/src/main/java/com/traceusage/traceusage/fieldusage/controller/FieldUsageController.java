package com.traceusage.traceusage.fieldusage.controller;

import com.traceusage.traceusage.fieldusage.dto.FieldUsageBatchRequest;
import com.traceusage.traceusage.fieldusage.service.FieldUsageService;
import com.traceusage.traceusage.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/field-events")
public class FieldUsageController {

    private static final String PUBLIC_KEY_HEADER = "X-TraceUsage-Public-Key";

    private final FieldUsageService fieldUsageService;

    public FieldUsageController(FieldUsageService fieldUsageService) {
        this.fieldUsageService = fieldUsageService;
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> collectBatch(
            @RequestHeader(name = PUBLIC_KEY_HEADER, required = false) String publicIngestKey,
            @Valid @RequestBody FieldUsageBatchRequest request) {
        fieldUsageService.collectBatch(publicIngestKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Field usage batch collected successfully", null));
    }
}
