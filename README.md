# TraceUsage

TraceUsage is an API usage monitoring and lifecycle decision-support platform. It
instruments Spring Boot APIs (V1) and browser applications (V2), ingests
telemetry through Kafka (V7), evaluates endpoint lifecycle status (V3), and
combines OpenAPI contracts with observed runtime behaviour (V5).

## Modules

| Module | Purpose |
| --- | --- |
| `traceusage-spring-boot-starter` | V1 backend SDK: request usage instrumentation and async HTTP batching |
| `traceusage-browser-sdk` | V2 browser SDK: response field discovery and Proxy-based field access tracking |
| `TraceUsage` | Spring Boot server: collector APIs, Kafka pipeline, analytics, lifecycle, OpenAPI intelligence |
| `traceusage-dashboard` | Next.js dashboard (Tailwind CSS) |
| `traceusage-demo-api` | Demo Spring Boot API instrumented with the starter |
| `traceusage-demo-web` | Demo frontend instrumented with the browser SDK |

## Telemetry storage architecture

```
Backend SDK -----------+
                        |
Browser SDK -----------+
                        v
                 Collector API
                        |
                        v
                      Kafka
                        |
             +----------+----------+
             |                     |
             v                     v
      Endpoint Consumer       Field Consumer
             |                     |
             v                     v
      Idempotent Aggregation  Idempotent Aggregation
             |                     |
             v                     v
      endpoint_usage_hourly  field_usage_hourly
             |                     |
             +----------+----------+
                        |
                        v
                    PostgreSQL
                        |
             +----------+----------+
             |                     |
             v                     v
      Recent Raw Data       Long-Term Rollups
             |                     |
             +----------+----------+
                        |
                        v
                   Analytics API
                        |
                 +------+------+
                 |             |
                 v             v
        Lifecycle Evaluation  Dashboard
```

### Why aggregation exists

Every individual request used to become its own PostgreSQL row. At 1,000
requests/minute that is ~43 million rows per 30 days per endpoint, which is
unsustainable for a product whose primary job is analytics.

TraceUsage now folds telemetry into **fixed one-hour UTC buckets**:

* 1,000 requests to `GET /api/products` in the same application, environment and
  hour produce **one** row in `endpoint_usage_hourly`.
* Counts stay exact: `request_count`, `success_count`, `error_count`,
  `first_seen` and `last_seen` are preserved per bucket.
* Analytics, lifecycle evaluation and OpenAPI analysis read the aggregates.
* Raw rows are still written for recent troubleshooting and are removed on a
  configurable schedule.

Kafka batching never reduced database row counts on its own; the reduction comes
from this consumer-side rollup.

### Aggregation keys

Aggregation never merges distinct identities:

| Table | Identity |
| --- | --- |
| `endpoint_usage_hourly` | `endpoint_id` + `hour_bucket` (`api_endpoints.id` already encodes application, environment, HTTP method and normalized path) |
| `field_usage_hourly` | `application_id`, `endpoint_id`, `schema_name`, `field_path`, `client_id`, `client_version`, `hour_bucket` |

`GET /api/users/{id} -> name` and `GET /api/products/{id} -> name` therefore stay
separate rows, and `supplier.name`, `products[].name` and
`orders[].items[].price` are never collapsed. Missing response-schema
information is stored as an empty string rather than being fabricated, so
historical events are never joined to an unrelated OpenAPI response variant.

### Idempotency (exactly-once counting)

Kafka is at-least-once. A naive upsert would inflate counts when a consumer
crashes before committing its offset. TraceUsage instead uses persistent
deduplication inside PostgreSQL:

* Endpoint events: `processed_endpoint_aggregation_events (event_id PK)`.
* Field batches: `processed_field_aggregation_batches (batch_id PK)` — the
  browser SDK's stable batch id.

The claim statement and the aggregate upserts run in the **same transaction**:

```sql
insert into processed_endpoint_aggregation_events (event_id, endpoint_id)
select claimed.event_id, claimed.endpoint_id
from unnest(?::uuid[], ?::bigint[]) as claimed(event_id, endpoint_id)
on conflict (event_id) do nothing
returning event_id
```

Only event ids returned by that statement contribute to counts, so a redelivered
record is recognised as a duplicate and increments nothing. Offsets are still
committed only after the database transaction succeeds.

Deduplication rows are retained for 30 days by default, which is longer than the
Kafka topic retention window. If they were deleted earlier, a replayed record
could be counted a second time.

### Retention policy

```properties
traceusage.telemetry.retention.enabled=true
traceusage.telemetry.retention.storage-mode=HYBRID   # or RAW_ONLY
traceusage.telemetry.retention.raw-days=14
traceusage.telemetry.retention.aggregate-days=730
traceusage.telemetry.retention.deduplication-days=30
traceusage.telemetry.retention.cleanup-interval-ms=3600000
```

* `HYBRID` (default) keeps recent raw rows for troubleshooting and long-term
  aggregates for analytics.
* `RAW_ONLY` disables raw-row deletion; aggregates are still maintained and
  deduplication rows are still trimmed.
* Cleanup runs in bounded batches and is restartable.
* `AGGREGATED_ONLY` is intentionally not implemented: skipping raw writes would
  complicate incident debugging without changing the correctness guarantees.

### Backfill

Migration `V10__add_hourly_telemetry_aggregates.sql` backfills the aggregate
tables from existing `usage_events` and `field_usage_events` rows with pure SQL
(`GROUP BY date_trunc('hour', ...)`), so nothing is loaded into JVM memory. The
same migration seeds the deduplication tables from the existing raw event ids,
which prevents double counting if Kafka replays historical records after
deploy. No raw row is deleted by the migration.

### Configuration

| Property | Default | Meaning |
| --- | --- | --- |
| `traceusage.ingestion.mode` | `direct` | `direct` or `kafka` |
| `traceusage.telemetry.retention.*` | see above | storage and cleanup policy |
| `management.endpoints.web.exposure.include` | `health,info,metrics` | Actuator endpoints |

Switching between `direct` and `kafka` ingestion does not change any SDK-facing
HTTP endpoint.

### Metrics

Actuator exposes `traceusage.aggregation.endpoint.events`
(`result=accepted|duplicate|error`), `traceusage.aggregation.field.batches`
(`result=accepted|duplicate`), `traceusage.aggregation.field.accesses` and
`traceusage.aggregation.endpoint.upsert` (timer). Kafka consumer lag and
dead-letter behaviour are reported by the Spring Boot Kafka metrics registry.
No telemetry values, headers or credentials are logged.

### Precision note

Analytics are exact for hour-aligned windows. A query window that starts or ends
in the middle of an hour is evaluated against the buckets that overlap it, so
sub-hour windows are approximate by up to one hour on each edge. Raw rows remain
available for the retention period when exact arbitrary-window reporting is
required.

## Verification

```bash
# Server
cd TraceUsage && ./mvnw test

# Dashboard
cd traceusage-dashboard && npm run build
```

The PostgreSQL aggregation tests use a real disposable PostgreSQL (embedded
binary, Testcontainers, or `-Dtraceusage.it.jdbc-url=...` in CI) and run the
production Flyway migrations before exercising the production aggregation code.