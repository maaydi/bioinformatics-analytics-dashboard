# ARCH-002 P0 Baseline Status

Captured 2026-11-10. This folder contains fixture definitions and read-only capture aids; it does not claim measured
production-like results.

## Runtime evidence

The repository's Docker Compose project had only `local-redis` running when checked. PostgreSQL and the application
services were not running. No PERF-001 import file, merge fixture, or API golden file was checked in before this P0
work. The PERF-001 journal contains estimates and targets, not measured actuals. Do not copy its `~120+ min`
estimate into the baseline as a result.

| P0 item | State | Evidence still required |
|---------|-------|------------------------|
| 0.5 Import baseline | BLOCKED | Approved PERF-001 input, running import/catalog/database stack, processed/skipped counts, elapsed time, configured chunk size, and per-table before/after counts. |
| 0.6 Public response goldens | BLOCKED | Running services with a fixed database fixture and captured JSON for the public gene, analytics, import-admin, and saved-filter routes. Existing controller tests mock jobs/services and do not freeze complete response bodies. |
| 0.7 AC-7 search cases | FIXTURE READY | `ac7-search-cases.json` contains 34 request scenarios covering every `GeneSearchRequest` filter, empty-result intent, pagination, and sorting. Expected counts must be recorded after seeding the stable AC-7 dataset. |
| 0.8 KPI/MV baseline | BLOCKED | Database snapshot plus KPI values and timed refreshes of each legacy materialized view. |
| 0.9 Merge fixtures | EXECUTION BLOCKED | `merge-fixtures/` contains light TSV, detailed DAT, and lower-version DAT inputs. The current TSV reader sends each row to a UniProt line processor that requires a complete tagged record, so the TSV input cannot currently be used as an import entry. Resolve the format/processor mismatch before claiming a merge run. |

## Capture procedure

1. Restore the approved database snapshot and record the commit, database version, host/container resources, active
   profiles, and relevant configuration. Record the effective import chunk size; do not infer it from test settings.
2. Record pre-import counts, run the PERF-001 fixture once with the approved strategy, and capture processed,
   skipped, created, updated, unchanged, and stale counts and wall-clock duration.
3. Record post-import counts from `catalog-row-counts.sql`; retain the command output and logs with this folder.
4. Capture response bodies and status codes for each route listed in P0 task 0.6 using the same fixed database state.
   Replace generated IDs/timestamps only through an explicit normalization step and retain both raw and normalized
   files.
5. Record KPI totals and time each legacy `REFRESH MATERIALIZED VIEW CONCURRENTLY` separately. Store the database
   state and timing method with the results.
6. Run the AC-7 fixture requests and save observed counts plus dataset identity. Do not treat the requests alone as
   a parity report.

No authentication credentials or database secrets should be stored in this folder.