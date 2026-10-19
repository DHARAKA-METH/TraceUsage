ALTER TABLE endpoint_usage_hourly RENAME TO endpoint_usage_hourly_legacy;

CREATE TABLE endpoint_usage_hourly (
    endpoint_id BIGINT NOT NULL REFERENCES api_endpoints(id),
    hour_bucket TIMESTAMPTZ NOT NULL,
    status_code SMALLINT NOT NULL,
    request_count BIGINT NOT NULL DEFAULT 0,
    first_seen TIMESTAMPTZ NOT NULL,
    last_seen TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (endpoint_id, hour_bucket, status_code),
    CONSTRAINT chk_endpoint_usage_hourly_request_count CHECK (request_count >= 0),
    CONSTRAINT chk_endpoint_usage_hourly_seen_order CHECK (first_seen <= last_seen)
);
CREATE INDEX idx_endpoint_status_hourly_bucket ON endpoint_usage_hourly(hour_bucket);

INSERT INTO endpoint_usage_hourly (endpoint_id, hour_bucket, status_code, request_count, first_seen, last_seen)
SELECT ep.id, date_trunc('hour', ue.occurred_at), ue.status_code, count(*), min(ue.occurred_at), max(ue.occurred_at)
FROM usage_events ue
JOIN api_endpoints ep ON ep.application_id = ue.application_id
  AND ep.http_method = ue.http_method AND ep.endpoint_path = ue.endpoint
  AND ep.environment_id IS NOT DISTINCT FROM ue.environment_id
GROUP BY ep.id, date_trunc('hour', ue.occurred_at), ue.status_code;
