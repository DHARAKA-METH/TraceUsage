package com.traceusage.traceusage.application.service;

import com.traceusage.traceusage.apikey.entity.ApiKey;
import com.traceusage.traceusage.apikey.repository.ApiKeyRepository;
import com.traceusage.traceusage.apikey.service.ApiKeyHashService;
import com.traceusage.traceusage.application.dto.ApplicationResponse;
import com.traceusage.traceusage.application.dto.CreateApplicationRequest;
import com.traceusage.traceusage.application.dto.CreateApplicationResponse;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.exception.ApplicationNotFoundException;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.shared.response.PageResponse;
import com.traceusage.traceusage.user.entity.User;
import com.traceusage.traceusage.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ApplicationServiceImpl implements ApplicationService {

    private static final int PROJECT_ID_GENERATION_ATTEMPTS = 10;

    private final ApplicationRepository applicationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final ApplicationCredentialGenerator credentialGenerator;
    private final ApiKeyHashService apiKeyHashService;

    public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                                  ApiKeyRepository apiKeyRepository,
                                  UserRepository userRepository,
                                  ApplicationCredentialGenerator credentialGenerator,
                                  ApiKeyHashService apiKeyHashService) {
        this.applicationRepository = applicationRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.credentialGenerator = credentialGenerator;
        this.apiKeyHashService = apiKeyHashService;
    }

    @Override
    @Transactional
    public CreateApplicationResponse create(Long ownerId, CreateApplicationRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));

        Application application = Application.create(
                generateUniqueProjectId(),
                request.name().trim(),
                request.environment().toLowerCase(Locale.ROOT),
                owner);
        Application savedApplication = applicationRepository.save(application);

        String rawApiKey = credentialGenerator.generateApiKey();
        ApiKey apiKey = ApiKey.create(
                savedApplication,
                credentialGenerator.extractPrefix(rawApiKey),
                apiKeyHashService.hash(rawApiKey));
        apiKeyRepository.save(apiKey);

        String rawPublicIngestKey = credentialGenerator.generatePublicIngestKey();
        ApiKey publicIngestKey = ApiKey.create(
                savedApplication,
                credentialGenerator.extractPrefix(rawPublicIngestKey),
                apiKeyHashService.hash(rawPublicIngestKey),
                "PUBLIC_BROWSER");
        apiKeyRepository.save(publicIngestKey);

        return CreateApplicationResponse.from(savedApplication, rawApiKey, rawPublicIngestKey);
    }

    @Override
    public PageResponse<ApplicationResponse> list(Long ownerId, Pageable pageable) {
        Page<ApplicationResponse> applications = applicationRepository
                .findAllByOwnerId(ownerId, pageable)
                .map(ApplicationResponse::from);
        return PageResponse.from(applications);
    }

    @Override
    public ApplicationResponse getById(Long ownerId, Long applicationId) {
        return applicationRepository.findByIdAndOwnerId(applicationId, ownerId)
                .map(ApplicationResponse::from)
                .orElseThrow(ApplicationNotFoundException::new);
    }

    private String generateUniqueProjectId() {
        for (int attempt = 0; attempt < PROJECT_ID_GENERATION_ATTEMPTS; attempt++) {
            String projectId = credentialGenerator.generateProjectId();
            if (!applicationRepository.existsByProjectId(projectId)) {
                return projectId;
            }
        }
        throw new IllegalStateException("Could not generate a unique project ID");
    }
}
