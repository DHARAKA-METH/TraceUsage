-- Retain the application foreign key for ownership and backwards-compatible
-- queries; environment_id is the telemetry partition used by new writes.
ALTER TABLE usage_events ADD COLUMN environment_id BIGINT REFERENCES environments(id);
UPDATE usage_events ue
SET environment_id = e.id
FROM environments e
WHERE e.application_id = ue.application_id;
CREATE INDEX idx_usage_events_environment_time ON usage_events(environment_id, occurred_at);

ALTER TABLE field_usage_events ADD COLUMN environment_id BIGINT REFERENCES environments(id);
UPDATE field_usage_events fue
SET environment_id = e.id
FROM environments e
WHERE e.application_id = fue.application_id;
CREATE INDEX idx_field_usage_events_environment_time ON field_usage_events(environment_id, observed_at);

-- Observed fields remain available for backwards-compatible reads while
-- api_fields is adopted by aggregation and analytics.
ALTER TABLE observed_fields ADD COLUMN environment_id BIGINT REFERENCES environments(id);
UPDATE observed_fields ofs
SET environment_id = ep.environment_id
FROM api_endpoints ep
WHERE ep.application_id = ofs.application_id
  AND ep.http_method = ofs.http_method
  AND ep.endpoint_path = ofs.endpoint;
CREATE INDEX idx_observed_fields_environment_endpoint
    ON observed_fields(environment_id, http_method, endpoint);
