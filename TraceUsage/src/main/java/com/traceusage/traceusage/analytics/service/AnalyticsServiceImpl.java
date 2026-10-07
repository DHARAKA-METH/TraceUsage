package com.traceusage.traceusage.analytics.service;

import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsItem;
import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsResponse;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.telemetry.repository.UsageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ApplicationRepository applicationRepository;
    private final UsageEventRepository usageEventRepository;

    public AnalyticsServiceImpl(ApplicationRepository applicationRepository,
                                UsageEventRepository usageEventRepository) {
        this.applicationRepository = applicationRepository;
        this.usageEventRepository = usageEventRepository;
    }

    @Override
    public EndpointAnalyticsResponse getEndpointAnalytics(Long ownerId,
                                                          String projectId,
                                                          Instant from,
                                                          Instant to) {
        Instant effectiveFrom = from == null ? Instant.EPOCH : from;
        Instant effectiveTo = to == null ? Instant.now() : to;

        Application application = applicationRepository
                .findByProjectIdAndOwnerId(projectId, ownerId)
                .orElseThrow(ApplicationNotFoundException::new);

        List<EndpointAnalyticsItem> endpoints = usageEventRepository.findEndpointAnalytics(
                application.getId(),
                effectiveFrom,
                effectiveTo);

        return new EndpointAnalyticsResponse(
                application.getProjectId(),
                application.getName(),
                application.getEnvironment(),
                endpoints);
    }
}
