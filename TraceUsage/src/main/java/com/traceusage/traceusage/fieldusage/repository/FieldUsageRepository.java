package com.traceusage.traceusage.fieldusage.repository;

import com.traceusage.traceusage.fieldusage.entity.FieldUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldUsageRepository extends JpaRepository<FieldUsageEvent, Long> {
}
