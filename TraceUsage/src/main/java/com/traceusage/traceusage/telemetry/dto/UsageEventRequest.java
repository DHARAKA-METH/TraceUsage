package com.traceusage.traceusage.telemetry.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UsageEventRequest(
        @NotNull UUID eventId,
        @NotBlank @Size(max = 10) String method,
        @NotBlank @Size(max = 500) String endpoint,
        @NotNull @Min(100) @Max(599) Integer statusCode,
        @NotNull Instant occurredAt) {
}
