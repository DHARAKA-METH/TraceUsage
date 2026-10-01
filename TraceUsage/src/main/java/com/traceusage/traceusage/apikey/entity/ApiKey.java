package com.traceusage.traceusage.apikey.entity;

import com.traceusage.traceusage.application.entity.Application;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "api_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "key_prefix", nullable = false, length = 30)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true, length = 255)
    private String keyHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static ApiKey create(Application application, String keyPrefix, String keyHash) {
        ApiKey apiKey = new ApiKey();
        apiKey.application = Objects.requireNonNull(application);
        apiKey.keyPrefix = Objects.requireNonNull(keyPrefix);
        apiKey.keyHash = Objects.requireNonNull(keyHash);
        apiKey.createdAt = Instant.now();
        return apiKey;
    }
}
