-- V11__add_sequences.sql
-- Replaces IDENTITY (serial) columns with explicit sequences on all entity tables
-- so that Hibernate's allocationSize=500 pre-allocation can be used for JDBC batch inserts.

-- ── protein_entry ─────────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS protein_entry_seq
    START WITH 1 INCREMENT BY 500;

SELECT setval('protein_entry_seq', COALESCE((SELECT MAX(id) FROM protein_entry), 0) + 500, false);

ALTER TABLE protein_entry
    ALTER COLUMN id SET DEFAULT nextval('protein_entry_seq');

-- ── cross_reference ───────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS cross_reference_seq
    START WITH 1 INCREMENT BY 500;

SELECT setval('cross_reference_seq', COALESCE((SELECT MAX(id) FROM cross_reference), 0) + 500, false);

ALTER TABLE cross_reference
    ALTER COLUMN id SET DEFAULT nextval('cross_reference_seq');

-- ── protein_feature ───────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS protein_feature_seq
    START WITH 1 INCREMENT BY 500;

SELECT setval('protein_feature_seq', COALESCE((SELECT MAX(id) FROM protein_feature), 0) + 500, false);

ALTER TABLE protein_feature
    ALTER COLUMN id SET DEFAULT nextval('protein_feature_seq');

-- ── protein_comment ───────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS protein_comment_seq
    START WITH 1 INCREMENT BY 500;

SELECT setval('protein_comment_seq', COALESCE((SELECT MAX(id) FROM protein_comment), 0) + 500, false);

ALTER TABLE protein_comment
    ALTER COLUMN id SET DEFAULT nextval('protein_comment_seq');

-- ── protein_publication ───────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS protein_publication_seq
    START WITH 1 INCREMENT BY 500;

SELECT setval('protein_publication_seq', COALESCE((SELECT MAX(id) FROM protein_publication), 0) + 500, false);

ALTER TABLE protein_publication
    ALTER COLUMN id SET DEFAULT nextval('protein_publication_seq');

-- ── host_organism ─────────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS host_organism_seq
    START WITH 1 INCREMENT BY 500;

SELECT setval('host_organism_seq', COALESCE((SELECT MAX(id) FROM host_organism), 0) + 500, false);

ALTER TABLE host_organism
    ALTER COLUMN id SET DEFAULT nextval('host_organism_seq');

-- ── keyword ───────────────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS keyword_seq
    START WITH 1 INCREMENT BY 500;

SELECT setval('keyword_seq', COALESCE((SELECT MAX(id) FROM keyword), 0) + 500, false);

ALTER TABLE keyword
    ALTER COLUMN id SET DEFAULT nextval('keyword_seq');

-- ── go_term ───────────────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS go_term_seq
    START WITH 1 INCREMENT BY 500;

SELECT setval('go_term_seq', COALESCE((SELECT MAX(id) FROM go_term), 0) + 500, false);

ALTER TABLE go_term
    ALTER COLUMN id SET DEFAULT nextval('go_term_seq');

