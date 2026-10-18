-- Applications previously represented exactly one environment. Preserve that
-- identity by creating one environment row for every existing application.
CREATE TABLE environments (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id),
    name VARCHAR(30) NOT NULL,
    monitoring_started_at TIMESTAMPTZ,
    inactivity_threshold_days INTEGER NOT NULL DEFAULT 90,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_environment_application_name UNIQUE (application_id, name),
    CONSTRAINT chk_environment_inactivity_threshold CHECK (inactivity_threshold_days > 0)
);

INSERT INTO environments (application_id, name, monitoring_started_at, inactivity_threshold_days, created_at, updated_at)
SELECT id, environment, monitoring_started_at, inactivity_threshold_days, created_at, CURRENT_TIMESTAMP
FROM applications;

ALTER TABLE api_keys ADD COLUMN environment_id BIGINT;
UPDATE api_keys k
SET environment_id = e.id
FROM environments e
WHERE e.application_id = k.application_id;
ALTER TABLE api_keys
    ADD CONSTRAINT fk_api_key_environment FOREIGN KEY (environment_id) REFERENCES environments(id);
CREATE INDEX idx_api_keys_environment_id ON api_keys(environment_id);

ALTER TABLE api_endpoints ADD COLUMN environment_id BIGINT;
UPDATE api_endpoints ep
SET environment_id = e.id
FROM environments e
WHERE e.application_id = ep.application_id;
ALTER TABLE api_endpoints
    ADD CONSTRAINT fk_api_endpoint_environment FOREIGN KEY (environment_id) REFERENCES environments(id);
CREATE UNIQUE INDEX uk_api_endpoints_environment_method_path
    ON api_endpoints(environment_id, http_method, endpoint_path);
CREATE INDEX idx_api_endpoints_environment ON api_endpoints(environment_id);

-- Runtime response-field identity is now anchored to a concrete endpoint.
CREATE TABLE api_fields (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES api_endpoints(id),
    schema_name VARCHAR(150) NOT NULL DEFAULT '',
    field_path VARCHAR(500) NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_api_fields_endpoint_schema_path UNIQUE(endpoint_id, schema_name, field_path),
    CONSTRAINT chk_api_fields_seen_order CHECK(first_seen_at <= last_seen_at)
);

INSERT INTO api_fields (endpoint_id, schema_name, field_path, first_seen_at, last_seen_at)
SELECT ep.id, COALESCE(ofs.schema_name, ''), ofs.field_path, ofs.first_seen, ofs.last_seen
FROM observed_fields ofs
JOIN api_endpoints ep
  ON ep.application_id = ofs.application_id
 AND ep.http_method = ofs.http_method
 AND ep.endpoint_path = ofs.endpoint
ON CONFLICT (endpoint_id, schema_name, field_path) DO UPDATE
SET first_seen_at = LEAST(api_fields.first_seen_at, EXCLUDED.first_seen_at),
    last_seen_at = GREATEST(api_fields.last_seen_at, EXCLUDED.last_seen_at);

CREATE INDEX idx_api_fields_endpoint ON api_fields(endpoint_id);

-- OpenAPI is application-level; runtime matching is environment-specific.
CREATE TABLE openapi_endpoint_environment_matches (
    openapi_endpoint_id BIGINT NOT NULL REFERENCES openapi_endpoints(id) ON DELETE CASCADE,
    api_endpoint_id BIGINT NOT NULL REFERENCES api_endpoints(id) ON DELETE CASCADE,
    matched_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    match_source VARCHAR(30) NOT NULL DEFAULT 'MIGRATION',
    PRIMARY KEY (openapi_endpoint_id, api_endpoint_id)
);

INSERT INTO openapi_endpoint_environment_matches (openapi_endpoint_id, api_endpoint_id, match_source)
SELECT oe.id, ep.id, 'MIGRATION'
FROM openapi_endpoints oe
JOIN api_endpoints ep
  ON ep.application_id = oe.application_id
 AND ep.http_method = oe.http_method
 AND ep.endpoint_path = oe.endpoint_path
ON CONFLICT DO NOTHING;

CREATE INDEX idx_openapi_endpoint_environment_matches_endpoint
    ON openapi_endpoint_environment_matches(api_endpoint_id);

-- Unified idempotency ledger. Existing ledgers remain during the compatibility
-- window and are copied to this table for reconciliation.
CREATE TABLE processed_events (
    application_id BIGINT NOT NULL REFERENCES applications(id),
    event_kind VARCHAR(30) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (application_id, event_kind, event_id),
    CONSTRAINT chk_processed_events_kind CHECK (event_kind IN ('ENDPOINT_EVENT', 'FIELD_BATCH'))
);

INSERT INTO processed_events (application_id, event_kind, event_id, processed_at)
SELECT ep.application_id, 'ENDPOINT_EVENT', pe.event_id, pe.processed_at
FROM processed_endpoint_aggregation_events pe
JOIN api_endpoints ep ON ep.id = pe.endpoint_id
ON CONFLICT DO NOTHING;

INSERT INTO processed_events (application_id, event_kind, event_id, processed_at)
SELECT application_id, 'FIELD_BATCH', batch_id, processed_at
FROM processed_field_aggregation_batches
ON CONFLICT DO NOTHING;

CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);
