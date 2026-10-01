package com.traceusage.traceusage.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationCredentialGeneratorTest {

    private final ApplicationCredentialGenerator generator =
            new ApplicationCredentialGenerator();

    @Test
    void generateProjectId_shouldCreateUniquePrefixedIdentifiers() {
        String first = generator.generateProjectId();
        String second = generator.generateProjectId();

        assertThat(first).startsWith("proj_");
        assertThat(second).startsWith("proj_");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void generateApiKey_shouldCreateStrongPrefixedCredential() {
        String apiKey = generator.generateApiKey();

        assertThat(apiKey).startsWith("tru_sk_");
        assertThat(apiKey).hasSizeGreaterThan(40);
        assertThat(generator.extractPrefix(apiKey)).hasSizeLessThanOrEqualTo(30);
    }
}
