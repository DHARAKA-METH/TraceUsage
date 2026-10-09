package com.traceusage.traceusage.fieldusage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FieldUsageBatchRequest(
        @NotNull UUID batchId,
        @Size(max = 100) String projectId,
        @NotBlank @Size(max = 150) String clientId,
        @NotBlank @Size(max = 80) String clientVersion,
        @NotNull Instant observedAt,
        @NotEmpty @Size(max = 100) List<@Valid ObservedFieldsRequest> responses) {
}
