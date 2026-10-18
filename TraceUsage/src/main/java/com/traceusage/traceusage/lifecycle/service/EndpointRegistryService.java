package com.traceusage.traceusage.lifecycle.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.environment.entity.Environment;
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
    public ApiEndpoint recordUsage(Application application, UsageEvent usageEvent) {
        if (usageEvent.getEnvironment() == null) {
            throw new IllegalStateException("Telemetry event is missing its environment");
        }
        return recordUsage(application, usageEvent.getEnvironment(), usageEvent);
    }

    @Transactional
    public ApiEndpoint recordUsage(Application application, Environment environment, UsageEvent usageEvent) {
        String httpMethod = usageEvent.getHttpMethod();
        String endpointPath = usageEvent.getEndpoint();

        ApiEndpoint endpoint = endpointRepository
                .findByEnvironmentIdAndHttpMethodAndEndpointPath(
                        environment.getId(), httpMethod, endpointPath)
                .orElseGet(() -> endpointRepository.save(
                        ApiEndpoint.register(application, environment, httpMethod, endpointPath)));

        endpoint.recordUsage(usageEvent.getOccurredAt(), true);

        environment.recordMonitoringStart(usageEvent.getOccurredAt());

        return endpoint;
    }
}
