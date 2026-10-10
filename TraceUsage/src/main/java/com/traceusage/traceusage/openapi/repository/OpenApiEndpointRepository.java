package com.traceusage.traceusage.openapi.repository;

import com.traceusage.traceusage.openapi.entity.OpenApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpenApiEndpointRepository extends JpaRepository<OpenApiEndpoint, Long> {

    Optional<OpenApiEndpoint> findBySpecIdAndHttpMethodAndEndpointPath(
            Long specId,
            String httpMethod,
            String endpointPath);

    long countBySpecId(Long specId);

    long countBySpecIdAndDeprecatedTrue(Long specId);
}
