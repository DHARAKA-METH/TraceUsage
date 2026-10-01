ALTER TABLE applications
    ADD COLUMN environment VARCHAR(30) NOT NULL DEFAULT 'development';

ALTER TABLE applications
    ALTER COLUMN environment DROP DEFAULT;

ALTER TABLE applications
    ADD CONSTRAINT chk_applications_environment
        CHECK (environment IN ('development', 'staging', 'production'));

CREATE INDEX idx_applications_owner_id ON applications (owner_id);

CREATE INDEX idx_api_keys_application_id ON api_keys (application_id);

CREATE INDEX idx_api_keys_key_prefix ON api_keys (key_prefix);
