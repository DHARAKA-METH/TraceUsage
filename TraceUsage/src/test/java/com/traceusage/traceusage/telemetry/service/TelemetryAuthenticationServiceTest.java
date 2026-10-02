package com.traceusage.traceusage.telemetry.service;

import com.traceusage.traceusage.apikey.service.ApiKeyAuthenticationService;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryAuthenticationServiceTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @InjectMocks
    private TelemetryAuthenticationService telemetryAuthenticationService;

    @Test
    void identifyApplication_withValidApiKey_shouldReturnIdentifiedApplication() {
        User owner = User.register("Test Developer", "developer@example.test", "fake-password-hash");
        Application application = Application.create(
                "proj_test", "Product Service", "development", owner);
        when(apiKeyAuthenticationService.authenticate("tru_sk_valid"))
                .thenReturn(application);

        var response = telemetryAuthenticationService.identifyApplication("tru_sk_valid");

        assertThat(response.name()).isEqualTo("Product Service");
        assertThat(response.environment()).isEqualTo("development");
        assertThat(response.projectId()).isEqualTo("proj_test");
        verify(apiKeyAuthenticationService).authenticate("tru_sk_valid");
    }
}
