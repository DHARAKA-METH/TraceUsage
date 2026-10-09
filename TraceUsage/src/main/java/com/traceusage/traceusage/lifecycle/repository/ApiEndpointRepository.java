package com.traceusage.traceusage.lifecycle.repository;

import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long> {

    List<ApiEndpoint> findAllByApplicationIdOrderByEndpointPathAscHttpMethodAsc(Long applicationId);

    Optional<ApiEndpoint> findByIdAndApplicationId(Long id, Long applicationId);

    Optional<ApiEndpoint> findByApplicationIdAndHttpMethodAndEndpointPath(
            Long applicationId,
            String httpMethod,
            String endpointPath);
}