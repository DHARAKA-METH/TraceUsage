CREATE TABLE applications (
                              id BIGSERIAL PRIMARY KEY,

                              project_id VARCHAR(100) NOT NULL UNIQUE,

                              name VARCHAR(150) NOT NULL,

                              owner_id BIGINT NOT NULL,

                              created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_application_owner
                                  FOREIGN KEY (owner_id)
                                      REFERENCES users(id)
);