package com.traceusage.traceusage.fieldusage.repository;

import com.traceusage.traceusage.fieldusage.entity.ObservedField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ObservedFieldRepository extends JpaRepository<ObservedField, Long> {

    @Modifying
    @Query(value = """
            insert into observed_fields (
                application_id,
                http_method,
                endpoint,
                schema_name,
                field_path,
                first_seen,
                last_seen
            ) values (
                :applicationId,
                :httpMethod,
                :endpoint,
                :schemaName,
                :fieldPath,
                :seenAt,
                :seenAt
            )
            on conflict on constraint uk_observed_field
            do update set last_seen = excluded.last_seen
            """, nativeQuery = true)
    void upsertObservedField(@Param("applicationId") Long applicationId,
                             @Param("httpMethod") String httpMethod,
                             @Param("endpoint") String endpoint,
                             @Param("schemaName") String schemaName,
                             @Param("fieldPath") String fieldPath,
                             @Param("seenAt") Instant seenAt);
}
