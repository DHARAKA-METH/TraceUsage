package com.traceusage.traceusage.lifecycle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "endpoint_deprecations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EndpointDeprecation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endpoint_id", nullable = false, unique = true)
    private ApiEndpoint endpoint;

    @Column(name = "deprecated_at", nullable = false)
    private Instant deprecatedAt;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "replacement_endpoint", length = 500)
    private String replacementEndpoint;

    @Column(name = "target_removal_date")
    private LocalDate targetRemovalDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static EndpointDeprecation create(ApiEndpoint endpoint,
                                            String reason,
                                            String replacementEndpoint,
                                            LocalDate targetRemovalDate) {
        EndpointDeprecation deprecation = new EndpointDeprecation();
        deprecation.endpoint = Objects.requireNonNull(endpoint);
        deprecation.reason = reason;
        deprecation.replacementEndpoint = replacementEndpoint;
        deprecation.targetRemovalDate = targetRemovalDate;
        deprecation.deprecatedAt = Instant.now();
        deprecation.createdAt = Instant.now();
        return deprecation;
    }

    public void update(String reason, String replacementEndpoint, LocalDate targetRemovalDate) {
        this.reason = reason;
        this.replacementEndpoint = replacementEndpoint;
        this.targetRemovalDate = targetRemovalDate;
    }
}