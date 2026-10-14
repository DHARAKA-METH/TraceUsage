package com.traceusage.traceusage.aggregation;

import com.traceusage.traceusage.analytics.service.AnalyticsServiceImpl;
import com.traceusage.traceusage.application.entity.Application;
import com.traceusage.traceusage.application.repository.ApplicationRepository;
import com.traceusage.traceusage.lifecycle.service.KnownClientAnalyzer;
import com.traceusage.traceusage.fieldusage.dto.AccessedFieldRequest;
import com.traceusage.traceusage.fieldusage.dto.FieldUsageBatchRequest;
import com.traceusage.traceusage.fieldusage.dto.ObservedFieldsRequest;
import com.traceusage.traceusage.lifecycle.entity.ApiEndpoint;
import com.traceusage.traceusage.lifecycle.repository.ApiEndpointRepository;
import com.traceusage.traceusage.telemetry.entity.UsageEvent;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Real PostgreSQL verification of the hourly aggregation behaviour.
 *
 * <p>Runs the production Flyway migrations (including V10) against a throwaway
 * PostgreSQL database and executes the production aggregation services against
 * it. By default a Testcontainers PostgreSQL instance is used; when Docker is
 * not available the test is skipped. CI can point the test at any disposable
 * PostgreSQL instance instead with
 * {@code -Dtraceusage.it.jdbc-url=jdbc:postgresql://...}.
 */
class HourlyAggregationPostgresTest {

    private static final String JDBC_URL_PROPERTY = "traceusage.it.jdbc-url";
    private static final String JDBC_USERNAME_PROPERTY = "traceusage.it.jdbc-username";
    private static final String JDBC_PASSWORD_PROPERTY = "traceusage.it.jdbc-password";

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("traceusage")
                    .withUsername("traceusage")
                    .withPassword("traceusage");

    private static String jdbcUrl;
    private static String jdbcUsername;
    private static String jdbcPassword;

    private static io.zonky.test.db.postgres.embedded.EmbeddedPostgres embeddedPostgres;

    private static JdbcTemplate jdbcTemplate;
    private static EndpointUsageAggregationService endpointService;
    private static FieldUsageAggregationService fieldService;

    @BeforeAll
    static void migrateSchema() {
        resolveDatabase();

        Flyway.configure()
                .dataSource(jdbcUrl, jdbcUsername, jdbcPassword)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(jdbcUrl, jdbcUsername, jdbcPassword));

        ObjectProvider<io.micrometer.core.instrument.MeterRegistry> noMetrics =
                new ObjectProvider<>() {
                    @Override
                    public io.micrometer.core.instrument.MeterRegistry getObject() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public io.micrometer.core.instrument.MeterRegistry getIfAvailable() {
                        return null;
                    }

                    @Override
                    public io.micrometer.core.instrument.MeterRegistry getIfUnique() {
                        return null;
                    }
                };

        endpointService = new EndpointUsageAggregationService(jdbcTemplate, noMetrics);
        fieldService = new FieldUsageAggregationService(
                jdbcTemplate, staticApiEndpointRepository(), noMetrics);
    }

    private static ApiEndpointRepository staticApiEndpointRepository() {
        ApiEndpointRepository repository = mock(ApiEndpointRepository.class);
        when(repository.findByApplicationIdAndHttpMethodAndEndpointPath(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    Long applicationId = invocation.getArgument(0);
                    String method = invocation.getArgument(1);
                    String path = invocation.getArgument(2);
                    Long id = jdbcTemplate.queryForObject("""
                            insert into api_endpoints (application_id, http_method, endpoint_path)
                            values (?, ?, ?)
                            on conflict (application_id, http_method, endpoint_path)
                            do update set endpoint_path = excluded.endpoint_path
                            returning id
                            """, Long.class, applicationId, method, path);
                    return Optional.of(endpointWithId(id));
                });
        return repository;
    }

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("delete from processed_endpoint_aggregation_events");
        jdbcTemplate.update("delete from processed_field_aggregation_batches");
        jdbcTemplate.update("delete from endpoint_usage_hourly");
        jdbcTemplate.update("delete from field_usage_hourly");
        jdbcTemplate.update("delete from usage_events");
        jdbcTemplate.update("delete from field_usage_events");
        jdbcTemplate.update("delete from observed_fields");
        jdbcTemplate.update("delete from api_endpoints");
        jdbcTemplate.update("delete from applications");
        jdbcTemplate.update("delete from users");
    }

    @Test
    @DisplayName("1000 requests to the same endpoint in one hour produce one aggregate row")
    void shouldCollapseThousandRequestsIntoSingleHourlyRow() {
        long applicationId = seedApplication("proj_perf", "performance", "development");
        ApiEndpoint endpoint = seedEndpoint(applicationId, "GET", "/api/products");
        Instant hour = Instant.parse("2026-10-14T10:00:00Z");

        for (int i = 0; i < 1_000; i++) {
            Instant occurredAt = hour.plusSeconds(i % 3600);
            endpointService.aggregateIfNew(
                    endpoint, usageEvent(applicationId, 200, occurredAt));
        }

        List<AggregateRow> rows = jdbcTemplate.query("""
                select hour_bucket, request_count, success_count, error_count, first_seen, last_seen
                from endpoint_usage_hourly
                where endpoint_id = ?
                """, (rs, rowNum) -> new AggregateRow(
                        rs.getTimestamp("hour_bucket").toInstant(),
                        rs.getLong("request_count"),
                        rs.getLong("success_count"),
                        rs.getLong("error_count"),
                        rs.getTimestamp("first_seen").toInstant(),
                        rs.getTimestamp("last_seen").toInstant()),
                endpoint.getId());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).hourBucket()).isEqualTo(hour);
        assertThat(rows.get(0).requestCount()).isEqualTo(1_000L);
        assertThat(rows.get(0).successCount()).isEqualTo(1_000L);
        assertThat(rows.get(0).errorCount()).isZero();
        assertThat(rows.get(0).firstSeen()).isEqualTo(hour);
        assertThat(rows.get(0).lastSeen()).isEqualTo(hour.plusSeconds(999));
    }

    @Test
    @DisplayName("1000 events plus 100 duplicate deliveries still report 1000 requests")
    void shouldNotInflateCountsOnDuplicateDelivery() {
        long applicationId = seedApplication("proj_dup", "dup", "development");
        ApiEndpoint endpoint = seedEndpoint(applicationId, "GET", "/api/products");
        Instant hour = Instant.parse("2026-10-14T10:00:00Z");

        List<UsageEvent> events = new java.util.ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            UsageEvent event = usageEvent(applicationId, 200, hour.plusSeconds(i % 3600));
            events.add(event);
            endpointService.aggregateIfNew(endpoint, event);
        }

        // Simulate a consumer crash before offset commit followed by Kafka replay.
        for (int i = 0; i < 100; i++) {
            assertThat(endpointService.aggregateIfNew(endpoint, events.get(i))).isFalse();
        }

        Long requestCount = jdbcTemplate.queryForObject(
                "select sum(request_count) from endpoint_usage_hourly where endpoint_id = ?",
                Long.class, endpoint.getId());

        assertThat(requestCount).isEqualTo(1_000L);
    }

    @Test
    @DisplayName("Processing the same Kafka batch twice leaves counts unchanged")
    void shouldBeIdempotentWhenWholeBatchIsReprocessed() {
        long applicationId = seedApplication("proj_replay", "replay", "development");
        ApiEndpoint endpoint = seedEndpoint(applicationId, "GET", "/api/products/{id}");
        Application application = applicationWithId(applicationId);
        FieldUsageBatchRequest batch = fieldBatch(
                UUID.randomUUID(), "/api/products/{id}", "Product", List.of("name", "price"),
                "product-web", "1.0.0", Instant.parse("2026-10-14T10:15:00Z"));

        assertThat(fieldService.aggregateBatchIfNew(application, batch)).isTrue();
        assertThat(fieldService.aggregateBatchIfNew(application, batch)).isFalse();

        List<Long> counts = jdbcTemplate.queryForList(
                "select access_count from field_usage_hourly where application_id = ?",
                Long.class, applicationId);

        assertThat(counts).containsExactly(1L, 1L);
        assertThat(jdbcTemplate.queryForObject(
                "select coalesce(sum(access_count), 0) from field_usage_hourly", Long.class))
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("Events spread across two hours produce two rows with correct per-hour counts")
    void shouldSplitCountsAcrossHourBuckets() {
        long applicationId = seedApplication("proj_hours", "hours", "development");
        ApiEndpoint endpoint = seedEndpoint(applicationId, "GET", "/api/products");
        Instant ten = Instant.parse("2026-10-14T10:00:00Z");
        Instant eleven = Instant.parse("2026-10-14T11:00:00Z");

        for (int i = 0; i < 400; i++) {
            endpointService.aggregateIfNew(endpoint, usageEvent(applicationId, 200, ten.plusSeconds(i)));
        }
        for (int i = 0; i < 600; i++) {
            endpointService.aggregateIfNew(endpoint, usageEvent(applicationId, 200, eleven.plusSeconds(i)));
        }

        assertThat(jdbcTemplate.queryForList(
                "select request_count from endpoint_usage_hourly order by hour_bucket", Long.class))
                .containsExactly(400L, 600L);
    }

    @Test
    @DisplayName("Different endpoints, methods and tenants are never merged")
    void shouldKeepEndpointsMethodsAndTenantsIsolated() {
        long devApp = seedApplication("proj_dev", "dev", "development");
        long prodApp = seedApplication("proj_prod", "prod", "production");
        ApiEndpoint getProducts = seedEndpoint(devApp, "GET", "/api/products");
        ApiEndpoint postProducts = seedEndpoint(devApp, "POST", "/api/products");
        ApiEndpoint prodProducts = seedEndpoint(prodApp, "GET", "/api/products");

        Instant at = Instant.parse("2026-10-14T10:30:00Z");
        endpointService.aggregateIfNew(getProducts, usageEvent(devApp, 200, at));
        endpointService.aggregateIfNew(postProducts, usageEvent(devApp, 201, at));
        endpointService.aggregateIfNew(prodProducts, usageEvent(prodApp, 500, at));

        assertThat(countsByEndpoint()).containsExactlyInAnyOrder(1L, 1L, 1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from endpoint_usage_hourly", Long.class)).isEqualTo(3L);
    }

    @Test
    @DisplayName("Late arriving events update the historical hour bucket")
    void shouldUpdateHistoricalBucketForLateEvents() {
        long applicationId = seedApplication("proj_late", "late", "development");
        ApiEndpoint endpoint = seedEndpoint(applicationId, "GET", "/api/products");
        Instant at = Instant.parse("2026-10-14T10:05:00Z");

        endpointService.aggregateIfNew(endpoint, usageEvent(applicationId, 200, at));
        endpointService.aggregateIfNew(
                endpoint, usageEvent(applicationId, 500, at.minusSeconds(300)));

        List<AggregateRow> rows = aggregateRows(endpoint.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).requestCount()).isEqualTo(2L);
        assertThat(rows.get(0).successCount()).isEqualTo(1L);
        assertThat(rows.get(0).errorCount()).isEqualTo(1L);
        assertThat(rows.get(0).firstSeen()).isEqualTo(at.minusSeconds(300));
        assertThat(rows.get(0).lastSeen()).isEqualTo(at);
    }

    @Test
    @DisplayName("Concurrent consumers increment the same bucket atomically")
    void shouldAggregateConcurrentlyWithoutLostUpdates() throws Exception {
        long applicationId = seedApplication("proj_concurrent", "concurrent", "development");
        ApiEndpoint endpoint = seedEndpoint(applicationId, "GET", "/api/products");
        Instant at = Instant.parse("2026-10-14T10:45:00Z");
        int threads = 8;
        int perThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        try {
            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            endpointService.aggregateIfNew(endpoint, usageEvent(applicationId, 200, at));
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(aggregateRows(endpoint.getId()).get(0).requestCount())
                .isEqualTo((long) threads * perThread);
    }

    @Test
    @DisplayName("Identical field names on different endpoints stay separate")
    void shouldNotMergeSameFieldNameAcrossEndpoints() {
        long applicationId = seedApplication("proj_fields", "fields", "development");

        fieldService.aggregateBatchIfNew(applicationWithId(applicationId), fieldBatch(
                UUID.randomUUID(), "/api/products/{id}", "Product", List.of("name"),
                "product-web", "1.0.0", Instant.parse("2026-10-14T10:00:00Z")));
        fieldService.aggregateBatchIfNew(applicationWithId(applicationId), fieldBatch(
                UUID.randomUUID(), "/api/users/{id}", "User", List.of("name"),
                "product-web", "1.0.0", Instant.parse("2026-10-14T10:00:00Z")));

        List<Object[]> rows = jdbcTemplate.query("""
                select endpoint, field_path, access_count
                from field_usage_hourly
                where application_id = ?
                order by endpoint
                """, (rs, rowNum) -> new Object[]{
                        rs.getString("endpoint"), rs.getString("field_path"), rs.getLong("access_count")},
                applicationId);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)[0]).isEqualTo("/api/products/{id}");
        assertThat(rows.get(1)[0]).isEqualTo("/api/users/{id}");
    }

    @Test
    @DisplayName("Retention cleanup removes old raw rows but keeps aggregates and recent dedup rows")
    void shouldApplyRetentionPolicyWithoutBreakingAnalytics() {
        long applicationId = seedApplication("proj_retention", "retention", "development");
        ApiEndpoint endpoint = seedEndpoint(applicationId, "GET", "/api/products");
        Instant old = Instant.now().minusSeconds(60L * 24 * 3600);
        UsageEvent oldEvent = usageEvent(applicationId, 200, old);

        jdbcTemplate.update("""
                insert into usage_events (event_id, application_id, http_method, endpoint, status_code, occurred_at)
                values (?, ?, 'GET', '/api/products', 200, ?)
                """, oldEvent.getEventId(), applicationId, Timestamp.from(old));
        endpointService.aggregateIfNew(endpoint, oldEvent);

        TelemetryRetentionProperties properties = new TelemetryRetentionProperties();
        properties.setEnabled(true);
        properties.setRawDays(14);
        properties.setAggregateDays(730);
        properties.setDeduplicationDays(30);
        new TelemetryRetentionService(jdbcTemplate, properties).cleanup();

        assertThat(jdbcTemplate.queryForObject("select count(*) from usage_events", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from endpoint_usage_hourly", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from processed_endpoint_aggregation_events", Long.class)).isEqualTo(1L);
    }

    @Test
    @DisplayName("V3 known-client analysis still works from hourly field aggregates")
    void shouldKeepLifecycleKnownClientAnalysisCompatible() {
        long applicationId = seedApplication("proj_lifecycle", "lifecycle", "development");
        KnownClientAnalyzer analyzer = new KnownClientAnalyzer(jdbcTemplate);
        Instant activeSince = Instant.now().minusSeconds(24 * 3600);

        ApiEndpoint productEndpoint = seedEndpoint(applicationId, "GET", "/api/products/{id}");
        Application application = applicationWithId(applicationId);

        assertThat(analyzer.analyze(lifecycleEndpoint(
                applicationId, productEndpoint.getId(), "GET", "/api/products/{id}"), activeSince).activeClientCount())
                .isZero();
        assertThat(analyzer.analyze(lifecycleEndpoint(
                applicationId, productEndpoint.getId(), "GET", "/api/products/{id}"), activeSince)
                .clientCoverageSufficient())
                .isFalse();

        fieldService.aggregateBatchIfNew(application, fieldBatch(
                UUID.randomUUID(), "/api/products/{id}", "Product", List.of("name"),
                "product-web", "1.0.0", Instant.now()));
        fieldService.aggregateBatchIfNew(application, fieldBatch(
                UUID.randomUUID(), "/api/products/{id}", "Product", List.of("price"),
                "android-app", "3.0.0", Instant.now()));

        var activity = analyzer.analyze(
                lifecycleEndpoint(applicationId, productEndpoint.getId(), "GET", "/api/products/{id}"),
                activeSince);

        assertThat(activity.clientCoverageSufficient()).isTrue();
        assertThat(activity.activeClientCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Analytics API still returns the same DTOs when reading hourly aggregates")
    void shouldKeepAnalyticsResponsesCompatible() {
        long applicationId = seedApplication("proj_analytics", "analytics", "development");
        ApiEndpoint endpoint = seedEndpoint(applicationId, "GET", "/api/products");
        Instant at = Instant.parse("2026-10-14T10:30:00Z");
        endpointService.aggregateIfNew(endpoint, usageEvent(applicationId, 200, at));
        endpointService.aggregateIfNew(endpoint, usageEvent(applicationId, 500, at));

        jdbcTemplate.update("""
                insert into observed_fields (application_id, http_method, endpoint, schema_name, field_path)
                values (?, 'GET', '/api/products', 'Product', ?)
                """, applicationId, "name");
        jdbcTemplate.update("""
                insert into observed_fields (application_id, http_method, endpoint, schema_name, field_path)
                values (?, 'GET', '/api/products', 'Product', ?)
                """, applicationId, "legacyCode");

        fieldService.aggregateBatchIfNew(applicationWithId(applicationId), fieldBatch(
                UUID.randomUUID(), "/api/products", "Product", List.of("name"),
                "product-web", "1.0.0", at));

        AnalyticsServiceImpl analyticsService =
                new AnalyticsServiceImpl(applicationRepositoryReturning(applicationId), jdbcTemplate);

        var endpointResponse = analyticsService.getEndpointAnalytics(
                1L, "proj_analytics", Instant.EPOCH, Instant.now().plusSeconds(60));

        assertThat(endpointResponse.endpoints()).hasSize(1);
        assertThat(endpointResponse.endpoints().get(0).totalRequests()).isEqualTo(2L);
        assertThat(endpointResponse.endpoints().get(0).successCount()).isEqualTo(1L);
        assertThat(endpointResponse.endpoints().get(0).failureCount()).isEqualTo(1L);
        assertThat(endpointResponse.endpoints().get(0).lastSeen()).isEqualTo(at);

        var fieldResponse = analyticsService.getFieldAnalytics(
                1L, "proj_analytics", "/api/products", null, null, Instant.EPOCH, Instant.now().plusSeconds(60));

        assertThat(fieldResponse.fields()).hasSize(2);
        var accessed = fieldResponse.fields().stream()
                .filter(item -> item.fieldPath().equals("name")).findFirst().orElseThrow();
        var unaccessed = fieldResponse.fields().stream()
                .filter(item -> item.fieldPath().equals("legacyCode")).findFirst().orElseThrow();

        assertThat(accessed.totalAccesses()).isEqualTo(1L);
        assertThat(accessed.status()).isEqualTo("ACCESS_OBSERVED");
        assertThat(unaccessed.totalAccesses()).isZero();
        assertThat(unaccessed.clientId()).isNull();
        assertThat(unaccessed.status()).isEqualTo("NO_ACCESS_OBSERVED");
    }

    private ApplicationRepository applicationRepositoryReturning(long applicationId) {
        ApplicationRepository repository = mock(ApplicationRepository.class);
        Application application = mock(Application.class);
        when(application.getId()).thenReturn(applicationId);
        when(application.getProjectId()).thenReturn("proj_analytics");
        when(application.getName()).thenReturn("analytics");
        when(application.getEnvironment()).thenReturn("development");
        when(repository.findByProjectIdAndOwnerId("proj_analytics", 1L)).thenReturn(Optional.of(application));
        return repository;
    }

    private ApiEndpoint lifecycleEndpoint(long applicationId, long endpointId, String method, String path) {
        ApiEndpoint endpoint = mock(ApiEndpoint.class);
        Application application = applicationWithId(applicationId);
        when(endpoint.getId()).thenReturn(endpointId);
        when(endpoint.getHttpMethod()).thenReturn(method);
        when(endpoint.getEndpointPath()).thenReturn(path);
        when(endpoint.getApplication()).thenReturn(application);
        return endpoint;
    }

    @Test
    @DisplayName("Performance: 1000 raw rows versus 1000 events aggregated into 1 row")
    void shouldReportStorageAndWriteCostComparison() {
        long applicationId = seedApplication("proj_benchmark", "benchmark", "development");
        ApiEndpoint endpoint = seedEndpoint(applicationId, "GET", "/api/products");
        Instant at = Instant.parse("2026-10-14T10:00:00Z");

        List<UsageEvent> events = new java.util.ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            events.add(usageEvent(applicationId, 200, at.plusSeconds(i % 3600)));
        }

        long aggregateStartedAt = System.nanoTime();
        events.forEach(event -> endpointService.aggregateIfNew(endpoint, event));
        long aggregateMillis = (System.nanoTime() - aggregateStartedAt) / 1_000_000;

        jdbcTemplate.update("delete from processed_endpoint_aggregation_events");
        jdbcTemplate.update("delete from endpoint_usage_hourly");

        List<EndpointUsageAggregationService.PendingEndpointEvent> pending =
                events.stream()
                        .map(event -> new EndpointUsageAggregationService.PendingEndpointEvent(endpoint, event))
                        .toList();
        long batchedStartedAt = System.nanoTime();
        endpointService.aggregateAllIfNew(pending);
        long batchedMillis = (System.nanoTime() - batchedStartedAt) / 1_000_000;

        long rawStartedAt = System.nanoTime();
        events.forEach(event -> jdbcTemplate.update("""
                insert into usage_events (event_id, application_id, http_method, endpoint, status_code, occurred_at)
                values (?, ?, 'GET', '/api/products', 200, ?)
                """, event.getEventId(), applicationId, Timestamp.from(event.getOccurredAt())));
        long rawMillis = (System.nanoTime() - rawStartedAt) / 1_000_000;

        long aggregateRows = jdbcTemplate.queryForObject(
                "select count(*) from endpoint_usage_hourly", Long.class);
        long rawRows = jdbcTemplate.queryForObject("select count(*) from usage_events", Long.class);

        System.out.println("STORAGE COMPARISON -> raw_rows=" + rawRows
                + " aggregate_rows=" + aggregateRows
                + " raw_write_ms=" + rawMillis
                + " aggregate_write_ms_per_event=" + aggregateMillis
                + " aggregate_write_ms_batched=" + batchedMillis
                + " rows_saved=" + (rawRows - aggregateRows));

        assertThat(aggregateRows).isEqualTo(1L);
        assertThat(rawRows).isEqualTo(1_000L);
    }

    private static void resolveDatabase() {
        String externalUrl = System.getProperty(JDBC_URL_PROPERTY);
        if (externalUrl != null && !externalUrl.isBlank()) {
            jdbcUrl = externalUrl;
            jdbcUsername = System.getProperty(JDBC_USERNAME_PROPERTY, "postgres");
            jdbcPassword = System.getProperty(JDBC_PASSWORD_PROPERTY, "postgres");
            return;
        }

        if (startEmbeddedPostgres()) {
            return;
        }

        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "No disposable PostgreSQL available; skipping aggregation integration test");
        POSTGRES.start();
        jdbcUrl = POSTGRES.getJdbcUrl();
        jdbcUsername = POSTGRES.getUsername();
        jdbcPassword = POSTGRES.getPassword();
    }

    private static boolean startEmbeddedPostgres() {
        try {
            embeddedPostgres = EmbeddedPostgres.builder()
                    .setServerConfig("timezone", "UTC")
                    .start();
            jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres");
            jdbcUsername = "postgres";
            jdbcPassword = "postgres";
            return true;
        } catch (Exception exception) {
            embeddedPostgres = null;
            return false;
        }
    }

    @AfterAll
    static void stopDatabase() throws java.io.IOException {
        if (embeddedPostgres != null) {
            embeddedPostgres.close();
        }
        if (POSTGRES.isRunning()) {
            POSTGRES.stop();
        }
    }

    private List<Long> countsByEndpoint() {
        return jdbcTemplate.queryForList(
                "select request_count from endpoint_usage_hourly order by request_count", Long.class);
    }

    private List<AggregateRow> aggregateRows(long endpointId) {
        return jdbcTemplate.query("""
                select hour_bucket, request_count, success_count, error_count, first_seen, last_seen
                from endpoint_usage_hourly
                where endpoint_id = ?
                order by hour_bucket
                """, (rs, rowNum) -> new AggregateRow(
                        rs.getTimestamp("hour_bucket").toInstant(),
                        rs.getLong("request_count"),
                        rs.getLong("success_count"),
                        rs.getLong("error_count"),
                        rs.getTimestamp("first_seen").toInstant(),
                        rs.getTimestamp("last_seen").toInstant()),
                endpointId);
    }

    private FieldUsageBatchRequest fieldBatch(UUID batchId,
                                              String endpoint,
                                              String schema,
                                              List<String> fieldPaths,
                                              String clientId,
                                              String clientVersion,
                                              Instant observedAt) {
        return new FieldUsageBatchRequest(
                batchId,
                "proj",
                clientId,
                clientVersion,
                observedAt,
                List.of(new ObservedFieldsRequest(
                        "GET",
                        endpoint,
                        schema,
                        fieldPaths,
                        fieldPaths.stream()
                                .map(path -> new AccessedFieldRequest(path, 1))
                                .toList())));
    }

    private long seedApplication(String projectId, String name, String environment) {
        jdbcTemplate.update(
                "insert into users (email, name, password_hash, created_at) values (?, ?, ?, now())",
                projectId + "@example.com", "Test Owner", "hash");
        Long ownerId = jdbcTemplate.queryForObject(
                "select id from users where email = ?", Long.class, projectId + "@example.com");
        jdbcTemplate.update("""
                insert into applications (project_id, name, environment, owner_id, created_at)
                values (?, ?, ?, ?, now())
                """, projectId, name, environment, ownerId);
        return jdbcTemplate.queryForObject(
                "select id from applications where project_id = ?", Long.class, projectId);
    }

    private ApiEndpoint seedEndpoint(long applicationId, String method, String path) {
        jdbcTemplate.update("""
                insert into api_endpoints (application_id, http_method, endpoint_path)
                values (?, ?, ?)
                """, applicationId, method, path);
        long id = jdbcTemplate.queryForObject("""
                select id from api_endpoints
                where application_id = ? and http_method = ? and endpoint_path = ?
                """, Long.class, applicationId, method, path);
        return endpointWithId(id);
    }

    private static ApiEndpoint endpointWithId(long id) {
        ApiEndpoint endpoint = mock(ApiEndpoint.class);
        when(endpoint.getId()).thenReturn(id);
        return endpoint;
    }

    private Application applicationWithId(long id) {
        Application application = mock(Application.class);
        when(application.getId()).thenReturn(id);
        return application;
    }

    private UsageEvent usageEvent(long applicationId, int statusCode, Instant occurredAt) {
        UsageEvent event = mock(UsageEvent.class);
        when(event.getEventId()).thenReturn(UUID.randomUUID());
        when(event.getStatusCode()).thenReturn(statusCode);
        when(event.getOccurredAt()).thenReturn(occurredAt);
        return event;
    }

    private record AggregateRow(
            Instant hourBucket,
            long requestCount,
            long successCount,
            long errorCount,
            Instant firstSeen,
            Instant lastSeen) {
    }
}