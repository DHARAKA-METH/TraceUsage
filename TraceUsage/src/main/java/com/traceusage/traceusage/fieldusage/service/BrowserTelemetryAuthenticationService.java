package com.traceusage.traceusage.fieldusage.service;

import com.traceusage.traceusage.apikey.entity.ApiKey;
import com.traceusage.traceusage.apikey.exception.InvalidApiKeyException;
import com.traceusage.traceusage.apikey.repository.ApiKeyRepository;
import com.traceusage.traceusage.apikey.service.ApiKeyHashService;
import com.traceusage.traceusage.application.entity.Application;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
        if (rawPublicKey == null || rawPublicKey.isBlank()) {
            System.out.println("REJECTED -> X-TraceUsage-Public-Key header is missing");
            throw new InvalidApiKeyException("Invalid public ingest key");
        }

        if (!rawPublicKey.startsWith("tru_pk_")) {
            System.out.println("REJECTED -> key prefix is not tru_pk_");
            throw new InvalidApiKeyException("Invalid public ingest key");
        }

        String keyHash = apiKeyHashService.hash(rawPublicKey.trim());
        Optional<ApiKey> matchedKey =
                apiKeyRepository.findByKeyHashAndKeyTypeAndRevokedAtIsNull(keyHash, PUBLIC_BROWSER_KEY_TYPE);

        if (matchedKey.isEmpty()) {
            System.out.println("REJECTED -> no active PUBLIC_BROWSER key matched");
            throw new InvalidApiKeyException("Invalid public ingest key");
        }

        Application application = matchedKey.get().getApplication();
        System.out.println("AUTH OK -> applicationId=" + application.getId());
        return application;
    }
}
