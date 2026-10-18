package com.traceusage.traceusage.environment.entity;

import com.traceusage.traceusage.application.entity.Application;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "environments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Environment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "monitoring_started_at")
    private Instant monitoringStartedAt;

    @Column(name = "inactivity_threshold_days", nullable = false)
    private Integer inactivityThresholdDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Environment create(Application application, String name) {
        Environment environment = new Environment();
        environment.application = Objects.requireNonNull(application);
        environment.name = Objects.requireNonNull(name).trim().toLowerCase(Locale.ROOT);
        environment.inactivityThresholdDays = 90;
        environment.createdAt = Instant.now();
        environment.updatedAt = environment.createdAt;
        return environment;
    }

    public void recordMonitoringStart(Instant observedAt) {
        if (monitoringStartedAt == null || observedAt.isBefore(monitoringStartedAt)) monitoringStartedAt = observedAt;
        updatedAt = Instant.now();
    }
}
