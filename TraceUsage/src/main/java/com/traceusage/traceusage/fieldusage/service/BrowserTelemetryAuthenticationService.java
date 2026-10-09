package com.traceusage.traceusage.fieldusage.service;

import com.traceusage.traceusage.apikey.exception.InvalidApiKeyException;
import com.traceusage.traceusage.apikey.repository.ApiKeyRepository;
import com.traceusage.traceusage.apikey.service.ApiKeyHashService;
import com.traceusage.traceusage.application.entity.Application;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BrowserTelemetryAuthenticationService {

    private static final String PUBLIC_BROWSER_KEY_TYPE = "PUBLIC_BROWSER";

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyHashService apiKeyHashService;

    public BrowserTelemetryAuthenticationService(ApiKeyRepository apiKeyRepository,
                                                 ApiKeyHashService apiKeyHashService) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyHashService = apiKeyHashService;
    }

    public Application authenticate(String rawPublicKey) {
        if (rawPublicKey == null || rawPublicKey.isBlank() || !rawPublicKey.startsWith("tru_pk_")) {
            throw new InvalidApiKeyException("Invalid public ingest key");
        }

        String keyHash = apiKeyHashService.hash(rawPublicKey.trim());
        return apiKeyRepository.findByKeyHashAndKeyTypeAndRevokedAtIsNull(keyHash, PUBLIC_BROWSER_KEY_TYPE)
                .map(apiKey -> apiKey.getApplication())
                .orElseThrow(() -> new InvalidApiKeyException("Invalid public ingest key"));
    }
}
