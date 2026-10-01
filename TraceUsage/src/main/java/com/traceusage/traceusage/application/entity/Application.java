package com.traceusage.traceusage.application.entity;

import com.traceusage.traceusage.user.entity.User;
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
@Table(name = "applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true, length = 100)
    private String projectId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "environment", nullable = false, length = 30)
    private String environment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static Application create(String projectId, String name,
                                     String environment, User owner) {
        Application application = new Application();
        application.projectId = Objects.requireNonNull(projectId);
        application.name = Objects.requireNonNull(name);
        application.environment = Objects.requireNonNull(environment);
        application.owner = Objects.requireNonNull(owner);
        application.createdAt = Instant.now();
        return application;
    }
}
