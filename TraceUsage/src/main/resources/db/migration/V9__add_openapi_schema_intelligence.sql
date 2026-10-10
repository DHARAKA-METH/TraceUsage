CREATE TABLE openapi_specs (
    id BIGSERIAL PRIMARY KEY,

    application_id BIGINT NOT NULL,

    file_name VARCHAR(255) NOT NULL,

    content_type VARCHAR(100),

    document_hash VARCHAR(128) NOT NULL,

    raw_document TEXT NOT NULL,

    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_openapi_spec_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id),

    CONSTRAINT uk_openapi_spec_document_hash
        UNIQUE (application_id, document_hash)
);

CREATE INDEX idx_openapi_specs_application
    ON openapi_specs (application_id);


CREATE TABLE openapi_endpoints (
    id BIGSERIAL PRIMARY KEY,

    application_id BIGINT NOT NULL,

    spec_id BIGINT NOT NULL,

    api_endpoint_id BIGINT,

    http_method VARCHAR(10) NOT NULL,

    endpoint_path VARCHAR(500) NOT NULL,

    operation_id VARCHAR(255),

    summary VARCHAR(500),

    deprecated BOOLEAN NOT NULL DEFAULT FALSE,

    discovered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_openapi_endpoint_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id),

    CONSTRAINT fk_openapi_endpoint_spec
        FOREIGN KEY (spec_id)
            REFERENCES openapi_specs(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_openapi_endpoint_api_endpoint
        FOREIGN KEY (api_endpoint_id)
            REFERENCES api_endpoints(id),

    CONSTRAINT uk_openapi_endpoint
        UNIQUE (spec_id, http_method, endpoint_path)
);

CREATE INDEX idx_openapi_endpoints_application
    ON openapi_endpoints (application_id);

CREATE INDEX idx_openapi_endpoints_spec
    ON openapi_endpoints (spec_id);

CREATE INDEX idx_openapi_endpoints_api_endpoint
    ON openapi_endpoints (api_endpoint_id);

CREATE INDEX idx_openapi_endpoints_deprecated
    ON openapi_endpoints (deprecated);


CREATE TABLE openapi_response_fields (
    id BIGSERIAL PRIMARY KEY,

    application_id BIGINT NOT NULL,

    spec_id BIGINT NOT NULL,

    openapi_endpoint_id BIGINT NOT NULL,

    response_status VARCHAR(10) NOT NULL,

    content_type VARCHAR(100) NOT NULL DEFAULT 'application/json',

    schema_name VARCHAR(255),

    field_path VARCHAR(1000) NOT NULL,

    field_type VARCHAR(100),

    required BOOLEAN NOT NULL DEFAULT FALSE,

    nullable BOOLEAN NOT NULL DEFAULT FALSE,

    deprecated BOOLEAN NOT NULL DEFAULT FALSE,

    description TEXT,

    discovered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_openapi_response_field_application
        FOREIGN KEY (application_id)
            REFERENCES applications(id),

    CONSTRAINT fk_openapi_response_field_spec
        FOREIGN KEY (spec_id)
            REFERENCES openapi_specs(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_openapi_response_field_endpoint
        FOREIGN KEY (openapi_endpoint_id)
            REFERENCES openapi_endpoints(id)
            ON DELETE CASCADE,

    CONSTRAINT uk_openapi_response_field
        UNIQUE (
            openapi_endpoint_id,
            response_status,
            content_type,
            field_path
        )
);

CREATE INDEX idx_openapi_response_fields_application
    ON openapi_response_fields (application_id);

CREATE INDEX idx_openapi_response_fields_spec
    ON openapi_response_fields (spec_id);

CREATE INDEX idx_openapi_response_fields_endpoint
    ON openapi_response_fields (openapi_endpoint_id);

CREATE INDEX idx_openapi_response_fields_field_path
    ON openapi_response_fields (field_path);

CREATE INDEX idx_openapi_response_fields_deprecated
    ON openapi_response_fields (deprecated);
