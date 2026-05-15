-- ============================================================
-- V5__gene_indexes.sql
-- Additional indexes for gene search and filtering performance
--
-- Adds:
-- - Trigram extension for fuzzy/proximity searches
-- - Trigram indexes for organism_name and gene_name_primary
-- - B-tree index on updated_date for sorting/pagination
-- - B-tree index on gene_name_primary for exact lookups
--
-- ============================================================

-- Enable PostgreSQL trigram extension for fuzzy text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Trigram index on organism_name for flexible organism filtering
-- Supports: ILIKE, trigram similarity, pattern matching
CREATE INDEX IF NOT EXISTS idx_pe_organism_trgm
    ON protein_entry USING gin (organism_name gin_trgm_ops);

-- Trigram index on gene_name_primary for fuzzy gene lookups
CREATE INDEX IF NOT EXISTS idx_pe_gene_name_trgm
    ON protein_entry USING gin (gene_name_primary gin_trgm_ops);

-- B-tree index on updated_date for chronological sorting and time-range queries
CREATE INDEX IF NOT EXISTS idx_pe_updated_date
    ON protein_entry (updated_date DESC);

-- B-tree index on gene_name_primary for exact gene name lookups
CREATE INDEX IF NOT EXISTS idx_pe_gene_name_primary
    ON protein_entry (gene_name_primary);

