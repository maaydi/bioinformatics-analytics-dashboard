-- ============================================================
-- V20__remove_materialized_views_in_public_schema.sql
-- Migration to microservices Architecture : remove Materialized views from public schema.
--
-- ============================================================

-- ── mv_length_histogram ──────────────────────────────────────
DROP MATERIALIZED VIEW IF EXISTS mv_length_histogram CASCADE;

-- ── mv_organism_counts ───────────────────────────────────────
DROP MATERIALIZED VIEW IF EXISTS mv_organism_counts CASCADE;


-- ── mv_reviewed_ratio ────────────────────────────────────────
DROP MATERIALIZED VIEW IF EXISTS mv_reviewed_ratio CASCADE;


-- ── mv_evidence_distribution ─────────────────────────────────
DROP MATERIALIZED VIEW IF EXISTS mv_evidence_distribution CASCADE;


-- ── mv_keyword_frequency ─────────────────────────────────────
DROP MATERIALIZED VIEW IF EXISTS mv_keyword_frequency CASCADE;


-- ── mv_dashboard_kpis ────────────────────────────────────────
DROP MATERIALIZED VIEW IF EXISTS mv_dashboard_kpis CASCADE;
