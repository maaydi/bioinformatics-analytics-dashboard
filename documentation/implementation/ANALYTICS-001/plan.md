# ANALYTICS-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Create Flyway migration for all six materialized views
3. Implement DTOs for each analytics response
4. Implement `AnalyticsRepository` with native SQL projections
5. Implement `AnalyticsService` (read from repo, validate params, compute compare)
6. Complete `AnalyticsController` (remove stubs, wire service)
7. Add `POST /api/analytics/compare` endpoint
8. Write unit tests for `AnalyticsService`
9. Write integration tests for all endpoints
10. Update documentation

## Status

- [ ] Requirements analyzed
- [ ] DB migration created
- [ ] DTOs implemented
- [ ] AnalyticsRepository implemented
- [ ] AnalyticsService implemented
- [ ] AnalyticsController completed
- [ ] Compare endpoint added
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Database Migration (`V4__materialized_views.sql`)

- [ ] `mv_dashboard_kpis` — aggregate: totalProteins, reviewedCount, unreviewedCount, organismCount, avgLength,
  avgMolecularWeight, minLength, maxLength
- [ ] `mv_length_histogram` — 100-AA buckets from 0 to 10,000; columns: bucket, rangeMin, rangeMax, count
- [ ] `mv_organism_counts` — group by organism_name, taxid; columns: organismName, taxid, total, reviewedCount,
  unreviewedCount, avgLength; ordered by total DESC
- [ ] `mv_reviewed_ratio` — two rows: reviewed TRUE/FALSE with count
- [ ] `mv_evidence_distribution` — five rows: evidenceLevel + count
- [ ] `mv_keyword_frequency` — group by keyword name; columns: keyword, count; ordered by count DESC
- [ ] `REFRESH MATERIALIZED VIEW CONCURRENTLY` trigger / scheduled job post-import (hook in `ImportService`)
- [ ] Unique indexes on views for CONCURRENTLY refresh support

### Backend — DTOs

- [ ] `DashboardKpisDto` — nine fields matching API contract
- [ ] `LengthBucketDto` — `{ bucket, rangeMin, rangeMax, count }`
- [ ] `OrganismCountDto` — `{ organismName, taxid, total, reviewedCount, unreviewedCount, avgLength }`
- [ ] `ReviewedRatioDto` — `{ reviewed, count }`
- [ ] `EvidenceLevelDto` — `{ evidenceLevel, label, count }`
- [ ] `KeywordFrequencyDto` — `{ keyword, count }`
- [ ] `CompareRequestDto` — `{ setA: GeneSearchRequest, setB: GeneSearchRequest }`
- [ ] `AnalyticsSubsetDto` —
  `{ count, avgLength, reviewedCount, reviewedRatio, lengthDistribution, evidenceDistribution }`
- [ ] `CompareResponseDto` — `{ a: AnalyticsSubsetDto, b: AnalyticsSubsetDto }`

### Backend — Repository

- [ ] `AnalyticsRepository` (or separate repositories per view) with `@Query` native SQL:
    - [ ] `findDashboardKpis()` → `DashboardKpisDto`
    - [ ] `findLengthHistogram()` → `List<LengthBucketDto>`
    - [ ] `findByOrganism(int limit)` → `List<OrganismCountDto>`
    - [ ] `findReviewedRatio()` → `List<ReviewedRatioDto>`
    - [ ] `findEvidenceLevels()` → `List<EvidenceLevelDto>`
    - [ ] `findKeywordFrequency(int limit)` → `List<KeywordFrequencyDto>`

### Backend — Service

- [ ] `AnalyticsService.getDashboardKpis()`
- [ ] `AnalyticsService.getLengthHistogram()`
- [ ] `AnalyticsService.getByOrganism(int limit)` — validate `1 ≤ limit ≤ 200`
- [ ] `AnalyticsService.getReviewedRatio()`
- [ ] `AnalyticsService.getEvidenceLevels()`
- [ ] `AnalyticsService.getKeywordFrequency(int limit)` — validate `1 ≤ limit ≤ 500`
- [ ] `AnalyticsService.compare(CompareRequestDto)` — run both filter sets through `GeneSpecification`, compute subset
  analytics on-the-fly

### Backend — Controller

- [ ] Remove `UnsupportedOperationException` stubs in `AnalyticsController`
- [ ] Wire `AnalyticsService`
- [ ] Add `POST /api/analytics/compare` mapping
- [ ] Return `400` on invalid `limit` values
- [ ] Return `401` for all endpoints (covered by `SecurityConfig`)

### Tests

- [ ] `AnalyticsServiceTest` — unit (mock repository):
    - [ ] `getByOrganism` with limit > 200 throws validation exception
    - [ ] `getKeywordFrequency` delegates to repository
    - [ ] `compare` returns correct a/b structure
- [ ] `AnalyticsControllerIntegrationTest` — Testcontainers:
    - [ ] Each GET endpoint returns 200 with correct schema
    - [ ] `GET /api/analytics/by-organism?limit=201` returns 400
    - [ ] `POST /api/analytics/compare` returns 200 with a/b subsets

### General

- [ ] Views use `CONCURRENTLY` refresh (requires unique index on each view)
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
