package com.traceusage.traceusage.telemetry.dto;

import com.traceusage.traceusage.application.entity.Application;

public record IdentifiedApplicationResponse(
        Long applicationId,
        String name,
        String environment,
        String projectId) {

    public static IdentifiedApplicationResponse from(Application application) {
        return new IdentifiedApplicationResponse(
                application.getId(),
                application.getName(),
                application.getEnvironment(),
                application.getProjectId());
    }
}
