-- ─────────────────────────────────────────────────────────────────────────────
-- Import Service — Initial Schema
-- ─────────────────────────────────────────────────────────────────────────────

CREATE SCHEMA IF NOT EXISTS import_batch;

-- ── import_job ───────────────────────────────────────────────
CREATE TABLE import_batch.import_job
(
    id                UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    status            VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    file_name         VARCHAR(255),
    strategy          VARCHAR(20) NOT NULL DEFAULT 'OVERWRITE',
    entry_count       INTEGER,
    records_processed INTEGER     NOT NULL DEFAULT 0,
    total_estimated   INTEGER,
    duration_ms       BIGINT,
    error_message     TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMPTZ
);

CREATE INDEX idx_ij_status ON import_batch.import_job (status);
CREATE INDEX idx_ij_created ON import_batch.import_job (created_at DESC);