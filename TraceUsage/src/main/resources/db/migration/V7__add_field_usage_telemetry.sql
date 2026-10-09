ALTER TABLE api_keys
    ADD COLUMN key_type VARCHAR(30) NOT NULL DEFAULT 'SECRET';

CREATE INDEX idx_api_keys_hash_type_active
    ON api_keys (key_hash, key_type)
    WHERE revoked_at IS NULL;

CREATE TABLE observed_fields (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    schema_name VARCHAR(150),
    field_path VARCHAR(500) NOT NULL,
    first_seen TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_observed_field_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id),

    CONSTRAINT uk_observed_field
        UNIQUE (application_id, http_method, endpoint, schema_name, field_path)
);

CREATE INDEX idx_observed_fields_application_endpoint
    ON observed_fields (application_id, http_method, endpoint);

CREATE TABLE field_usage_events (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    batch_id UUID NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    schema_name VARCHAR(150),
    field_path VARCHAR(500) NOT NULL,
    client_id VARCHAR(150) NOT NULL,
    client_version VARCHAR(80) NOT NULL,
    access_count INTEGER NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_field_usage_event_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id),

    CONSTRAINT ck_field_usage_access_count_positive
        CHECK (access_count > 0)
);

CREATE INDEX idx_field_usage_events_application_endpoint
    ON field_usage_events (application_id, http_method, endpoint);

CREATE INDEX idx_field_usage_events_application_observed_at
    ON field_usage_events (application_id, observed_at);

CREATE TABLE processed_field_usage_batches (
    id BIGSERIAL PRIMARY KEY,
    batch_id UUID NOT NULL UNIQUE,
    application_id BIGINT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_processed_field_usage_batch_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id)
);
