-- ============================================================
-- V1__init_schema.sql
-- Initial schema creation for Bioinformatics Analytics Dashboard
--
-- Authoritative DDL source: documentation/domain-model.md
-- DO NOT modify column types or names without updating domain-model.md.
-- ============================================================

-- ── keyword ──────────────────────────────────────────────────
CREATE TABLE keyword (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE UNIQUE INDEX idx_kw_name ON keyword (name);

-- ── go_term ──────────────────────────────────────────────────
CREATE TABLE go_term (
    id          SERIAL PRIMARY KEY,
    go_id       VARCHAR(15)  NOT NULL UNIQUE,
    aspect      CHAR(1)      NOT NULL
                CHECK (aspect IN ('P','F','C')),
    description TEXT         NOT NULL
);

CREATE UNIQUE INDEX idx_go_goid   ON go_term (go_id);
CREATE        INDEX idx_go_aspect ON go_term (aspect);

-- ── protein_entry ───────────────────────────────────────────
CREATE TABLE protein_entry (
    id                   BIGSERIAL PRIMARY KEY,

    accession            VARCHAR(20)  NOT NULL UNIQUE,
    entry_name           VARCHAR(50)  NOT NULL,
    reviewed             BOOLEAN      NOT NULL DEFAULT FALSE,

    integrated_date      DATE,
    sequence_date        DATE,
    updated_date         DATE,
    sequence_version     SMALLINT,
    entry_version        SMALLINT,

    protein_full_name    TEXT,
    protein_short_name   VARCHAR(200),
    protein_ec_number    VARCHAR(50),

    gene_name_primary    VARCHAR(100),
    gene_name_synonyms   TEXT[],
    gene_orf_names       TEXT[],
    gene_ordered_locus   TEXT[],

    organism_name        VARCHAR(300) NOT NULL,
    organism_common_name VARCHAR(150),
    taxid                INTEGER      NOT NULL,
    lineage              TEXT[],

    length               INTEGER      NOT NULL,
    molecular_weight     INTEGER,
    sequence_checksum    VARCHAR(20),
    sequence             TEXT,

    evidence_level       SMALLINT     NOT NULL CHECK (evidence_level BETWEEN 1 AND 5),

    search_vector        TSVECTOR,

    metadata_jsonb       JSONB,

    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_pe_accession      ON protein_entry (accession);
CREATE        INDEX idx_pe_entry_name     ON protein_entry (entry_name);
CREATE        INDEX idx_pe_organism_name  ON protein_entry (organism_name);
CREATE        INDEX idx_pe_taxid          ON protein_entry (taxid);
CREATE        INDEX idx_pe_reviewed       ON protein_entry (reviewed);
CREATE        INDEX idx_pe_evidence       ON protein_entry (evidence_level);
CREATE        INDEX idx_pe_length         ON protein_entry (length);
CREATE        INDEX idx_pe_mw             ON protein_entry (molecular_weight);
CREATE        INDEX idx_pe_reviewed_org   ON protein_entry (reviewed, organism_name);
CREATE        INDEX idx_pe_reviewed_ev    ON protein_entry (reviewed, evidence_level);
CREATE        INDEX idx_pe_synonyms       ON protein_entry USING GIN (gene_name_synonyms);
CREATE        INDEX idx_pe_orf            ON protein_entry USING GIN (gene_orf_names);
CREATE        INDEX idx_pe_lineage        ON protein_entry USING GIN (lineage);
CREATE        INDEX idx_pe_fts            ON protein_entry USING GIN (search_vector);
CREATE        INDEX idx_pe_metadata       ON protein_entry USING GIN (metadata_jsonb);

CREATE OR REPLACE FUNCTION trg_protein_entry_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.accession,         '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.entry_name,        '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.gene_name_primary, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.protein_full_name, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(NEW.organism_name,     '')), 'D');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_pe_search_vector
BEFORE INSERT OR UPDATE ON protein_entry
FOR EACH ROW EXECUTE FUNCTION trg_protein_entry_search_vector();

-- ── protein_keyword ──────────────────────────────────────────
CREATE TABLE protein_keyword (
    protein_id  BIGINT  NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    keyword_id  INTEGER NOT NULL REFERENCES keyword (id)       ON DELETE CASCADE,
    PRIMARY KEY (protein_id, keyword_id)
);

CREATE INDEX idx_pk_keyword ON protein_keyword (keyword_id);

-- ── protein_go_term ──────────────────────────────────────────
CREATE TABLE protein_go_term (
    protein_id    BIGINT  NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    go_term_id    INTEGER NOT NULL REFERENCES go_term (id)       ON DELETE CASCADE,
    evidence_code VARCHAR(10),
    PRIMARY KEY (protein_id, go_term_id)
);

CREATE INDEX idx_pgt_go     ON protein_go_term (go_term_id);
CREATE INDEX idx_pgt_aspect ON protein_go_term (go_term_id) INCLUDE (protein_id);

-- ── cross_reference ──────────────────────────────────────────
CREATE TABLE cross_reference (
    id             BIGSERIAL PRIMARY KEY,
    protein_id     BIGINT       NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    source         VARCHAR(30)  NOT NULL,
    identifier     VARCHAR(100) NOT NULL,
    secondary_id   VARCHAR(100),
    tertiary_info  VARCHAR(200)
);

CREATE INDEX idx_xref_protein    ON cross_reference (protein_id);
CREATE INDEX idx_xref_source     ON cross_reference (source);
CREATE INDEX idx_xref_source_id  ON cross_reference (source, identifier);

-- ── protein_feature ──────────────────────────────────────────
CREATE TABLE protein_feature (
    id           BIGSERIAL PRIMARY KEY,
    protein_id   BIGINT       NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    feature_type VARCHAR(30)  NOT NULL,
    start_pos    INTEGER,
    end_pos      INTEGER,
    note         TEXT,
    feature_id   VARCHAR(20)
);

CREATE INDEX idx_ft_protein      ON protein_feature (protein_id);
CREATE INDEX idx_ft_type         ON protein_feature (feature_type);
CREATE INDEX idx_ft_type_protein ON protein_feature (feature_type, protein_id);

-- ── host_organism ────────────────────────────────────────────
CREATE TABLE host_organism (
    id         SERIAL  PRIMARY KEY,
    protein_id BIGINT  NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    taxid      INTEGER NOT NULL,
    name       TEXT    NOT NULL
);

CREATE INDEX idx_ho_protein ON host_organism (protein_id);
CREATE INDEX idx_ho_taxid   ON host_organism (taxid);

-- ── protein_comment ──────────────────────────────────────────
CREATE TABLE protein_comment (
    id           BIGSERIAL PRIMARY KEY,
    protein_id   BIGINT      NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    comment_type VARCHAR(50) NOT NULL,
    text         TEXT        NOT NULL
);

CREATE INDEX idx_cc_protein ON protein_comment (protein_id);
CREATE INDEX idx_cc_type    ON protein_comment (comment_type);

-- ── protein_publication ──────────────────────────────────────
CREATE TABLE protein_publication (
    id         BIGSERIAL PRIMARY KEY,
    protein_id BIGINT       NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    ref_number SMALLINT,
    pubmed_id  VARCHAR(20),
    doi        VARCHAR(200),
    authors    TEXT,
    title      TEXT,
    journal    VARCHAR(300)
);

CREATE INDEX idx_pub_protein ON protein_publication (protein_id);
CREATE INDEX idx_pub_pubmed  ON protein_publication (pubmed_id);

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
