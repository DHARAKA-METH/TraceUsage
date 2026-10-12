package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.apikey.exception.InvalidApiKeyException;
import com.traceusage.traceusage.apikey.service.ApiKeyAuthenticationService;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.messaging.config.IngestionProperties;
import com.traceusage.traceusage.messaging.producer.EndpointEventPublisher;
import com.traceusage.traceusage.telemetry.dto.BatchUsageEventRequest;
import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;
import com.traceusage.traceusage.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceImplTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @Mock
    private EndpointTelemetryPersistenceService persistenceService;

    @Mock
    private ObjectProvider<EndpointEventPublisher> endpointEventPublisher;

    private IngestionProperties ingestionProperties;

    private TelemetryServiceImpl telemetryService;

    @BeforeEach
    void setUp() {
        ingestionProperties = new IngestionProperties();
        ingestionProperties.setMode("direct");
        telemetryService = new TelemetryServiceImpl(
                apiKeyAuthenticationService,
                persistenceService,
                endpointEventPublisher,
                ingestionProperties);
    }

    @Test
    void collect_withValidEventInDirectMode_shouldAuthenticateAndPersist() {
        Application application = application();
        UsageEventRequest request = request();
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid")).thenReturn(application);

        telemetryService.collect("tru_sk_valid", request);

        verify(persistenceService).saveIfNew(application, request);
        verify(endpointEventPublisher, never()).getIfAvailable();
    }

    @Test
    void collect_withInvalidApiKey_shouldPropagateUnauthorizedFailure() {
        UsageEventRequest request = request();
        when(apiKeyAuthenticationService.authenticate("tru_sk_invalid"))
                .thenThrow(new InvalidApiKeyException("Invalid API key"));

        assertThatThrownBy(() -> telemetryService.collect("tru_sk_invalid", request))
                .isInstanceOf(InvalidApiKeyException.class);
        verify(persistenceService, never()).saveIfNew(any(), any(UsageEventRequest.class));
    }

    @Test
    void collectBatch_withValidEventsInDirectMode_shouldAuthenticateAndPersistBatch() {
        Application application = application();
        BatchUsageEventRequest request = batchRequest();
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid")).thenReturn(application);

        telemetryService.collectBatch("tru_sk_valid", request);

        verify(apiKeyAuthenticationService).authenticate("tru_sk_valid");
        verify(persistenceService).saveBatchIfNew(application, request.events());
    }

    @Test
    void collectBatch_withDuplicateEventIdsInsideRequest_shouldDeduplicateBeforePersisting() {
        Application application = application();
        UsageEventRequest event = request();
        BatchUsageEventRequest request = new BatchUsageEventRequest(List.of(event, event));
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid")).thenReturn(application);

        telemetryService.collectBatch("tru_sk_valid", request);

        verify(persistenceService).saveBatchIfNew(application, List.of(event));
    }

    @Test
    void collectBatch_withInvalidApiKey_shouldRejectWithoutPersisting() {
        BatchUsageEventRequest request = batchRequest();
        when(apiKeyAuthenticationService.authenticate("tru_sk_invalid"))
                .thenThrow(new InvalidApiKeyException("Invalid API key"));

        assertThatThrownBy(() -> telemetryService.collectBatch("tru_sk_invalid", request))
                .isInstanceOf(InvalidApiKeyException.class);
        verify(persistenceService, never()).saveBatchIfNew(any(), any());
    }

    private Application application() {
        User owner = User.register("Test Developer", "developer@example.test", "fake-password-hash");
        return Application.create("proj_test", "Product Service", "development", owner);
    }

    private UsageEventRequest request() {
        return new UsageEventRequest(
                UUID.fromString("f83284af-6427-4f42-a713-29dd41449915"),
                " get ",
                " /api/products ",
                200,
                Instant.parse("2026-10-02T04:45:00Z"));
    }

    private BatchUsageEventRequest batchRequest() {
        return new BatchUsageEventRequest(List.of(
                request(),
                new UsageEventRequest(
                        UUID.fromString("aee69442-b1f7-44bc-9372-840755a3e2ba"),
                        "post",
                        "/api/products",
                        201,
                        Instant.parse("2026-10-02T09:00:01Z"))));
    }
}
