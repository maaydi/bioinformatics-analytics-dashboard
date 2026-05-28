# ANALYTICS-001 — Analytics Endpoints & Materialized Views

## Description

Implement the full analytics backend powered by PostgreSQL materialized views:

- `GET /api/analytics/dashboard-kpis` — `mv_dashboard_kpis`
- `GET /api/analytics/length-histogram` — `mv_length_histogram`
- `GET /api/analytics/by-organism` — `mv_organism_counts` (top N, default 50)
- `GET /api/analytics/reviewed-ratio` — `mv_reviewed_ratio`
- `GET /api/analytics/evidence-levels` — `mv_evidence_distribution`
- `GET /api/analytics/keyword-frequency` — `mv_keyword_frequency` (top N, default 100)

All materialized-view endpoints must respond in ≤ 500 ms (NFR §12.1).

## Scope

| Layer        | Artifact                                                                                                                 |
|--------------|--------------------------------------------------------------------------------------------------------------------------|
| DB migration | `V4__materialized_views.sql` — DDL for all six materialized views + refresh policy                                       |
| Repository   | Native SQL `@Query` projections reading from materialized views                                                          |
| DTOs         | `DashboardKpisDto`, `LengthBucketDto`, `OrganismCountDto`, `ReviewedRatioDto`, `EvidenceLevelDto`, `KeywordFrequencyDto` |
| Service      | `AnalyticsService` — delegates to repositories, validates query params                                                   |
| Controller   | `AnalyticsController` — remove stubs, wire service                                                                       |

## Acceptance Criteria

- [ ] `GET /api/analytics/dashboard-kpis` returns `200` with `DashboardKpisDto` (all nine fields present).
- [ ] `GET /api/analytics/length-histogram` returns a non-empty array of bucket objects.
- [ ] `GET /api/analytics/by-organism?limit=10` returns at most 10 organisms.
- [ ] `GET /api/analytics/by-organism?limit=201` returns `400` (limit exceeds 200 cap).
- [ ] `GET /api/analytics/reviewed-ratio` returns exactly two items (reviewed true/false).
- [ ] `GET /api/analytics/evidence-levels` returns exactly five items with labels.
- [ ] `GET /api/analytics/keyword-frequency?limit=20` returns at most 20 keywords.
- [ ] All endpoints return `401` without JWT.
- [ ] Response time ≤ 500 ms against a populated database (verified in integration test).
- [ ] Unit tests for `AnalyticsService`.
- [ ] Integration tests for all six GET endpoints.

## References

- `documentation/api-contract.md` §2 — Analytics Endpoints
- `documentation/domain-model.md` — materialized view DDL
- `documentation/plan.md` — US-11, US-12, US-13, US-14, US-32, US-33, US-34, US-35
