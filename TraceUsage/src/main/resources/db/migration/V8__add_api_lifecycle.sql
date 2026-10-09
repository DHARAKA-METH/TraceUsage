ALTER TABLE applications
    ADD COLUMN monitoring_started_at TIMESTAMPTZ;

ALTER TABLE applications
    ADD COLUMN inactivity_threshold_days INTEGER NOT NULL DEFAULT 90;

ALTER TABLE applications
    ADD CONSTRAINT chk_inactivity_threshold
        CHECK (inactivity_threshold_days > 0);

-- Derive monitoring start from the first real telemetry event when available,
-- otherwise fall back to application creation time.
UPDATE applications a
SET monitoring_started_at = COALESCE(
        (SELECT MIN(ue.occurred_at)
         FROM usage_events ue
         WHERE ue.application_id = a.id),
        a.created_at
    );

CREATE TABLE api_endpoints (
    id BIGSERIAL PRIMARY KEY,

    application_id BIGINT NOT NULL,

    http_method VARCHAR(10) NOT NULL,

    endpoint_path VARCHAR(500) NOT NULL,

    first_discovered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    last_seen_at TIMESTAMPTZ,

    first_seen_at TIMESTAMPTZ,

    request_count BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_api_endpoint_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id),

    CONSTRAINT uk_api_endpoint
        UNIQUE (application_id, http_method, endpoint_path)
);

-- Register every endpoint TraceUsage has ever observed.
INSERT INTO api_endpoints (
    application_id,
    http_method,
    endpoint_path,
    first_seen_at,
    last_seen_at,
    request_count
)
SELECT ue.application_id,
       ue.http_method,
       ue.endpoint,
       MIN(ue.occurred_at),
       MAX(ue.occurred_at),
       COUNT(*)
FROM usage_events ue
GROUP BY ue.application_id, ue.http_method, ue.endpoint
ON CONFLICT ON CONSTRAINT uk_api_endpoint DO NOTHING;

CREATE INDEX idx_api_endpoints_application
    ON api_endpoints (application_id);

CREATE TABLE endpoint_deprecations (
    id BIGSERIAL PRIMARY KEY,

    endpoint_id BIGINT NOT NULL UNIQUE,

    deprecated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    reason VARCHAR(1000),

    replacement_endpoint VARCHAR(500),

    target_removal_date DATE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_endpoint_deprecation_endpoint
        FOREIGN KEY (endpoint_id)
            REFERENCES api_endpoints(id),

    CONSTRAINT chk_target_removal_date
        CHECK (target_removal_date IS NULL OR target_removal_date >= CURRENT_DATE)
);

CREATE INDEX idx_endpoint_deprecations_endpoint
    ON endpoint_deprecations (endpoint_id);