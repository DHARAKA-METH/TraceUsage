package com.traceusage.traceusage.telemetry.repository;

import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    boolean existsByEventId(UUID eventId);

    Optional<UsageEvent> findByEventId(UUID eventId);

    @Query("""
            select u.eventId
            from UsageEvent u
            where u.eventId in :eventIds
            """)
    Set<UUID> findExistingEventIds(@Param("eventIds") Collection<UUID> eventIds);
}
