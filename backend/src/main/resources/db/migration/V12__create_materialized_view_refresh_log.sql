CREATE TABLE materialized_view_refresh_log
(
    id             BIGSERIAL PRIMARY KEY,
    job_identifier VARCHAR(255) NOT NULL,
    view_name      VARCHAR(100) NOT NULL,
    success        BOOLEAN      NOT NULL,
    error_message  TEXT,
    executed_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mv_refresh_log_job ON materialized_view_refresh_log (job_identifier);

CREATE UNIQUE INDEX IF NOT EXISTS unq_mv_length_histogram ON mv_length_histogram (id);
CREATE UNIQUE INDEX IF NOT EXISTS unq_mv_organism_counts ON mv_organism_counts (id);
CREATE UNIQUE INDEX IF NOT EXISTS unq_mv_reviewed_ratio ON mv_reviewed_ratio (id);
CREATE UNIQUE INDEX IF NOT EXISTS unq_mv_evidence_distribution ON mv_evidence_distribution (id);
CREATE UNIQUE INDEX IF NOT EXISTS unq_mv_keyword_frequency ON mv_keyword_frequency (id);