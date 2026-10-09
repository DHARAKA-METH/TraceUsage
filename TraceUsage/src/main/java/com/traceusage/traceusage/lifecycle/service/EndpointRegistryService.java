package com.traceusage.traceusage.lifecycle.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.lifecycle.repository.ApiEndpointRepository;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the endpoint registry in sync with observed V1 telemetry.
 * Endpoints are registered lazily the first time they are seen.
 */
@Component
public class EndpointRegistryService {

    private final ApiEndpointRepository endpointRepository;

    public EndpointRegistryService(ApiEndpointRepository endpointRepository) {
        this.endpointRepository = endpointRepository;
    }

    @Transactional
    public void recordUsage(Application application, UsageEvent usageEvent) {
        String httpMethod = usageEvent.getHttpMethod();
        String endpointPath = usageEvent.getEndpoint();

        ApiEndpoint endpoint = endpointRepository
                .findByApplicationIdAndHttpMethodAndEndpointPath(
                        application.getId(), httpMethod, endpointPath)
                .orElseGet(() -> endpointRepository.save(
                        ApiEndpoint.register(application, httpMethod, endpointPath)));

        endpoint.recordUsage(usageEvent.getOccurredAt(), true);

        if (application.getMonitoringStartedAt() == null
                || usageEvent.getOccurredAt().isBefore(application.getMonitoringStartedAt())) {
            application.updateMonitoringStartedAt(usageEvent.getOccurredAt());
        }
    }
}