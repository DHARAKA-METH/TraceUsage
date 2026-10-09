package com.traceusage.traceusage.application.dto;

import com.traceusage.traceusage.application.entity.Application;

import java.time.Instant;

public record CreateApplicationResponse(
        Long id,
        String name,
        String environment,
        String projectId,
        String apiKey,
        String publicIngestKey,
        Instant createdAt) {

    public static CreateApplicationResponse from(Application application, String rawApiKey) {
        return from(application, rawApiKey, null);
    }

    public static CreateApplicationResponse from(Application application, String rawApiKey, String rawPublicIngestKey) {
        return new CreateApplicationResponse(
                application.getId(),
                application.getName(),
                application.getEnvironment(),
                application.getProjectId(),
                rawApiKey,
                rawPublicIngestKey,
                application.getCreatedAt());
    }
}
