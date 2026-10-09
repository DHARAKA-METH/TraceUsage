package com.traceusage.traceusage.lifecycle.repository;

import com.traceusage.traceusage.lifecycle.entity.EndpointDeprecation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EndpointDeprecationRepository extends JpaRepository<EndpointDeprecation, Long> {

    Optional<EndpointDeprecation> findByEndpointId(Long endpointId);

    boolean existsByEndpointId(Long endpointId);
}