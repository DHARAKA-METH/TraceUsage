package com.traceusage.traceusage.aggregation;

import com.traceusage.traceusage.apikey.service.ApiKeyHashService;
import com.traceusage.traceusage.fieldusage.dto.*;
import com.traceusage.traceusage.fieldusage.service.FieldUsageService;
import com.traceusage.traceusage.messaging.config.IngestionProperties;
import com.traceusage.traceusage.telemetry.dto.UsageEventRequest;
import com.traceusage.traceusage.telemetry.service.TelemetryService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "traceusage.ingestion.mode=kafka", "traceusage.telemetry.retention.enabled=false",
        "spring.jpa.show-sql=false", "app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class EnvironmentIngestionIntegrationTest {
    static final EmbeddedPostgres POSTGRES;
    static {
        try { POSTGRES = EmbeddedPostgres.builder().start(); }
        catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ApiKeyHashService hashes;
    @Autowired TelemetryService endpoints;
    @Autowired FieldUsageService fields;
    @Autowired IngestionProperties ingestion;
    @Autowired com.traceusage.traceusage.analytics.service.AnalyticsService analytics;
    @Autowired com.traceusage.traceusage.fieldusage.service.FieldUsagePersistenceService fieldPersistence;

    @ParameterizedTest
    @ValueSource(strings = {"direct", "kafka"})
    void isolatesEnvironmentsAndDeduplicatesReplay(String mode) {
        ingestion.setMode(mode);
        String project = "proj_" + UUID.randomUUID();
        Long owner = jdbc.queryForObject("insert into users(name,email,password_hash) values ('Test', ?, 'hash') returning id", Long.class, project + "@example.invalid");
        Long app = jdbc.queryForObject("insert into applications(project_id,name,owner_id,environment) values (?, 'Integration', ?, 'development') returning id", Long.class, project, owner);
        Long dev = environment(app, "development");
        Long prod = environment(app, "production");
        String devKey = key(app, dev, "SECRET", "tru_sk_");
        String prodKey = key(app, prod, "SECRET", "tru_sk_");
        String devPublic = key(app, dev, "PUBLIC_BROWSER", "tru_pk_");
        String prodPublic = key(app, prod, "PUBLIC_BROWSER", "tru_pk_");
        Instant at = Instant.now();
        UsageEventRequest devEvent = new UsageEventRequest(UUID.randomUUID(), "GET", "/api/items", 200, at);
        UsageEventRequest prodEvent = new UsageEventRequest(UUID.randomUUID(), "GET", "/api/items", 500, at);
        endpoints.collect(devKey, devEvent);
        endpoints.collect(prodKey, prodEvent);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(endpointCount(app)).isEqualTo(2));
        var devBatch = batch(project, at, 3);
        var prodBatch = batch(project, at, 7);
        fields.collectBatch(devPublic, devBatch);
        fields.collectBatch(prodPublic, prodBatch);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(accesses(dev)).isEqualTo(3);
            assertThat(accesses(prod)).isEqualTo(7);
        });
        endpoints.collect(devKey, devEvent);
        endpoints.collect(prodKey, prodEvent);
        fields.collectBatch(devPublic, devBatch);
        fields.collectBatch(prodPublic, prodBatch);
        // A subsequent distinct event provides a processing barrier for the same endpoint key.
        endpoints.collect(prodKey, new UsageEventRequest(UUID.randomUUID(), "GET", "/api/items", 404, at));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(endpointCount(app)).isEqualTo(3));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(accesses(dev)).isEqualTo(3);
            assertThat(accesses(prod)).isEqualTo(7);
            assertThat(jdbc.queryForList("select h.status_code::integer from endpoint_usage_hourly h join api_endpoints e on e.id=h.endpoint_id where e.environment_id=? order by h.status_code", Integer.class, prod)).containsExactly(404, 500);
            assertThat(jdbc.queryForObject("select count(*) from field_usage_events where environment_id=?", Long.class, prod)).isEqualTo(1);
        });
        var report = analytics.getEndpointAnalytics(owner, project, at.minusSeconds(60), at.plusSeconds(60), prod);
        assertThat(report.environment()).isEqualTo("production");
        assertThat(report.endpoints()).hasSize(1);
        assertThat(report.endpoints().get(0).statusCounts()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(404, 1L, 500, 1L));
        assertThat(report.endpoints().get(0).totalRequests()).isEqualTo(2);
        var fieldReport = analytics.getFieldAnalytics(owner, project, "/api/items", null, null, at.minusSeconds(60), at.plusSeconds(60), prod);
        assertThat(fieldReport.fields()).hasSize(1);
        assertThat(fieldReport.fields().get(0).totalAccesses()).isEqualTo(7);
        Long foreignApp = jdbc.queryForObject("insert into applications(project_id,name,owner_id,environment) values (?, 'Other', ?, 'development') returning id", Long.class, "proj_" + UUID.randomUUID(), owner);
        Long foreignEnvironment = environment(foreignApp, "development");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> analytics.getEndpointAnalytics(owner, project, at, at, foreignEnvironment))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var invalidMessage = com.traceusage.traceusage.messaging.dto.FieldTelemetryBatchMessage.create(UUID.randomUUID(), app, foreignEnvironment,
                project, "integration-web", "1", at, List.of());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fieldPersistence.saveIfNew(invalidMessage))
                .isInstanceOf(IllegalArgumentException.class);
    }
    private Long environment(Long app, String name) {
        return jdbc.queryForObject("insert into environments(application_id,name) values (?,?) returning id", Long.class, app, name);
    }
    private String key(Long app, Long env, String type, String prefix) {
        String raw = prefix + UUID.randomUUID();
        jdbc.update("insert into api_keys(application_id,environment_id,key_prefix,key_hash,key_type) values (?,?,?,?,?)", app, env, prefix, hashes.hash(raw), type);
        return raw;
    }
    private FieldUsageBatchRequest batch(String project, Instant at, int count) {
        return new FieldUsageBatchRequest(UUID.randomUUID(), project, "integration-web", "1", at,
                List.of(new ObservedFieldsRequest("GET", "/api/items", "Items", List.of("[].name"), List.of(new AccessedFieldRequest("[].name", count)))));
    }
    private long endpointCount(Long app) {
        return jdbc.queryForObject("select coalesce(sum(h.request_count),0) from endpoint_usage_hourly h join api_endpoints e on e.id=h.endpoint_id where e.application_id=?", Long.class, app);
    }
    private long accesses(Long env) {
        return jdbc.queryForObject("select coalesce(sum(h.access_count),0) from field_usage_hourly h join api_endpoints e on e.id=h.endpoint_id where e.environment_id=?", Long.class, env);
    }
}
