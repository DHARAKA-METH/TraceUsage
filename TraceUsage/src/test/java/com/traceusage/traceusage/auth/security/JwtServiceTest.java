package com.traceusage.traceusage.auth.security;

import com.traceusage.traceusage.auth.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(Base64.getEncoder().encodeToString(
                "test-key-with-at-least-thirty-two-bytes".getBytes(StandardCharsets.UTF_8)));
        properties.setAccessTokenExpiration(Duration.ofMinutes(15));
        jwtService = new JwtService(properties);
    }

    @Test
    void generateAccessToken_withValidUser_shouldCreateVerifiableToken() {
        AuthenticatedUser user = new AuthenticatedUser(
                7L, "Dharaka", "dharaka@example.com", "password-hash");

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.validateAccessTokenAndGetSubject(token))
                .isEqualTo("dharaka@example.com");
    }

    @Test
    void validateAccessToken_withTamperedToken_shouldRejectToken() {
        AuthenticatedUser user = new AuthenticatedUser(
                7L, "Dharaka", "dharaka@example.com", "password-hash");
        String token = jwtService.generateAccessToken(user);
        String[] parts = token.split("\\.");
        char replacement = parts[2].charAt(0) == 'a' ? 'b' : 'a';
        String tamperedToken = parts[0] + "." + parts[1] + "."
                + replacement + parts[2].substring(1);

        assertThatThrownBy(() -> jwtService.validateAccessTokenAndGetSubject(tamperedToken))
                .isInstanceOf(JwtException.class);
    }
}
