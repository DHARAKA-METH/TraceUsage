package com.traceusage.traceusage.apikey.service;

import com.traceusage.traceusage.apikey.exception.InvalidApiKeyException;
import com.traceusage.traceusage.apikey.repository.ApiKeyRepository;
import com.traceusage.traceusage.application.entity.Application;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ApiKeyAuthenticationService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyHashService apiKeyHashService;

    public ApiKeyAuthenticationService(ApiKeyRepository apiKeyRepository,
                                       ApiKeyHashService apiKeyHashService) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyHashService = apiKeyHashService;
    }

    public Application authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new InvalidApiKeyException("Invalid API key");
        }

        String keyHash = apiKeyHashService.hash(rawApiKey.trim());
        return apiKeyRepository.findByKeyHashAndRevokedAtIsNull(keyHash)
                .map(apiKey -> apiKey.getApplication())
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API key"));
    }
}
