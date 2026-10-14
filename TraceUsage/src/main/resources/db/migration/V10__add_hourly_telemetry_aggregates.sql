CREATE TABLE endpoint_usage_hourly (
    endpoint_id BIGINT NOT NULL,
    hour_bucket TIMESTAMPTZ NOT NULL,
    request_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    error_count BIGINT NOT NULL DEFAULT 0,
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_endpoint_usage_hourly
        PRIMARY KEY (endpoint_id, hour_bucket),

    CONSTRAINT fk_endpoint_usage_hourly_endpoint
        FOREIGN KEY (endpoint_id)
            REFERENCES api_endpoints(id),

    CONSTRAINT chk_endpoint_usage_hourly_counts
        CHECK (request_count >= 0 AND success_count >= 0 AND error_count >= 0),

    CONSTRAINT chk_endpoint_usage_hourly_seen_order
        CHECK (first_seen <= last_seen)
);

CREATE INDEX idx_endpoint_usage_hourly_bucket
    ON endpoint_usage_hourly (hour_bucket);

CREATE INDEX idx_endpoint_usage_hourly_endpoint_last_seen
    ON endpoint_usage_hourly (endpoint_id, last_seen);

CREATE TABLE processed_endpoint_aggregation_events (
    event_id UUID PRIMARY KEY,
    endpoint_id BIGINT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_processed_endpoint_aggregation_endpoint
        FOREIGN KEY (endpoint_id)
            REFERENCES api_endpoints(id)
);

CREATE INDEX idx_processed_endpoint_aggregation_processed_at
    ON processed_endpoint_aggregation_events (processed_at);

CREATE TABLE field_usage_hourly (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    endpoint_id BIGINT NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    schema_name VARCHAR(150) NOT NULL DEFAULT '',
    field_path VARCHAR(500) NOT NULL,
    client_id VARCHAR(150) NOT NULL,
    client_version VARCHAR(80) NOT NULL,
    hour_bucket TIMESTAMPTZ NOT NULL,
    access_count BIGINT NOT NULL DEFAULT 0,
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_field_usage_hourly_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id),

    CONSTRAINT fk_field_usage_hourly_endpoint
        FOREIGN KEY (endpoint_id)
            REFERENCES api_endpoints(id),

    CONSTRAINT uk_field_usage_hourly_bucket
        UNIQUE (
            application_id,
            endpoint_id,
            schema_name,
            field_path,
            client_id,
            client_version,
            hour_bucket
        ),

    CONSTRAINT chk_field_usage_hourly_access_count
        CHECK (access_count >= 0),

    CONSTRAINT chk_field_usage_hourly_seen_order
        CHECK (first_seen <= last_seen)
);

CREATE INDEX idx_field_usage_hourly_application_bucket
    ON field_usage_hourly (application_id, hour_bucket);

CREATE INDEX idx_field_usage_hourly_endpoint_field
    ON field_usage_hourly (endpoint_id, field_path);

CREATE INDEX idx_field_usage_hourly_client
    ON field_usage_hourly (application_id, client_id, client_version);

CREATE TABLE processed_field_aggregation_batches (
    batch_id UUID PRIMARY KEY,
    application_id BIGINT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_processed_field_aggregation_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id)
);

CREATE INDEX idx_processed_field_aggregation_processed_at
    ON processed_field_aggregation_batches (processed_at);

-- Backfill endpoint hourly aggregates from existing raw endpoint events.
INSERT INTO endpoint_usage_hourly (
    endpoint_id,
    hour_bucket,
    request_count,
    success_count,
    error_count,
    first_seen,
    last_seen
)
SELECT ae.id,
       date_trunc('hour', ue.occurred_at),
       COUNT(*),
       COUNT(*) FILTER (WHERE ue.status_code BETWEEN 200 AND 399),
       COUNT(*) FILTER (WHERE ue.status_code >= 400),
       MIN(ue.occurred_at),
       MAX(ue.occurred_at)
FROM usage_events ue
JOIN api_endpoints ae
  ON ae.application_id = ue.application_id
 AND ae.http_method = ue.http_method
 AND ae.endpoint_path = ue.endpoint
GROUP BY ae.id, date_trunc('hour', ue.occurred_at)
ON CONFLICT (endpoint_id, hour_bucket) DO UPDATE SET
    request_count = endpoint_usage_hourly.request_count + EXCLUDED.request_count,
    success_count = endpoint_usage_hourly.success_count + EXCLUDED.success_count,
    error_count = endpoint_usage_hourly.error_count + EXCLUDED.error_count,
    first_seen = LEAST(endpoint_usage_hourly.first_seen, EXCLUDED.first_seen),
    last_seen = GREATEST(endpoint_usage_hourly.last_seen, EXCLUDED.last_seen);

INSERT INTO processed_endpoint_aggregation_events (event_id, endpoint_id)
SELECT ue.event_id, ae.id
FROM usage_events ue
JOIN api_endpoints ae
  ON ae.application_id = ue.application_id
 AND ae.http_method = ue.http_method
 AND ae.endpoint_path = ue.endpoint
ON CONFLICT (event_id) DO NOTHING;

-- Backfill field hourly aggregates from existing raw field usage events.
INSERT INTO field_usage_hourly (
    application_id,
    endpoint_id,
    http_method,
    endpoint,
    schema_name,
    field_path,
    client_id,
    client_version,
    hour_bucket,
    access_count,
    first_seen,
    last_seen
)
SELECT f.application_id,
       ae.id,
       f.http_method,
       f.endpoint,
       COALESCE(f.schema_name, ''),
       f.field_path,
       f.client_id,
       f.client_version,
       date_trunc('hour', f.observed_at),
       SUM(f.access_count),
       MIN(f.observed_at),
       MAX(f.observed_at)
FROM field_usage_events f
JOIN api_endpoints ae
  ON ae.application_id = f.application_id
 AND ae.http_method = f.http_method
 AND ae.endpoint_path = f.endpoint
GROUP BY f.application_id,
         ae.id,
         f.http_method,
         f.endpoint,
         COALESCE(f.schema_name, ''),
         f.field_path,
         f.client_id,
         f.client_version,
         date_trunc('hour', f.observed_at)
ON CONFLICT (
    application_id,
    endpoint_id,
    schema_name,
    field_path,
    client_id,
    client_version,
    hour_bucket
) DO UPDATE SET
    access_count = field_usage_hourly.access_count + EXCLUDED.access_count,
    first_seen = LEAST(field_usage_hourly.first_seen, EXCLUDED.first_seen),
    last_seen = GREATEST(field_usage_hourly.last_seen, EXCLUDED.last_seen);

INSERT INTO processed_field_aggregation_batches (batch_id, application_id)
SELECT batch_id, application_id
FROM processed_field_usage_batches
ON CONFLICT (batch_id) DO NOTHING;
