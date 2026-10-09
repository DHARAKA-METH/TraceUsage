package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.apikey.exception.InvalidApiKeyException;
import com.traceusage.traceusage.apikey.service.ApiKeyAuthenticationService;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.lifecycle.service.EndpointRegistryService;
import com.traceusage.traceusage.telemetry.dto.BatchUsageEventRequest;
import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import com.traceusage.traceusage.telemetry.repository.UsageEventRepository;
import com.traceusage.traceusage.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceImplTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @Mock
    private UsageEventRepository usageEventRepository;

    @Mock
    private EndpointRegistryService endpointRegistry;

    @InjectMocks
    private TelemetryServiceImpl telemetryService;

    @Test
    void collect_withValidEvent_shouldAuthenticateAndSaveUsageEvent() {
        Application application = application();
        UsageEventRequest request = request();
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid")).thenReturn(application);
        when(usageEventRepository.existsByEventId(request.eventId())).thenReturn(false);

        telemetryService.collect("tru_sk_valid", request);

        ArgumentCaptor<UsageEvent> eventCaptor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(usageEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getApplication()).isSameAs(application);
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo(request.eventId());
        assertThat(eventCaptor.getValue().getHttpMethod()).isEqualTo("GET");
        assertThat(eventCaptor.getValue().getEndpoint()).isEqualTo("/api/products");
        assertThat(eventCaptor.getValue().getStatusCode()).isEqualTo(200);
    }

    @Test
    void collect_withDuplicateEventId_shouldNotSaveAgain() {
        Application application = application();
        UsageEventRequest request = request();
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid")).thenReturn(application);
        when(usageEventRepository.existsByEventId(request.eventId())).thenReturn(true);

        telemetryService.collect("tru_sk_valid", request);

        verify(usageEventRepository, never()).save(any());
    }

    @Test
    void collect_withConcurrentDuplicateInsert_shouldTreatAsSuccess() {
        Application application = application();
        UsageEventRequest request = request();
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid")).thenReturn(application);
        when(usageEventRepository.existsByEventId(request.eventId()))
                .thenReturn(false)
                .thenReturn(true);
        when(usageEventRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate event_id"));

        telemetryService.collect("tru_sk_valid", request);
    }

    @Test
    void collect_withInvalidApiKey_shouldPropagateUnauthorizedFailure() {
        UsageEventRequest request = request();
        when(apiKeyAuthenticationService.authenticate("tru_sk_invalid"))
                .thenThrow(new InvalidApiKeyException("Invalid API key"));

        assertThatThrownBy(() -> telemetryService.collect("tru_sk_invalid", request))
                .isInstanceOf(InvalidApiKeyException.class);
        verify(usageEventRepository, never()).save(any());
    }

    @Test
    void collectBatch_withValidEvents_shouldAuthenticateOnceAndSaveEvents() {
        Application application = application();
        BatchUsageEventRequest request = batchRequest();
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid")).thenReturn(application);
        when(usageEventRepository.findExistingEventIds(any())).thenReturn(Set.of());

        telemetryService.collectBatch("tru_sk_valid", request);

        ArgumentCaptor<UsageEvent> eventCaptor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(apiKeyAuthenticationService).authenticate("tru_sk_valid");
        verify(usageEventRepository, times(2)).save(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(UsageEvent::getEventId)
                .containsExactly(
                        UUID.fromString("f83284af-6427-4f42-a713-29dd41449915"),
                        UUID.fromString("aee69442-b1f7-44bc-9372-840755a3e2ba"));
    }

    @Test
    void collectBatch_withExistingEventId_shouldSkipPreviouslyProcessedEvents() {
        Application application = application();
        BatchUsageEventRequest request = batchRequest();
        UUID existingEventId = UUID.fromString("f83284af-6427-4f42-a713-29dd41449915");
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid")).thenReturn(application);
        when(usageEventRepository.findExistingEventIds(any())).thenReturn(Set.of(existingEventId));

        telemetryService.collectBatch("tru_sk_valid", request);

        ArgumentCaptor<UsageEvent> eventCaptor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(usageEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId())
                .isEqualTo(UUID.fromString("aee69442-b1f7-44bc-9372-840755a3e2ba"));
    }

    @Test
    void collectBatch_withDuplicateEventIdsInsideRequest_shouldSaveOnlyOnce() {
        Application application = application();
        UsageEventRequest event = request();
        BatchUsageEventRequest request = new BatchUsageEventRequest(List.of(event, event));
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid")).thenReturn(application);
        when(usageEventRepository.findExistingEventIds(any())).thenReturn(Set.of());

        telemetryService.collectBatch("tru_sk_valid", request);

        verify(usageEventRepository).save(any());
    }

    @Test
    void collectBatch_withInvalidApiKey_shouldRejectWithoutSaving() {
        BatchUsageEventRequest request = batchRequest();
        when(apiKeyAuthenticationService.authenticate("tru_sk_invalid"))
                .thenThrow(new InvalidApiKeyException("Invalid API key"));

        assertThatThrownBy(() -> telemetryService.collectBatch("tru_sk_invalid", request))
                .isInstanceOf(InvalidApiKeyException.class);
        verify(usageEventRepository, never()).save(any());
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
