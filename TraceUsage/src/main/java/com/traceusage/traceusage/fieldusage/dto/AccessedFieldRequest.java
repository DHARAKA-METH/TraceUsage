package com.traceusage.traceusage.fieldusage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccessedFieldRequest(
        @NotBlank @Size(max = 500) String fieldPath,
        @Min(1) @Max(1_000_000) Integer accessCount) {
}
