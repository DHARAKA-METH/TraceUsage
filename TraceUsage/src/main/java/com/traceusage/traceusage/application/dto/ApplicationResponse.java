package com.traceusage.traceusage.application.dto;

import com.traceusage.traceusage.application.entity.Application;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        String name,
        String environment,
        String projectId,
        Instant createdAt) {

    public static ApplicationResponse from(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getName(),
                application.getEnvironment(),
                application.getProjectId(),
                application.getCreatedAt());
    }
}
