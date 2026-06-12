# COMPARE-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Implement `CompareRequestDto`, `CompareResponseDto`, `AnalyticsSubsetDto` (backend)
3. Implement `AnalyticsService.compare()` method
4. Add `POST /api/analytics/compare` to `AnalyticsController`
5. Define Angular `CompareRequest`, `CompareResponse`, `AnalyticsSubset` models
6. Implement `AnalyticsService.compare()` method (Angular)
7. Implement `CompareComponent` (two filter panels + results)
8. Write unit tests (backend + frontend)
9. Update documentation

## Status

- [x] Requirements analyzed
- [x] Backend DTOs implemented
- [x] AnalyticsService.compare() implemented
- [x] Controller endpoint added
- [ ] Angular models defined
- [ ] Angular service method implemented
- [ ] CompareComponent implemented
- [ ] Unit tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Backend — DTOs

- [x] `CompareRequestDto` — `{ setA: GeneSearchRequest, setB: GeneSearchRequest }` with `@Valid` on both sets
- [x] `AnalyticsSubsetDto` —
  `{ count, avgLength, reviewedCount, reviewedRatio, lengthDistribution: List<LengthBucketDto>, evidenceDistribution: List<EvidenceLevelDto> }`
- [x] `CompareResponseDto` — `{ a: AnalyticsSubsetDto, b: AnalyticsSubsetDto }`

### Backend — Service

- [x] `AnalyticsService.compare(CompareRequestDto request)`:
  - [x] Build `Specification` for Set A and Set B using `GeneSpecification`
  - [x] For each set: count rows, compute avg length, reviewed count, reviewed ratio
  - [x] For each set: compute length distribution (re-use `mv_length_histogram` bucket boundaries, query against
      filtered set)
  - [x] For each set: compute evidence distribution (group-by query on filtered set)
  - [x] Return `CompareResponseDto`

### Backend — Controller

- [x] `@PostMapping("/compare")` in `AnalyticsController`
- [x] Delegates to `AnalyticsService.compare()`
- [x] Returns `200 OK` with `CompareResponseDto`
- [x] Returns `400` on `@Valid` failure
- [x] Returns `401` without JWT

### Backend — Tests

- [ ] `AnalyticsServiceTest`:
    - [ ] `compare` with two distinct filters returns non-null a and b
    - [ ] `compare` with empty filter set A returns total dataset subset
- [ ] `AnalyticsControllerIntegrationTest`:
    - [ ] `POST /api/analytics/compare` valid body → 200 with a/b
    - [ ] `POST /api/analytics/compare` invalid body → 400

### Frontend — Models

- [ ] `compare-request.model.ts` — `{ setA: GeneSearchRequest, setB: GeneSearchRequest }`
- [ ] `analytics-subset.model.ts` —
  `{ count, avgLength, reviewedCount, reviewedRatio, lengthDistribution: LengthBucket[], evidenceDistribution: EvidenceLevel[] }`
- [ ] `compare-response.model.ts` — `{ a: AnalyticsSubset, b: AnalyticsSubset }`

### Frontend — Service (add to `features/analytics/analytics.service.ts` or existing service)

- [ ] `compare(request: CompareRequest): Observable<CompareResponse>` — `POST /api/analytics/compare`

### Frontend — `CompareComponent` (new sub-component under `features/analytics/compare/`)

- [ ] `compare.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `compare.component.html` — external template
- [ ] `compare.component.scss`
- [ ] Two filter panel sections labeled "Set A" and "Set B"
    - [ ] Each uses a slim version of `GeneFilterComponent` (subset of fields or full panel)
- [ ] "Compare" button
- [ ] Warning `@if (setsAreIdentical())` — computed signal checking deep equality of Set A and Set B
- [ ] Results section (rendered `@if (result())` is non-null):
    - [ ] KPI row: count, avg length, reviewed count, reviewed ratio for each set
    - [ ] Side-by-side length distribution charts (Set A vs Set B)
    - [ ] Side-by-side evidence distribution charts (Set A vs Set B)
- [ ] Loading state during compare API call
- [ ] Error state with Retry button

### Tests

- [ ] `CompareComponent` unit tests:
    - [ ] Shows warning when Set A = Set B
    - [ ] Triggers service call on Compare click
    - [ ] Renders KPI row values after successful response
    - [ ] Shows error state on service failure
- [ ] Angular service test:
    - [ ] `compare()` sends correct POST body

### General

- [ ] Depends on `ANALYTICS-001` (compare endpoint) and `GENE-002` (filter component reuse)
- [ ] Native control flow only
- [ ] `ChangeDetectionStrategy.OnPush`
- [ ] AXE checks pass
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
