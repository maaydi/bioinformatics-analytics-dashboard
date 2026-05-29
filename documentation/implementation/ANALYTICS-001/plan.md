# ANALYTICS-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Create Flyway migration for all six materialized views
3. Implement DTOs for each analytics response
4. Implement `AnalyticsRepository` with native SQL projections
5. Implement `AnalyticsService` (read from repo, validate params)
6. Complete `AnalyticsController` (remove stubs, wire service)
7. Write unit tests for `AnalyticsService`
8. Write integration tests for all endpoints
9. Update documentation

## Status

- [x] Requirements analyzed
- [ ] DB migration created
- [x] DTOs implemented
- [ ] AnalyticsRepository implemented
- [ ] AnalyticsService implemented
- [x] AnalyticsController completed
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Database Migration (`V4__materialized_views.sql`)

- [x] `mv_dashboard_kpis` — aggregate: totalProteins, reviewedCount, unreviewedCount, organismCount, avgLength,
  avgMolecularWeight, minLength, maxLength
- [x] `mv_length_histogram` — 100-AA buckets from 0 to 10,000; columns: bucket, rangeMin, rangeMax, count
- [x] `mv_organism_counts` — group by organism_name, taxid; columns: organismName, taxid, total, reviewedCount,
  unreviewedCount, avgLength; ordered by total DESC
- [x] `mv_reviewed_ratio` — two rows: reviewed TRUE/FALSE with count
- [x] `mv_evidence_distribution` — five rows: evidenceLevel + count
- [x] `mv_keyword_frequency` — group by keyword name; columns: keyword, count; ordered by count DESC
- [ ] `REFRESH MATERIALIZED VIEW CONCURRENTLY` trigger / scheduled job post-import (hook in `ImportService`)
- [ ] Unique indexes on views for CONCURRENTLY refresh support

### Backend — DTOs

- [x] `DashboardKpisDto` — nine fields matching API contract
- [x] `LengthBucketDto` — `{ bucket, rangeMin, rangeMax, count }`
- [x] `OrganismCountDto` — `{ organismName, taxid, total, reviewedCount, unreviewedCount, avgLength }`
- [x] `ReviewedRatioDto` — `{ reviewed, count }`
- [x] `EvidenceLevelDto` — `{ evidenceLevel, label, count }`
- [x] `KeywordFrequencyDto` — `{ keyword, count }`

### Backend — Repository

- [x] `AnalyticsRepository` (or separate repositories per view) with `@Query` native SQL:
  - [x] `findDashboardKpis()` → `DashboardKpisDto`
    - [x] `findLengthHistogram()` → `List<LengthBucketDto>`
    - [x] `findByOrganism(int limit)` → `List<OrganismCountDto>`
    - [x] `findReviewedRatio()` → `List<ReviewedRatioDto>`
    - [x] `findEvidenceLevels()` → `List<EvidenceLevelDto>`
    - [x] `findKeywordFrequency(int limit)` → `List<KeywordFrequencyDto>`

### Backend — Service

- [x] `AnalyticsService.getDashboardKpis()`
- [x] `AnalyticsService.getLengthHistogram()`
- [x] `AnalyticsService.getByOrganism(int limit)` — validate `1 ≤ limit ≤ 200`
- [x] `AnalyticsService.getReviewedRatio()`
- [x] `AnalyticsService.getEvidenceLevels()`
- [x] `AnalyticsService.getKeywordFrequency(int limit)` — validate `1 ≤ limit ≤ 500`

### Backend — Controller

- [x] Remove `UnsupportedOperationException` stubs in `AnalyticsController`
- [x] Wire `AnalyticsService`
- [x] Return `400` on invalid `limit` values
- [x] Return `401` for all endpoints (covered by `SecurityConfig`)

### Tests

- [ ] `AnalyticsServiceTest` — unit (mock repository):
    - [ ] `getByOrganism` with limit > 200 throws validation exception
    - [ ] `getKeywordFrequency` delegates to repository
- [ ] `AnalyticsControllerIntegrationTest` — Testcontainers:
    - [ ] Each GET endpoint returns 200 with correct schema
    - [ ] `GET /api/analytics/by-organism?limit=201` returns 400

### General

- [ ] Views use `CONCURRENTLY` refresh (requires unique index on each view)
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
