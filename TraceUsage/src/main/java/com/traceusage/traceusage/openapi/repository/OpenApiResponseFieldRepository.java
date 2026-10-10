package com.traceusage.traceusage.openapi.repository;

import com.traceusage.traceusage.openapi.entity.OpenApiResponseField;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenApiResponseFieldRepository extends JpaRepository<OpenApiResponseField, Long> {

    long countBySpecId(Long specId);

    long countBySpecIdAndDeprecatedTrue(Long specId);
}
