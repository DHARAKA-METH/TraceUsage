package com.traceusage.traceusage.fieldusage.entity;

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
@Table(name = "field_usage_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FieldUsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "schema_name", length = 150)
    private String schemaName;

    @Column(name = "field_path", nullable = false, length = 500)
    private String fieldPath;

    @Column(name = "client_id", nullable = false, length = 150)
    private String clientId;

    @Column(name = "client_version", nullable = false, length = 80)
    private String clientVersion;

    @Column(name = "access_count", nullable = false)
    private Integer accessCount;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static FieldUsageEvent create(Application application,
                                         UUID batchId,
                                         String httpMethod,
                                         String endpoint,
                                         String schemaName,
                                         String fieldPath,
                                         String clientId,
                                         String clientVersion,
                                         Integer accessCount,
                                         Instant observedAt) {
        FieldUsageEvent event = new FieldUsageEvent();
        event.application = Objects.requireNonNull(application);
        event.batchId = Objects.requireNonNull(batchId);
        event.httpMethod = Objects.requireNonNull(httpMethod);
        event.endpoint = Objects.requireNonNull(endpoint);
        event.schemaName = schemaName;
        event.fieldPath = Objects.requireNonNull(fieldPath);
        event.clientId = Objects.requireNonNull(clientId);
        event.clientVersion = Objects.requireNonNull(clientVersion);
        event.accessCount = Objects.requireNonNull(accessCount);
        event.observedAt = Objects.requireNonNull(observedAt);
        event.createdAt = Instant.now();
        return event;
    }
}
