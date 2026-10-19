ALTER TABLE field_usage_hourly ADD COLUMN last_accessed TIMESTAMPTZ;
UPDATE field_usage_hourly SET last_accessed = last_seen WHERE access_count > 0;
