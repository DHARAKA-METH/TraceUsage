package com.traceusage.traceusage.application.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApplicationCredentialGenerator {

    private static final int PROJECT_ID_BYTES = 12;
    private static final int API_KEY_BYTES = 32;
    private static final int KEY_PREFIX_LENGTH = 24;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public String generateProjectId() {
        return "proj_" + randomValue(PROJECT_ID_BYTES);
    }

    public String generateApiKey() {
        return "tru_sk_" + randomValue(API_KEY_BYTES);
    }

    public String generatePublicIngestKey() {
        return "tru_pk_" + randomValue(API_KEY_BYTES);
    }

    public String extractPrefix(String rawApiKey) {
        return rawApiKey.substring(0, Math.min(rawApiKey.length(), KEY_PREFIX_LENGTH));
    }

    private String randomValue(int numberOfBytes) {
        byte[] bytes = new byte[numberOfBytes];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}
