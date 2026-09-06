-- ============================================================================
-- Export Pipeline Tables
-- ============================================================================

-- ──────────────────────────────────────────────────────────────────────────
-- export_pipeline — Represents a saved export configuration and its status
-- ──────────────────────────────────────────────────────────────────────────
CREATE TABLE export_pipeline
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          VARCHAR(255) NOT NULL, -- Username from auth service
    name             VARCHAR(200) NOT NULL,
    description      VARCHAR(500),
    filter_json      JSONB        NOT NULL, -- Serialized GeneSearchRequest
    format           VARCHAR(10)  NOT NULL CHECK (format IN ('CSV', 'TSV', 'JSON', 'EXCEL')),
    field_schema     JSONB        NOT NULL, -- Ordered list of selected field names
    status           VARCHAR(20)  NOT NULL DEFAULT 'QUEUED' CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    estimated_rows   BIGINT,
    actual_rows      BIGINT,
    file_path        VARCHAR(500),
    file_size_bytes  BIGINT,
    error_message    TEXT,
    job_execution_id BIGINT,                -- References BATCH_JOB_EXECUTION
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    deleted_at       TIMESTAMPTZ,
    duration_ms      BIGINT
);

-- Indexes for common query patterns
CREATE INDEX idx_export_pipeline_user ON export_pipeline (user_id, created_at DESC);
CREATE INDEX idx_export_pipeline_status ON export_pipeline (status);
CREATE INDEX idx_export_pipeline_deleted ON export_pipeline (deleted_at) WHERE deleted_at IS NULL;


-- ──────────────────────────────────────────────────────────────────────────
-- export_job_execution — Denormalized tracking of batch job progress
-- ──────────────────────────────────────────────────────────────────────────
CREATE TABLE export_job_execution
(
    id               BIGSERIAL PRIMARY KEY,
    pipeline_id      BIGINT      NOT NULL REFERENCES export_pipeline (id) ON DELETE CASCADE,
    job_execution_id BIGINT      NOT NULL UNIQUE,
    chunks_total     INTEGER,
    chunks_processed INTEGER              DEFAULT 0,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for fast pipeline → job execution lookups
CREATE INDEX idx_export_job_execution_pipeline ON export_job_execution (pipeline_id);

