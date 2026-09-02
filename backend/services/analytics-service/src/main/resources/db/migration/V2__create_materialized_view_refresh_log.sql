CREATE TABLE analytics.materialized_view_refresh_log
(
    id             BIGSERIAL PRIMARY KEY,
    job_identifier VARCHAR(255) NOT NULL,
    view_name      VARCHAR(100) NOT NULL,
    success        BOOLEAN      NOT NULL,
    error_message  TEXT,
    executed_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mv_refresh_log_job ON analytics.materialized_view_refresh_log (job_identifier);