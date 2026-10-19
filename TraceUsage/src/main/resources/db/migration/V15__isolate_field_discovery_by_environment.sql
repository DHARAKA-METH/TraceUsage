ALTER TABLE api_endpoints DROP CONSTRAINT uk_api_endpoint;
ALTER TABLE observed_fields DROP CONSTRAINT uk_observed_field;
ALTER TABLE observed_fields ADD CONSTRAINT uk_observed_field
    UNIQUE(application_id, environment_id, http_method, endpoint, schema_name, field_path);
