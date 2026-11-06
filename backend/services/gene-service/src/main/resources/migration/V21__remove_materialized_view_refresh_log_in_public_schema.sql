-- ============================================================
-- Migration to microservices Architecture : remove Materialized views logs from public schema.
--
-- ============================================================

-- ── mv_length_histogram ──────────────────────────────────────
DROP TABLE IF EXISTS public.materialized_view_refresh_log;
