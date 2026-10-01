package com.traceusage.traceusage.application.repository;

import com.traceusage.traceusage.application.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByProjectId(String projectId);

    Page<Application> findAllByOwnerId(Long ownerId, Pageable pageable);

    Optional<Application> findByIdAndOwnerId(Long id, Long ownerId);
}
