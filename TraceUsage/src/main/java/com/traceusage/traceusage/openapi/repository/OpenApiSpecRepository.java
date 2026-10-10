package com.traceusage.traceusage.openapi.repository;

import com.traceusage.traceusage.openapi.entity.OpenApiSpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpenApiSpecRepository extends JpaRepository<OpenApiSpec, Long> {

    Optional<OpenApiSpec> findByApplicationIdAndDocumentHash(Long applicationId, String documentHash);
}
