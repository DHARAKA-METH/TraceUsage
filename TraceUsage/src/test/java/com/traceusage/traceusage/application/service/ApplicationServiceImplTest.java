package com.traceusage.traceusage.application.service;

import com.traceusage.traceusage.apikey.entity.ApiKey;
import com.traceusage.traceusage.apikey.repository.ApiKeyRepository;
import com.traceusage.traceusage.application.dto.CreateApplicationRequest;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.user.entity.User;
import com.traceusage.traceusage.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationCredentialGenerator credentialGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    @Test
    void create_withAuthenticatedOwner_shouldStoreHashAndReturnRawKeyOnce() {
        User owner = User.register("Dharaka", "dharaka@example.com", "password-hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(credentialGenerator.generateProjectId()).thenReturn("proj_test123456789");
        when(applicationRepository.existsByProjectId("proj_test123456789")).thenReturn(false);
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(credentialGenerator.generateApiKey()).thenReturn("tru_sk_raw-secret-value");
        when(credentialGenerator.extractPrefix("tru_sk_raw-secret-value"))
                .thenReturn("tru_sk_raw-secret");
        when(passwordEncoder.encode("tru_sk_raw-secret-value")).thenReturn("stored-hash");

        var response = applicationService.create(
                7L, new CreateApplicationRequest(" Product Service ", "DEVELOPMENT"));

        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(applicationCaptor.capture());
        assertThat(applicationCaptor.getValue().getName()).isEqualTo("Product Service");
        assertThat(applicationCaptor.getValue().getEnvironment()).isEqualTo("development");
        assertThat(applicationCaptor.getValue().getOwner()).isSameAs(owner);

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(apiKeyCaptor.capture());
        assertThat(apiKeyCaptor.getValue().getKeyHash()).isEqualTo("stored-hash");
        assertThat(apiKeyCaptor.getValue().getKeyPrefix()).isEqualTo("tru_sk_raw-secret");
        assertThat(response.apiKey()).isEqualTo("tru_sk_raw-secret-value");
        assertThat(response.projectId()).isEqualTo("proj_test123456789");
    }

    @Test
    void list_shouldQueryOnlyAuthenticatedOwnersApplications() {
        User owner = User.register("Dharaka", "dharaka@example.com", "password-hash");
        Application application = Application.create(
                "proj_test", "Product Service", "development", owner);
        PageRequest pageable = PageRequest.of(0, 20);
        when(applicationRepository.findAllByOwnerId(7L, pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));

        var response = applicationService.list(7L, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).projectId()).isEqualTo("proj_test");
        verify(applicationRepository).findAllByOwnerId(7L, pageable);
    }

    @Test
    void getById_whenApplicationIsNotOwned_shouldReturnNotFound() {
        when(applicationRepository.findByIdAndOwnerId(42L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getById(7L, 42L))
                .isInstanceOf(ApplicationNotFoundException.class);
    }
}
