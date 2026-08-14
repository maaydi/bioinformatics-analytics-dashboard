-- ============================================================
-- V6__protein_varchar_to_text.sql
-- Convert all fixed-length VARCHAR(...) columns to TEXT
-- Non-destructive migration: uses USING <col>::text where needed
-- This keeps existing indexes and constraints intact where possible.
-- ============================================================

-- keyword
ALTER TABLE keyword
    ALTER COLUMN name TYPE TEXT USING name::text;

-- go_term
ALTER TABLE go_term
    ALTER COLUMN go_id TYPE TEXT USING go_id::text;

-- protein_entry
ALTER TABLE protein_entry
    ALTER COLUMN accession TYPE TEXT USING accession::text;
ALTER TABLE protein_entry
    ALTER COLUMN organism_name TYPE TEXT USING organism_name::text;
ALTER TABLE protein_entry
    ALTER COLUMN organism_common_name TYPE TEXT USING organism_common_name::text;
ALTER TABLE protein_entry
    ALTER COLUMN sequence_checksum TYPE TEXT USING sequence_checksum::text;

-- cross_reference
ALTER TABLE cross_reference
    ALTER COLUMN source TYPE TEXT USING source::text;
ALTER TABLE cross_reference
    ALTER COLUMN identifier TYPE TEXT USING identifier::text;
ALTER TABLE cross_reference
    ALTER COLUMN secondary_id TYPE TEXT USING secondary_id::text;
ALTER TABLE cross_reference
    ALTER COLUMN tertiary_info TYPE TEXT USING tertiary_info::text;

-- protein_feature
ALTER TABLE protein_feature
    ALTER COLUMN feature_type TYPE TEXT USING feature_type::text;
ALTER TABLE protein_feature
    ALTER COLUMN feature_id TYPE TEXT USING feature_id::text;

-- protein_comment
ALTER TABLE protein_comment
    ALTER COLUMN comment_type TYPE TEXT USING comment_type::text;

-- protein_publication
ALTER TABLE protein_publication
    ALTER COLUMN pubmed_id TYPE TEXT USING pubmed_id::text;
ALTER TABLE protein_publication
    ALTER COLUMN doi TYPE TEXT USING doi::text;
ALTER TABLE protein_publication
    ALTER COLUMN journal TYPE TEXT USING journal::text;