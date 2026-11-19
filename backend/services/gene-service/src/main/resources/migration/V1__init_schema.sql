-- ============================================================
-- V1__init_schema.sql
-- Fresh catalog baseline. Other services own their tables and Flyway histories.
--
-- Authoritative DDL source: documentation/domain-model.md
-- DO NOT modify column types or names without updating domain-model.md.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE SEQUENCE protein_entry_seq INCREMENT BY 500;
CREATE SEQUENCE cross_reference_seq INCREMENT BY 500;
CREATE SEQUENCE protein_feature_seq INCREMENT BY 500;
CREATE SEQUENCE protein_comment_seq INCREMENT BY 500;
CREATE SEQUENCE protein_publication_seq INCREMENT BY 500;
CREATE SEQUENCE host_organism_seq INCREMENT BY 500;
CREATE SEQUENCE keyword_seq INCREMENT BY 500;
CREATE SEQUENCE go_term_seq INCREMENT BY 500;

-- ── keyword ──────────────────────────────────────────────────
CREATE TABLE keyword (
    id   INTEGER PRIMARY KEY DEFAULT nextval('keyword_seq'),
    name TEXT NOT NULL UNIQUE
);

CREATE UNIQUE INDEX idx_kw_name ON keyword (name);

-- ── go_term ──────────────────────────────────────────────────
CREATE TABLE go_term (
    id          INTEGER PRIMARY KEY DEFAULT nextval('go_term_seq'),
    go_id       TEXT         NOT NULL UNIQUE,
    aspect      CHAR(1)      NOT NULL
                CHECK (aspect IN ('P','F','C')),
    description TEXT         NOT NULL
);

CREATE UNIQUE INDEX idx_go_goid   ON go_term (go_id);
CREATE        INDEX idx_go_aspect ON go_term (aspect);

-- ── protein_entry ───────────────────────────────────────────
CREATE TABLE protein_entry (
    id                   BIGINT PRIMARY KEY DEFAULT nextval('protein_entry_seq'),

    accession            TEXT         NOT NULL UNIQUE,
    entry_name           TEXT         NOT NULL,
    reviewed             BOOLEAN      NOT NULL DEFAULT FALSE,

    integrated_date      DATE,
    sequence_date        DATE,
    updated_date         DATE,
    sequence_version     SMALLINT,
    entry_version        SMALLINT,

    protein_full_name    TEXT,
    protein_short_name   TEXT,
    protein_ec_number    TEXT,

    gene_name_primary    TEXT,
    gene_name_synonyms   TEXT[],
    gene_orf_names       TEXT[],
    gene_ordered_locus   TEXT[],

    organism_name        TEXT         NOT NULL,
    organism_common_name TEXT,
    taxid                INTEGER      NOT NULL,
    lineage              TEXT[],

    length               INTEGER      NOT NULL,
    molecular_weight     INTEGER,
    sequence_checksum    TEXT,
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

CREATE INDEX idx_pe_organism_trgm ON protein_entry USING GIN (organism_name gin_trgm_ops);
CREATE INDEX idx_pe_gene_name_trgm ON protein_entry USING GIN (gene_name_primary gin_trgm_ops);
CREATE INDEX idx_pe_updated_date ON protein_entry (updated_date DESC);
CREATE INDEX idx_pe_gene_name_primary ON protein_entry (gene_name_primary);

CREATE FUNCTION fts_match(document tsvector, query_text text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
AS $$
SELECT document @@ websearch_to_tsquery('english', query_text);
$$;

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
    id             BIGINT PRIMARY KEY DEFAULT nextval('cross_reference_seq'),
    protein_id     BIGINT       NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    source         TEXT         NOT NULL,
    identifier     TEXT         NOT NULL,
    secondary_id   TEXT,
    tertiary_info  TEXT
);

CREATE INDEX idx_xref_protein    ON cross_reference (protein_id);
CREATE INDEX idx_xref_source     ON cross_reference (source);
CREATE INDEX idx_xref_source_id  ON cross_reference (source, identifier);

-- ── protein_feature ──────────────────────────────────────────
CREATE TABLE protein_feature (
    id           BIGINT PRIMARY KEY DEFAULT nextval('protein_feature_seq'),
    protein_id   BIGINT       NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    feature_type TEXT          NOT NULL,
    start_pos    INTEGER,
    end_pos      INTEGER,
    note         TEXT,
    feature_id   TEXT,
    evidence     TEXT
);

CREATE INDEX idx_ft_protein      ON protein_feature (protein_id);
CREATE INDEX idx_ft_type         ON protein_feature (feature_type);
CREATE INDEX idx_ft_type_protein ON protein_feature (feature_type, protein_id);

-- ── host_organism ────────────────────────────────────────────
CREATE TABLE host_organism (
    id         INTEGER PRIMARY KEY DEFAULT nextval('host_organism_seq'),
    protein_id BIGINT  NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    taxid      INTEGER NOT NULL,
    name       TEXT    NOT NULL
);

CREATE INDEX idx_ho_protein ON host_organism (protein_id);
CREATE INDEX idx_ho_taxid   ON host_organism (taxid);

-- ── protein_comment ──────────────────────────────────────────
CREATE TABLE protein_comment (
    id           BIGINT PRIMARY KEY DEFAULT nextval('protein_comment_seq'),
    protein_id   BIGINT      NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    comment_type TEXT        NOT NULL,
    text         TEXT        NOT NULL
);

CREATE INDEX idx_cc_protein ON protein_comment (protein_id);
CREATE INDEX idx_cc_type    ON protein_comment (comment_type);

-- ── protein_publication ──────────────────────────────────────
CREATE TABLE protein_publication (
    id         BIGINT PRIMARY KEY DEFAULT nextval('protein_publication_seq'),
    protein_id BIGINT       NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    ref_number SMALLINT,
    pubmed_id  TEXT,
    doi        TEXT,
    authors    TEXT,
    title      TEXT,
    journal    TEXT
);

CREATE INDEX idx_pub_protein ON protein_publication (protein_id);
CREATE INDEX idx_pub_pubmed  ON protein_publication (pubmed_id);

-- ── saved_filter ─────────────────────────────────────────────
CREATE TABLE saved_filter (
    id          BIGSERIAL    PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    filter_json JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_saved_filter_user_name UNIQUE (username, name)
);

CREATE INDEX idx_sf_user ON saved_filter (username);
