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
@Table(name = "processed_field_usage_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedFieldUsageBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true)
    private UUID batchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public static ProcessedFieldUsageBatch create(UUID batchId, Application application) {
        ProcessedFieldUsageBatch batch = new ProcessedFieldUsageBatch();
        batch.batchId = Objects.requireNonNull(batchId);
        batch.application = Objects.requireNonNull(application);
        batch.processedAt = Instant.now();
        return batch;
    }
}
