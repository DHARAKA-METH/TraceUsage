package com.traceusage.traceusage.lifecycle.entity;

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
@Table(name = "api_endpoints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "endpoint_path", nullable = false, length = 500)
    private String endpointPath;

    @Column(name = "first_discovered_at", nullable = false)
    private Instant firstDiscoveredAt;

    @Column(name = "first_seen_at")
    private Instant firstSeenAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "request_count", nullable = false)
    private Long requestCount;

    protected ApiEndpoint(Application application, String httpMethod, String endpointPath) {
        this.application = Objects.requireNonNull(application);
        this.httpMethod = Objects.requireNonNull(httpMethod);
        this.endpointPath = Objects.requireNonNull(endpointPath);
        this.firstDiscoveredAt = Instant.now();
        this.requestCount = 0L;
    }

    public static ApiEndpoint register(Application application, String httpMethod, String endpointPath) {
        return new ApiEndpoint(application, httpMethod, endpointPath);
    }

    public void recordUsage(Instant occurredAt, boolean counted) {
        if (counted) {
            this.requestCount = this.requestCount + 1;
        }

        if (occurredAt == null) {
            return;
        }

        if (this.lastSeenAt == null || occurredAt.isAfter(this.lastSeenAt)) {
            this.lastSeenAt = occurredAt;
        }

        if (this.firstSeenAt == null || occurredAt.isBefore(this.firstSeenAt)) {
            this.firstSeenAt = occurredAt;
        }
    }
}