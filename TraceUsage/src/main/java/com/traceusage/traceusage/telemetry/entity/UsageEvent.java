package com.traceusage.traceusage.telemetry.entity;

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
import java.util.UUID;

@Entity
@Table(name = "usage_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static UsageEvent create(UUID eventId,
                                    Application application,
                                    String httpMethod,
                                    String endpoint,
                                    Integer statusCode,
                                    Instant occurredAt) {
        UsageEvent usageEvent = new UsageEvent();
        usageEvent.eventId = Objects.requireNonNull(eventId);
        usageEvent.application = Objects.requireNonNull(application);
        usageEvent.httpMethod = Objects.requireNonNull(httpMethod);
        usageEvent.endpoint = Objects.requireNonNull(endpoint);
        usageEvent.statusCode = Objects.requireNonNull(statusCode);
        usageEvent.occurredAt = Objects.requireNonNull(occurredAt);
        usageEvent.createdAt = Instant.now();
        return usageEvent;
    }
}
