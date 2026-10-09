package com.traceusage.traceusage.fieldusage.repository;

import com.traceusage.traceusage.fieldusage.entity.ProcessedFieldUsageBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedFieldUsageBatchRepository extends JpaRepository<ProcessedFieldUsageBatch, Long> {

    boolean existsByBatchId(UUID batchId);
}
