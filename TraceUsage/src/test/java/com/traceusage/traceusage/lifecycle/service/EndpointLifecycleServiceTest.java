package com.traceusage.traceusage.lifecycle.service;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.fieldusage.repository.FieldUsageRepository;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.lifecycle.entity.EndpointDeprecation;
import com.traceusage.traceusage.lifecycle.evaluator.LifecycleStatusEvaluator;
import com.traceusage.traceusage.lifecycle.repository.ApiEndpointRepository;
import com.traceusage.traceusage.lifecycle.repository.EndpointDeprecationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndpointLifecycleServiceTest {

    private static final Long APPLICATION_ID = 1L;
    private static final Long ENDPOINT_ID = 1L;

    @Mock
    private ApiEndpointRepository endpointRepository;

    @Mock
    private EndpointDeprecationRepository deprecationRepository;

    @Mock
    private FieldUsageRepository fieldUsageRepository;

    @Mock
    private Application application;

    private EndpointLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        lifecycleService = new EndpointLifecycleService(
                endpointRepository,
                deprecationRepository,
                new LifecycleStatusEvaluator(),
                new KnownClientAnalyzer(fieldUsageRepository));

        when(application.getId()).thenReturn(APPLICATION_ID);
        when(application.getInactivityThresholdDays()).thenReturn(90);
    }

    @Test
    void shouldReportNewlyMonitoredWhenApplicationWasJustCreated() {
        when(application.getMonitoringStartedAt()).thenReturn(Instant.now());

        ApiEndpoint endpoint = ApiEndpoint.register(application, "GET", "/api/products");
        endpoint.recordUsage(Instant.now(), true);

        stubEndpoint(endpoint);

        var response = lifecycleService.getEndpointLifecycle(application, ENDPOINT_ID);

        assertThat(response.status().name()).isEqualTo("NEWLY_MONITORED");
        assertThat(response.reason()).contains("Insufficient monitoring history");
    }

    @Test
    void shouldReportInactiveForNeverObservedEndpoint() {
        when(application.getMonitoringStartedAt())
                .thenReturn(Instant.now().minus(Duration.ofDays(400)));

        ApiEndpoint endpoint = ApiEndpoint.register(application, "GET", "/api/legacy");

        stubEndpoint(endpoint);

        var response = lifecycleService.getEndpointLifecycle(application, ENDPOINT_ID);

        assertThat(response.status().name()).isEqualTo("INACTIVE");
        assertThat(response.requestCount()).isZero();
        assertThat(response.clientCoverageSufficient()).isFalse();
    }

    @Test
    void shouldReportActiveForRecentlyUsedEndpoint() {
        when(application.getMonitoringStartedAt())
                .thenReturn(Instant.now().minus(Duration.ofDays(400)));

        ApiEndpoint endpoint = ApiEndpoint.register(application, "GET", "/api/products");
        endpoint.recordUsage(Instant.now(), true);

        stubEndpoint(endpoint);

        var response = lifecycleService.getEndpointLifecycle(application, ENDPOINT_ID);

        assertThat(response.status().name()).isEqualTo("ACTIVE");
        assertThat(response.requestCount()).isEqualTo(1);
    }

    @Test
    void shouldReportDeprecatedActiveWhenDeprecatedEndpointIsStillUsed() {
        when(application.getMonitoringStartedAt())
                .thenReturn(Instant.now().minus(Duration.ofDays(400)));

        ApiEndpoint endpoint = ApiEndpoint.register(application, "GET", "/api/legacy");
        endpoint.recordUsage(Instant.now(), true);

        stubEndpoint(endpoint);
        when(deprecationRepository.findByEndpointId(any()))
                .thenReturn(Optional.of(EndpointDeprecation.create(
                        endpoint, "Replaced by v2", "/api/v2/products", null)));

        var response = lifecycleService.getEndpointLifecycle(application, ENDPOINT_ID);

        assertThat(response.status().name()).isEqualTo("DEPRECATED_ACTIVE");
        assertThat(response.deprecated()).isTrue();
    }

    private void stubEndpoint(ApiEndpoint endpoint) {
        when(endpointRepository.findByIdAndApplicationId(ENDPOINT_ID, APPLICATION_ID))
                .thenReturn(Optional.of(endpoint));
        when(deprecationRepository.findByEndpointId(any()))
                .thenReturn(Optional.empty());
        when(fieldUsageRepository.findAllByEndpoint(any(), any(), any()))
                .thenReturn(List.of());
    }
}