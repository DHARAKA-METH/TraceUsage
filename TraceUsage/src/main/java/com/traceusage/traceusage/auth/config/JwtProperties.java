package com.traceusage.traceusage.auth.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Base64;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotBlank
    private String secret;

    @NotNull
    private Duration accessTokenExpiration = Duration.ofMinutes(15);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(Duration accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    @AssertTrue(message = "must decode to at least 256 bits")
    public boolean isSecretStrongEnough() {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        try {
            return Base64.getDecoder().decode(secret).length >= 32;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @AssertTrue(message = "must be positive")
    public boolean isAccessTokenExpirationPositive() {
        return accessTokenExpiration == null
                || (!accessTokenExpiration.isZero() && !accessTokenExpiration.isNegative());
    }
}
