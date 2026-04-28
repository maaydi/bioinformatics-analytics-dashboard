-- ============================================================
-- V1__init_schema.sql
-- Initial schema creation for Bioinformatics Analytics Dashboard
--
-- Authoritative DDL source: documentation/domain-model.md
-- DO NOT modify column types or names without updating domain-model.md.
-- ============================================================

-- ── protein_entry ───────────────────────────────────────────
-- Full DDL: documentation/domain-model.md §1
-- TODO: paste DDL from domain-model.md §1 here

-- ── keyword ──────────────────────────────────────────────────
-- Full DDL: documentation/domain-model.md §2
-- TODO: paste DDL from domain-model.md §2 here

-- ── protein_keyword ──────────────────────────────────────────
-- Full DDL: documentation/domain-model.md §3
-- TODO: paste DDL from domain-model.md §3 here

-- ── go_term ──────────────────────────────────────────────────
-- Full DDL: documentation/domain-model.md §4
-- TODO: paste DDL from domain-model.md §4 here

-- ── protein_go_term ──────────────────────────────────────────
-- Full DDL: documentation/domain-model.md §5
-- TODO: paste DDL from domain-model.md §5 here

-- ── cross_reference ──────────────────────────────────────────
-- Full DDL: documentation/domain-model.md §6
-- TODO: paste DDL from domain-model.md §6 here

-- ── protein_feature ──────────────────────────────────────────
-- Full DDL: documentation/domain-model.md §7
-- TODO: paste DDL from domain-model.md §7 here

-- ── host_organism ────────────────────────────────────────────
-- Full DDL: documentation/domain-model.md §8
-- TODO: paste DDL from domain-model.md §8 here

-- ── protein_comment ──────────────────────────────────────────
-- Full DDL: documentation/domain-model.md §9
-- TODO: paste DDL from domain-model.md §9 here

-- ── protein_publication ──────────────────────────────────────
-- Full DDL: documentation/domain-model.md §10
-- TODO: paste DDL from domain-model.md §10 here

-- ── app_user ─────────────────────────────────────────────────
CREATE TABLE app_user (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER'
                CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── saved_filter ─────────────────────────────────────────────
CREATE TABLE saved_filter (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    filter_json JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, name)
);

CREATE INDEX idx_sf_user ON saved_filter (user_id);

-- ── import_job ───────────────────────────────────────────────
CREATE TABLE import_job (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    status             VARCHAR(20)  NOT NULL DEFAULT 'RUNNING'
                        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    file_name          VARCHAR(255),
    strategy           VARCHAR(20)  NOT NULL DEFAULT 'OVERWRITE',
    entry_count        INTEGER,
    records_processed  INTEGER      NOT NULL DEFAULT 0,
    total_estimated    INTEGER,
    duration_ms        BIGINT,
    error_message      TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at       TIMESTAMPTZ
);

CREATE INDEX idx_ij_status ON import_job (status);
CREATE INDEX idx_ij_created ON import_job (created_at DESC);
