package com.traceusage.traceusage.telemetry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchUsageEventRequest(
        @NotEmpty @Size(max = 100) List<@Valid UsageEventRequest> events) {
}
