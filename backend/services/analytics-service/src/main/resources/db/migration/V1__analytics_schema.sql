-- ─────────────────────────────────────────────────────────────────────────────
-- Analytics Service — Initial Schema
-- Materialized views for Dashboard and Analytics chart endpoints.
--
-- Authoritative DDL source: documentation/domain-model.md §11
-- These views are REFRESHED CONCURRENTLY after each import job.
-- Scope: analytics schema only (isolated from other services)
-- Strategy: REPLICA datasource only (all analytics read go to replica)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE SCHEMA IF NOT EXISTS analytics;

-- ── mv_length_histogram ──────────────────────────────────────
-- Feeds: Protein Length Histogram (Analytics page)
DROP MATERIALIZED VIEW IF EXISTS analytics.mv_length_histogram CASCADE;
CREATE MATERIALIZED VIEW analytics.mv_length_histogram AS
SELECT width_bucket(length, 0, 10000, 100)             AS bucket,
       (width_bucket(length, 0, 10000, 100) - 1) * 100 AS range_min,
       width_bucket(length, 0, 10000, 100) * 100 - 1   AS range_max,
       COUNT(*)                                        AS count
FROM public.protein_entry
GROUP BY bucket
ORDER BY bucket;

CREATE UNIQUE INDEX ON analytics.mv_length_histogram (bucket);

-- ── mv_organism_counts ───────────────────────────────────────
-- Feeds: Proteins by Organism bar chart, top organisms KPI
DROP MATERIALIZED VIEW IF EXISTS analytics.mv_organism_counts CASCADE;
CREATE MATERIALIZED VIEW analytics.mv_organism_counts AS
SELECT organism_name,
       taxid,
       COUNT(*)                                 AS total,
       COUNT(*) FILTER (WHERE reviewed = TRUE)  AS reviewed_count,
       COUNT(*) FILTER (WHERE reviewed = FALSE) AS unreviewed_count,
       ROUND(AVG(length))                       AS avg_length
FROM public.protein_entry
GROUP BY organism_name, taxid;

CREATE UNIQUE INDEX idx_mv_organism_counts_taxid ON analytics.mv_organism_counts (organism_name, taxid);
CREATE INDEX idx_mv_organism_counts_total ON analytics.mv_organism_counts (total DESC);

-- ── mv_reviewed_ratio ────────────────────────────────────────
-- Feeds: Reviewed vs Unreviewed pie chart, Dashboard KPI
DROP MATERIALIZED VIEW IF EXISTS analytics.mv_reviewed_ratio CASCADE;
CREATE MATERIALIZED VIEW analytics.mv_reviewed_ratio AS
SELECT reviewed,
       COUNT(*) AS count
FROM public.protein_entry
GROUP BY reviewed;

CREATE UNIQUE INDEX ON analytics.mv_reviewed_ratio (reviewed);

-- ── mv_evidence_distribution ─────────────────────────────────
-- Feeds: Evidence Level Pie Chart (Dashboard)
DROP MATERIALIZED VIEW IF EXISTS analytics.mv_evidence_distribution CASCADE;

CREATE MATERIALIZED VIEW analytics.mv_evidence_distribution AS
SELECT evidence_level,
       CASE evidence_level
           WHEN 1 THEN 'Protein level'
           WHEN 2 THEN 'Transcript level'
           WHEN 3 THEN 'Homology'
           WHEN 4 THEN 'Predicted'
           WHEN 5 THEN 'Uncertain'
           END  AS label,
       COUNT(*) AS count
FROM public.protein_entry
GROUP BY evidence_level
ORDER BY evidence_level;

CREATE UNIQUE INDEX ON analytics.mv_evidence_distribution (evidence_level);

-- ── mv_keyword_frequency ─────────────────────────────────────
-- Feeds: Keyword Frequency word-cloud / bar chart (Analytics page)
DROP MATERIALIZED VIEW IF EXISTS analytics.mv_keyword_frequency CASCADE;

CREATE MATERIALIZED VIEW analytics.mv_keyword_frequency AS
SELECT k.name               AS keyword,
       COUNT(pk.protein_id) AS count
FROM public.keyword k
         JOIN public.protein_keyword pk ON pk.keyword_id = k.id
GROUP BY k.name
ORDER BY count DESC;

CREATE UNIQUE INDEX ON analytics.mv_keyword_frequency (keyword);
CREATE INDEX ON analytics.mv_keyword_frequency (count DESC);

-- ── mv_dashboard_kpis ────────────────────────────────────────
-- Feeds: all top KPI cards on the Dashboard
DROP MATERIALIZED VIEW IF EXISTS analytics.mv_dashboard_kpis CASCADE;

CREATE MATERIALIZED VIEW analytics.mv_dashboard_kpis AS
SELECT COUNT(*)                                AS total_proteins,
       COUNT(*) FILTER (WHERE reviewed = TRUE) AS reviewed_count,
       COUNT(DISTINCT organism_name)           AS organism_count,
       COUNT(DISTINCT taxid)                   AS taxon_count,
       ROUND(AVG(length))                      AS avg_length,
       ROUND(AVG(molecular_weight))            AS avg_molecular_weight,
       MIN(length)                             AS min_length,
       MAX(length)                             AS max_length
FROM public.protein_entry;
