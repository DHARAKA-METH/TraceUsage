package com.traceusage.traceusage.apikey.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyHashServiceTest {

    private final ApiKeyHashService apiKeyHashService = new ApiKeyHashService();

    @Test
    void hash_withSameApiKey_shouldProduceStableSha256HexDigest() {
        String rawApiKey = "tru_sk_example-api-key";

        String hash = apiKeyHashService.hash(rawApiKey);

        assertThat(hash).isEqualTo(apiKeyHashService.hash(rawApiKey));
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void hash_withDifferentApiKeys_shouldProduceDifferentDigests() {
        assertThat(apiKeyHashService.hash("tru_sk_first"))
                .isNotEqualTo(apiKeyHashService.hash("tru_sk_second"));
    }
}
