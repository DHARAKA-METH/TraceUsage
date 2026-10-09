package com.traceusage.traceusage.fieldusage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ObservedFieldsRequest(
        @NotBlank @Size(max = 10) String method,
        @NotBlank @Size(max = 500) String endpoint,
        @Size(max = 150) String schema,
        @Size(max = 500) List<@NotBlank @Size(max = 500) String> observedFields,
        @NotEmpty @Size(max = 500) List<@Valid AccessedFieldRequest> fields) {
}
