-- Keep legacy dimensions during the transition so historical analytics and
-- rollback remain possible. New writes can use field_id as the canonical key.
ALTER TABLE field_usage_hourly ADD COLUMN field_id BIGINT;
ALTER TABLE field_usage_hourly
    ADD CONSTRAINT fk_field_usage_hourly_api_field
    FOREIGN KEY (field_id) REFERENCES api_fields(id);

UPDATE field_usage_hourly h
SET field_id = f.id
FROM api_fields f
WHERE f.endpoint_id = h.endpoint_id
  AND f.schema_name = h.schema_name
  AND f.field_path = h.field_path;

ALTER TABLE field_usage_hourly
    ADD COLUMN observed_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE field_usage_hourly
    ADD CONSTRAINT chk_field_usage_hourly_observed_count CHECK (observed_count >= 0);

CREATE INDEX idx_field_usage_hourly_field_bucket
    ON field_usage_hourly(field_id, hour_bucket);

-- Historical observed_fields rows record discovery but not a count. Keep their
-- observed_count at zero rather than fabricating historical observations.
