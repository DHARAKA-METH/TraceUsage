CREATE TABLE api_keys (
                          id BIGSERIAL PRIMARY KEY,

                          application_id BIGINT NOT NULL,

                          key_prefix VARCHAR(30) NOT NULL,

                          key_hash VARCHAR(255) NOT NULL UNIQUE,

                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          revoked_at TIMESTAMPTZ,

                          CONSTRAINT fk_api_key_application
                              FOREIGN KEY (application_id)
                                  REFERENCES applications(id)
);