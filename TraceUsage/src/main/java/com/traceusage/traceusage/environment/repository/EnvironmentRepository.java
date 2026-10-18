package com.traceusage.traceusage.environment.repository;

import com.traceusage.traceusage.environment.entity.Environment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {
    Optional<Environment> findByApplicationIdAndName(Long applicationId, String name);
}
