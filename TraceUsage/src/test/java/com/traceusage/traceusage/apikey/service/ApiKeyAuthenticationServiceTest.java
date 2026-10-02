package com.traceusage.traceusage.apikey.service;

import com.traceusage.traceusage.apikey.entity.ApiKey;
import com.traceusage.traceusage.apikey.exception.InvalidApiKeyException;
import com.traceusage.traceusage.apikey.repository.ApiKeyRepository;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ApiKeyHashService apiKeyHashService;

    @InjectMocks
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @Test
    void authenticate_withActiveKey_shouldReturnAssociatedApplication() {
        User owner = User.register("Dharaka", "dharaka@example.com", "password-hash");
        Application application = Application.create(
                "proj_test", "Product Service", "development", owner);
        ApiKey apiKey = ApiKey.create(application, "tru_sk_example", "key-hash");
        when(apiKeyHashService.hash("tru_sk_example-api-key")).thenReturn("key-hash");
        when(apiKeyRepository.findByKeyHashAndRevokedAtIsNull("key-hash"))
                .thenReturn(Optional.of(apiKey));

        Application authenticatedApplication =
                apiKeyAuthenticationService.authenticate("tru_sk_example-api-key");

        assertThat(authenticatedApplication).isSameAs(application);
        verify(apiKeyRepository).findByKeyHashAndRevokedAtIsNull("key-hash");
    }

    @Test
    void authenticate_withMissingKey_shouldRejectWithoutDatabaseQuery() {
        assertThatThrownBy(() -> apiKeyAuthenticationService.authenticate("  "))
                .isInstanceOf(InvalidApiKeyException.class)
                .hasMessage("Invalid API key");

        verifyNoInteractions(apiKeyHashService, apiKeyRepository);
    }

    @Test
    void authenticate_withUnknownOrRevokedKey_shouldReject() {
        when(apiKeyHashService.hash("tru_sk_unknown")).thenReturn("unknown-hash");
        when(apiKeyRepository.findByKeyHashAndRevokedAtIsNull("unknown-hash"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyAuthenticationService.authenticate("tru_sk_unknown"))
                .isInstanceOf(InvalidApiKeyException.class)
                .hasMessage("Invalid API key");
    }
}
