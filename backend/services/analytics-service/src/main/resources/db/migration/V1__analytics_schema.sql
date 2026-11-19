-- ─────────────────────────────────────────────────────────────────────────────
-- Analytics Service — Initial Schema
-- Materialized views for Dashboard and Analytics chart endpoints.
--
-- Authoritative DDL source: documentation/domain-model.md §11
-- Fresh install: run after gene-service V1 on shared PostgreSQL.
-- These views read the public catalog until the API-backed refresh refactor.
-- View creation/refresh uses the primary; read-only queries may use the replica.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE SCHEMA IF NOT EXISTS analytics;

-- ── mv_length_histogram ──────────────────────────────────────
-- Feeds: Protein Length Histogram (Analytics page)
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
CREATE MATERIALIZED VIEW analytics.mv_reviewed_ratio AS
SELECT reviewed,
       COUNT(*) AS count
FROM public.protein_entry
GROUP BY reviewed;

CREATE UNIQUE INDEX ON analytics.mv_reviewed_ratio (reviewed);

-- ── mv_evidence_distribution ─────────────────────────────────
-- Feeds: Evidence Level Pie Chart (Dashboard)

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

CREATE TABLE analytics.materialized_view_refresh_log
(
    id             BIGSERIAL PRIMARY KEY,
    job_identifier VARCHAR(255) NOT NULL,
    view_name      VARCHAR(100) NOT NULL,
    success        BOOLEAN      NOT NULL,
    error_message  TEXT,
    executed_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mv_refresh_log_job ON analytics.materialized_view_refresh_log (job_identifier);
