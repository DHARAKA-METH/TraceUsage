package com.traceusage.traceusage.aggregation;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EndpointUsageAggregationServiceTest {

    private static final String AGGREGATE_SQL_FRAGMENT = "insert into endpoint_usage_hourly";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistry;

    @Mock
    private Application application;

    @Mock
    private ApiEndpoint endpoint;

    private EndpointUsageAggregationService service;
    private final List<UUID> submittedEventIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        submittedEventIds.clear();
        when(meterRegistry.getIfAvailable()).thenReturn(null);
        service = new EndpointUsageAggregationService(jdbcTemplate, meterRegistry);
        when(application.getId()).thenReturn(1L);
        when(endpoint.getId()).thenReturn(10L);
    }

    @Test
    void shouldUpsertIntoUtcHourBucketAndSkipDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-10-14T10:37:25Z");
        submit(eventId, 10L, 200, occurredAt);

        claimOnly(1);
        assertThat(service.aggregateIfNew(endpoint, event(eventId, 200, occurredAt))).isTrue();

        List<Object[]> upserts = aggregateUpserts();
        assertThat(upserts).hasSize(1);
        Object[] row = upserts.get(0);
        assertThat(row[0]).isEqualTo(10L);
        assertThat(row[1]).isEqualTo(Timestamp.from(Instant.parse("2026-10-14T10:00:00Z")));
        assertThat(row[2]).isEqualTo(1L);
        assertThat(row[3]).isEqualTo(1L);
        assertThat(row[4]).isEqualTo(0L);
        assertThat(row[5]).isEqualTo(Timestamp.from(occurredAt));
        assertThat(row[6]).isEqualTo(Timestamp.from(occurredAt));

        // Kafka redelivery of the same record must not increment counts again.
        claimOnly(0);
        assertThat(service.aggregateIfNew(endpoint, event(eventId, 200, occurredAt))).isFalse();
        assertThat(aggregateUpserts()).hasSize(1);
    }

    @Test
    void shouldCollapseSameHourEventsIntoASingleAggregateRowPerBatch() {
        List<EndpointUsageAggregationService.PendingEndpointEvent> pending = List.of(
                submit(10L, 200, Instant.parse("2026-10-14T10:00:05Z")),
                submit(10L, 200, Instant.parse("2026-10-14T10:37:25Z")),
                submit(10L, 500, Instant.parse("2026-10-14T10:55:10Z")),
                submit(10L, 204, Instant.parse("2026-10-14T10:59:59Z")));

        claimAll();

        assertThat(service.aggregateAllIfNew(pending)).isEqualTo(4);

        List<Object[]> upserts = aggregateUpserts();
        assertThat(upserts).hasSize(1);
        Object[] row = upserts.get(0);
        assertThat(row[0]).isEqualTo(10L);
        assertThat(row[2]).isEqualTo(4L);
        assertThat(row[3]).isEqualTo(3L);
        assertThat(row[4]).isEqualTo(1L);
        assertThat(row[5]).isEqualTo(Timestamp.from(Instant.parse("2026-10-14T10:00:05Z")));
        assertThat(row[6]).isEqualTo(Timestamp.from(Instant.parse("2026-10-14T10:59:59Z")));
    }

    @Test
    void shouldSplitAggregateRowsByEndpointAndHour() {
        List<EndpointUsageAggregationService.PendingEndpointEvent> pending = List.of(
                submit(10L, 200, Instant.parse("2026-10-14T10:05:00Z")),
                submit(10L, 200, Instant.parse("2026-10-14T11:05:00Z")),
                submit(20L, 200, Instant.parse("2026-10-14T10:05:00Z")),
                submit(30L, 200, Instant.parse("2026-10-14T10:05:00Z")));

        claimAll();

        service.aggregateAllIfNew(pending);

        List<Object[]> upserts = aggregateUpserts();
        assertThat(upserts).hasSize(4);
        assertThat(upserts).extracting(row -> row[0]).containsExactlyInAnyOrder(10L, 10L, 20L, 30L);
        assertThat(upserts).extracting(row -> row[1]).containsOnly(
                Timestamp.from(Instant.parse("2026-10-14T10:00:00Z")),
                Timestamp.from(Instant.parse("2026-10-14T11:00:00Z")));
    }

    @Test
    void shouldCountOnlyNewlyClaimedEventsWhenPartOfTheBatchIsDuplicate() {
        List<EndpointUsageAggregationService.PendingEndpointEvent> pending = List.of(
                submit(10L, 200, Instant.parse("2026-10-14T10:05:00Z")),
                submit(10L, 200, Instant.parse("2026-10-14T10:06:00Z")),
                submit(10L, 404, Instant.parse("2026-10-14T10:07:00Z")));

        claim(0, 2);

        assertThat(service.aggregateAllIfNew(pending)).isEqualTo(2);

        Object[] row = aggregateUpserts().get(0);
        assertThat(row[2]).isEqualTo(2L);
        assertThat(row[3]).isEqualTo(1L);
        assertThat(row[4]).isEqualTo(1L);
    }

    @Test
    void shouldClassifySuccessAndErrorUsingExistingStatusRules() {
        List<EndpointUsageAggregationService.PendingEndpointEvent> pending = List.of(
                submit(10L, 200, Instant.parse("2026-10-14T10:37:25Z")),
                submit(10L, 204, Instant.parse("2026-10-14T10:37:25Z")),
                submit(10L, 400, Instant.parse("2026-10-14T10:37:25Z")),
                submit(10L, 500, Instant.parse("2026-10-14T10:37:25Z")));

        claimAll();

        service.aggregateAllIfNew(pending);

        Object[] row = aggregateUpserts().get(0);
        assertThat(row[3]).isEqualTo(2L);
        assertThat(row[4]).isEqualTo(2L);
    }

    @Test
    void shouldNotWriteAggregatesWhenEveryEventIsDuplicate() {
        List<EndpointUsageAggregationService.PendingEndpointEvent> pending = List.of(
                submit(10L, 200, Instant.parse("2026-10-14T10:05:00Z")),
                submit(10L, 200, Instant.parse("2026-10-14T10:06:00Z")));

        claimNone();

        assertThat(service.aggregateAllIfNew(pending)).isZero();
        assertThat(aggregateUpserts()).isEmpty();
    }

    @Test
    void shouldDoNothingForAnEmptyBatch() {
        assertThat(service.aggregateAllIfNew(List.of())).isZero();
        assertThat(sqlStatements()).isEmpty();
    }

    private void submit(UUID eventId, long endpointId, int statusCode, Instant occurredAt) {
        submittedEventIds.add(eventId);
    }

    private EndpointUsageAggregationService.PendingEndpointEvent submit(
            long endpointId, int statusCode, Instant occurredAt) {
        UUID eventId = UUID.randomUUID();
        submittedEventIds.add(eventId);
        return new EndpointUsageAggregationService.PendingEndpointEvent(
                endpointWithId(endpointId), usageEvent(eventId, statusCode, occurredAt));
    }

    private void claimAll() {
        claimAllIndices(submittedEventIds.size());
    }

    private void claimOnly(int count) {
        claimAllIndices(count);
    }

    private void claim(int... acceptedIndices) {
        List<UUID> accepted = new ArrayList<>();
        for (int index : acceptedIndices) {
            accepted.add(submittedEventIds.get(index));
        }
        stubClaimResponse(accepted);
    }

    private void claimNone() {
        stubClaimResponse(List.of());
    }

    private void claimAllIndices(int count) {
        stubClaimResponse(new ArrayList<>(submittedEventIds.subList(0, count)));
    }

    private void stubClaimResponse(List<UUID> accepted) {
        when(jdbcTemplate.query(
                anyString(),
                any(PreparedStatementSetter.class),
                any(RowMapper.class)))
                .thenAnswer(invocation -> accepted);
    }

    private ApiEndpoint endpointWithId(long id) {
        ApiEndpoint other = org.mockito.Mockito.mock(ApiEndpoint.class);
        when(other.getId()).thenReturn(id);
        return other;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> aggregateUpserts() {
        return mockingDetails(jdbcTemplate).getInvocations().stream()
                .filter(invocation -> "batchUpdate".equals(invocation.getMethod().getName()))
                .filter(invocation -> ((String) invocation.getArguments()[0]).contains(AGGREGATE_SQL_FRAGMENT))
                .map(invocation -> (List<Object[]>) invocation.getArguments()[1])
                .flatMap(List::stream)
                .toList();
    }

    private List<String> sqlStatements() {
        return mockingDetails(jdbcTemplate).getInvocations().stream()
                .map(invocation -> (String) invocation.getArguments()[0])
                .toList();
    }

    private UsageEvent usageEvent(UUID eventId, int statusCode, Instant occurredAt) {
        UsageEvent event = org.mockito.Mockito.mock(UsageEvent.class);
        when(event.getEventId()).thenReturn(eventId);
        when(event.getStatusCode()).thenReturn(statusCode);
        when(event.getOccurredAt()).thenReturn(occurredAt);
        return event;
    }

    private UsageEvent event(UUID eventId, int statusCode, Instant occurredAt) {
        return UsageEvent.create(eventId, application, "GET", "/api/products", statusCode, occurredAt);
    }
}