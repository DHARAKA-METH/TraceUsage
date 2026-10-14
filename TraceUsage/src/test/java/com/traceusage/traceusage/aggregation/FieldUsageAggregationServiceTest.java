package com.traceusage.traceusage.aggregation;

import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.fieldusage.dto.AccessedFieldRequest;
import com.traceusage.traceusage.fieldusage.dto.FieldUsageBatchRequest;
import com.traceusage.traceusage.fieldusage.dto.ObservedFieldsRequest;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.lifecycle.repository.ApiEndpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FieldUsageAggregationServiceTest {

    private static final String AGGREGATE_SQL_FRAGMENT = "insert into field_usage_hourly";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ApiEndpointRepository endpointRepository;

    @Mock
    private ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistry;

    @Mock
    private Application application;

    @Mock
    private ApiEndpoint productEndpoint;

    @Mock
    private ApiEndpoint userEndpoint;

    private FieldUsageAggregationService service;

    @BeforeEach
    void setUp() {
        when(meterRegistry.getIfAvailable()).thenReturn(null);
        service = new FieldUsageAggregationService(jdbcTemplate, endpointRepository, meterRegistry);

        when(application.getId()).thenReturn(1L);
        when(productEndpoint.getId()).thenReturn(10L);
        when(userEndpoint.getId()).thenReturn(11L);
        when(endpointRepository.findByApplicationIdAndHttpMethodAndEndpointPath(1L, "GET", "/api/products/{id}"))
                .thenReturn(Optional.of(productEndpoint));
        when(endpointRepository.findByApplicationIdAndHttpMethodAndEndpointPath(1L, "GET", "/api/users/{id}"))
                .thenReturn(Optional.of(userEndpoint));
        when(endpointRepository.save(any(ApiEndpoint.class))).thenAnswer(invocation -> {
            ApiEndpoint saved = invocation.getArgument(0);
            return saved;
        });
    }

    @Test
    void shouldSkipEntireBatchWhenBatchIdWasAlreadyProcessed() {
        UUID batchId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(batchId), eq(1L))).thenReturn(0);

        assertThat(service.aggregateBatchIfNew(application, batch(batchId, List.of(
                response("/api/products/{id}", "Product", List.of("name", "price")))))).isFalse();

        assertThat(aggregateUpserts()).isEmpty();
    }

    @Test
    void shouldAggregateNestedFieldPathsSeparately() {
        UUID batchId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(batchId), eq(1L))).thenReturn(1);

        service.aggregateBatchIfNew(application, batch(batchId, List.of(
                response("/api/products/{id}", "Product",
                        List.of("supplier.name", "products[].name", "orders[].items[].price")))));

        List<Object[]> upserts = aggregateUpserts();
        assertThat(upserts).hasSize(3);
        assertThat(upserts).extracting(params -> params[5])
                .containsExactlyInAnyOrder(
                        "supplier.name",
                        "products[].name",
                        "orders[].items[].price");
    }

    @Test
    void shouldNeverMergeSameFieldNameAcrossEndpoints() {
        UUID batchId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(batchId), eq(1L))).thenReturn(1);

        service.aggregateBatchIfNew(application, batch(batchId, List.of(
                response("/api/products/{id}", "Product", List.of("name")),
                response("/api/users/{id}", "User", List.of("name")))));

        List<Object[]> upserts = aggregateUpserts();
        assertThat(upserts).hasSize(2);
        assertThat(upserts).extracting(params -> params[1]).containsExactlyInAnyOrder(10L, 11L);
        assertThat(upserts).extracting(params -> params[3])
                .containsExactlyInAnyOrder("/api/products/{id}", "/api/users/{id}");
        assertThat(upserts).extracting(params -> params[5])
                .containsOnly("name");
    }

    @Test
    void shouldKeepClientAndVersionSeparatedAndBucketByUtcHour() {
        UUID batchId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(batchId), eq(1L))).thenReturn(1);

        service.aggregateBatchIfNew(application,
                batch(batchId, List.of(
                        response("/api/products/{id}", "Product", List.of("name"))),
                        "android-app", "2.1.0", Instant.parse("2026-10-14T10:59:59Z")));

        List<Object[]> upserts = aggregateUpserts();
        assertThat(upserts).hasSize(1);
        assertThat(upserts.get(0)[6]).isEqualTo("android-app");
        assertThat(upserts.get(0)[7]).isEqualTo("2.1.0");
        assertThat(upserts.get(0)[8]).isEqualTo(Timestamp.from(Instant.parse("2026-10-14T10:00:00Z")));
    }

    @Test
    void shouldSumAccessCountsForRepeatedFieldWithinOneBatch() {
        UUID batchId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(batchId), eq(1L))).thenReturn(1);

        FieldUsageBatchRequest request = new FieldUsageBatchRequest(
                batchId,
                "proj_1",
                "product-web",
                "1.0.0",
                Instant.parse("2026-10-14T10:10:00Z"),
                List.of(
                        new ObservedFieldsRequest(
                                "GET", "/api/products/{id}", "Product", List.of("name"),
                                List.of(new AccessedFieldRequest("name", 3))),
                        new ObservedFieldsRequest(
                                "GET", "/api/products/{id}", "Product", List.of("name"),
                                List.of(new AccessedFieldRequest("name", 4)))));

        service.aggregateBatchIfNew(application, request);

        List<Object[]> upserts = aggregateUpserts();
        assertThat(upserts).hasSize(1);
        assertThat(upserts.get(0)[9]).isEqualTo(7L);
    }

    @Test
    void shouldRepresentMissingSchemaAsEmptyStringRatherThanFabricatingOne() {
        UUID batchId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(batchId), eq(1L))).thenReturn(1);

        service.aggregateBatchIfNew(application, batch(batchId, List.of(
                response("/api/products/{id}", null, List.of("name")))));

        List<Object[]> upserts = aggregateUpserts();
        assertThat(upserts).hasSize(1);
        assertThat(upserts.get(0)[4]).isEqualTo("");
    }

    @Test
    void shouldRegisterUnknownEndpointSoFieldTelemetryIsNeverDropped() {
        UUID batchId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(batchId), eq(1L))).thenReturn(1);
        when(endpointRepository.findByApplicationIdAndHttpMethodAndEndpointPath(anyLong(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        ApiEndpoint registered = mock(ApiEndpoint.class);
        when(registered.getId()).thenReturn(42L);
        when(endpointRepository.save(any(ApiEndpoint.class))).thenReturn(registered);

        service.aggregateBatchIfNew(application, batch(batchId, List.of(
                response("/api/orders", "Order", List.of("total")))));

        List<Object[]> upserts = aggregateUpserts();
        assertThat(upserts).hasSize(1);
        assertThat(upserts.get(0)[1]).isEqualTo(42L);
    }

    private ObservedFieldsRequest response(String endpoint, String schema, List<String> fieldPaths) {
        return new ObservedFieldsRequest(
                "GET",
                endpoint,
                schema,
                fieldPaths,
                fieldPaths.stream().map(path -> new AccessedFieldRequest(path, 1)).toList());
    }

    private FieldUsageBatchRequest batch(UUID batchId, List<ObservedFieldsRequest> responses) {
        return batch(batchId, responses, "product-web", "1.0.0", Instant.parse("2026-10-14T10:37:25Z"));
    }

    private FieldUsageBatchRequest batch(UUID batchId,
                                         List<ObservedFieldsRequest> responses,
                                         String clientId,
                                         String clientVersion,
                                         Instant observedAt) {
        return new FieldUsageBatchRequest(batchId, "proj_1", clientId, clientVersion, observedAt, responses);
    }

    private List<Object[]> aggregateUpserts() {
        return mockingDetails(jdbcTemplate).getInvocations().stream()
                .filter(invocation -> "update".equals(invocation.getMethod().getName()))
                .filter(invocation -> ((String) invocation.getArguments()[0])
                        .contains(AGGREGATE_SQL_FRAGMENT))
                .map(this::withoutSql)
                .toList();
    }

    private Object[] withoutSql(Invocation invocation) {
        Object[] all = invocation.getArguments();
        Object[] params = new Object[all.length - 1];
        System.arraycopy(all, 1, params, 0, params.length);
        return params;
    }
}