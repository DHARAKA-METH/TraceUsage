package com.traceusage.traceusage.lifecycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DeprecateEndpointRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 500) String replacementEndpoint,
        LocalDate targetRemovalDate) {
}