package com.traceusage.traceusage.telemetry.repository;

import com.traceusage.traceusage.analytics.dto.EndpointAnalyticsItem;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
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

    @Query("""
            select new com.traceusage.traceusage.analytics.dto.EndpointAnalyticsItem(
                u.httpMethod,
                u.endpoint,
                count(u),
                coalesce(sum(case when u.statusCode between 200 and 399 then 1L else 0L end), 0L),
                coalesce(sum(case when u.statusCode >= 400 then 1L else 0L end), 0L),
                max(u.occurredAt)
            )
            from UsageEvent u
            where u.application.id = :applicationId
              and (:from is null or u.occurredAt >= :from)
              and (:to is null or u.occurredAt <= :to)
            group by u.httpMethod, u.endpoint
            order by count(u) desc, max(u.occurredAt) desc
            """)
    List<EndpointAnalyticsItem> findEndpointAnalytics(
            @Param("applicationId") Long applicationId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
