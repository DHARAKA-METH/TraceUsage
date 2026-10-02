package com.traceusage.traceusage.telemetry.repository;

import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    boolean existsByEventId(UUID eventId);

    Optional<UsageEvent> findByEventId(UUID eventId);
}
