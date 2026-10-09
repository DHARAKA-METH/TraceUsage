package com.traceusage.traceusage.fieldusage.repository;

import com.traceusage.traceusage.fieldusage.entity.FieldUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FieldUsageRepository extends JpaRepository<FieldUsageEvent, Long> {

    @Query("""
            select e from FieldUsageEvent e
            where e.application.id = :applicationId
              and e.httpMethod = :httpMethod
              and e.endpoint = :endpoint
            """)
    List<FieldUsageEvent> findAllByEndpoint(
            @Param("applicationId") Long applicationId,
            @Param("httpMethod") String httpMethod,
            @Param("endpoint") String endpoint);
}