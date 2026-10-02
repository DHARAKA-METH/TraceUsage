CREATE TABLE usage_events (
                              id BIGSERIAL PRIMARY KEY,

                              event_id UUID NOT NULL UNIQUE,

                              application_id BIGINT NOT NULL,

                              http_method VARCHAR(10) NOT NULL,

                              endpoint VARCHAR(500) NOT NULL,

                              status_code INTEGER NOT NULL,

                              occurred_at TIMESTAMPTZ NOT NULL,

                              created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_usage_event_application
                                  FOREIGN KEY (application_id)
                                      REFERENCES applications(id)
);

CREATE INDEX idx_usage_events_application_time
    ON usage_events (application_id, occurred_at);
