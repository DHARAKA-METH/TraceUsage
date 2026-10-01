package com.traceusage.traceusage.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank
        @Pattern(
                regexp = "(?i)development|staging|production",
                message = "must be development, staging, or production")
        String environment) {
}
