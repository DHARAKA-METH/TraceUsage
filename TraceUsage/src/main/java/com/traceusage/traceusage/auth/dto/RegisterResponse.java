package com.traceusage.traceusage.auth.dto;

import com.traceusage.traceusage.user.entity.User;

import java.time.Instant;

public record RegisterResponse(
        Long id,
        String name,
        String email,
        Instant createdAt,
        String accessToken) {

    public static RegisterResponse from(User user, String accessToken) {
        return new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                accessToken);
    }
}
