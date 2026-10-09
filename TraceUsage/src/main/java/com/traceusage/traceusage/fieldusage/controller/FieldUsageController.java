package com.traceusage.traceusage.fieldusage.controller;

import com.traceusage.traceusage.fieldusage.dto.FieldUsageBatchRequest;
import com.traceusage.traceusage.fieldusage.service.FieldUsageService;
import com.traceusage.traceusage.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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
            @Valid @RequestBody FieldUsageBatchRequest request,
            HttpServletRequest httpRequest) {
        printRequest(publicIngestKey, request, httpRequest);
        fieldUsageService.collectBatch(publicIngestKey, request);
        System.out.println("FIELD USAGE BATCH SAVED TO DATABASE");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Field usage batch collected successfully", null));
    }

    private void printRequest(String publicIngestKey,
                              FieldUsageBatchRequest request,
                              HttpServletRequest httpRequest) {
        System.out.println("=============================================");
        System.out.println("RECEIVED FIELD USAGE REQUEST FROM DEMO WEB");
        System.out.println("Origin: " + httpRequest.getHeader("Origin"));
        System.out.println("PublicKey: " + maskPublicKey(publicIngestKey));
        System.out.println("BatchId: " + request.batchId());
        System.out.println("ClientId: " + request.clientId());
        System.out.println("ClientVersion: " + request.clientVersion());
        System.out.println("ProjectId: " + request.projectId());
        System.out.println("ObservedAt: " + request.observedAt());
        System.out.println("Responses: " + request.responses().size());
        System.out.println("BODY: " + request);

        request.responses().forEach(response -> {
            System.out.println("---- RESPONSE ----");
            System.out.println("Method: " + response.method());
            System.out.println("Endpoint: " + response.endpoint());
            System.out.println("Schema: " + response.schema());
            System.out.println("ObservedFields: " + response.observedFields());
            System.out.println("AccessedFields: " + response.fields());

            response.fields().forEach(field -> System.out.println(
                    "ACCESS -> endpoint=" + response.endpoint()
                            + " fieldPath=" + field.fieldPath()
                            + " accessCount=" + field.accessCount()));
        });
        System.out.println("=============================================");
    }

    private String maskPublicKey(String publicIngestKey) {
        if (publicIngestKey == null || publicIngestKey.isBlank()) {
            return "<missing>";
        }

        String trimmed = publicIngestKey.trim();
        if (trimmed.length() <= 16) {
            return "<invalid>";
        }

        return trimmed.substring(0, 16) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}