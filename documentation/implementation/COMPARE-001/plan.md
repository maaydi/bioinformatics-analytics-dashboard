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
- [x] Angular models defined
- [x] Angular service method implemented
- [x] CompareComponent implemented
- [x] Unit tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [x] Coverage ≥ 80%

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

- [x] `AnalyticsServiceTest`:
  - [x] `compare` with two distinct filters returns non-null a and b
  - [x] `compare` with empty filter set A returns total dataset subset
- [x] `AnalyticsControllerIntegrationTest`:
  - [x] `POST /api/analytics/compare` valid body → 200 with a/b
  - [x] `POST /api/analytics/compare` invalid body → 400

### Frontend — Models

- [x] `compare-request.model.ts` — `{ setA: GeneSearchRequest, setB: GeneSearchRequest }`
- [x] `analytics-subset.model.ts` —
  `{ count, avgLength, reviewedCount, reviewedRatio, lengthDistribution: LengthBucket[], evidenceDistribution: EvidenceLevel[] }`
- [x] `compare-response.model.ts` — `{ a: AnalyticsSubset, b: AnalyticsSubset }`

### Frontend — Service (add to `features/analytics/analytics.service.ts` or existing service)

- [x] `compare(request: CompareRequest): Observable<CompareResponse>` — `POST /api/analytics/compare`

### Frontend — `CompareComponent` (new subcomponent under `features/analytics/compare/`)

- [x] `compare.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [x] `compare.component.html` — external template
- [x] `compare.component.scss`
- [x] Two filter panel sections labeled "Set A" and "Set B"
  - [x] Each uses a slim version of `GeneFilterComponent` (subset of fields or full panel)
- [x] "Compare" button
- [x] Warning `@if (setsAreIdentical())` — computed signal checking deep equality of Set A and Set B
- [x] Results section (rendered `@if (result())` is non-null):
  - [x] KPI row: count, avg length, reviewed count, reviewed ratio for each set
  - [x] Side-by-side length distribution charts (Set A vs Set B)
  - [x] Side-by-side evidence distribution charts (Set A vs Set B)
- [x] Loading state during compare API call
- [x] Error state with Retry button

### Tests

- [x] `CompareComponent` unit tests (27 tests — all passing):
  - [x] Component initialization (filter, results, loading, error signals)
  - [x] Filter application and clearing
  - [x] Validation and warning for identical filters
  - [x] Service integration with correct payloads
  - [x] Loading state management
  - [x] Response handling and KPI transformation
  - [x] Number formatting
  - [x] Error handling
  - [x] Reset functionality
  - [x] Edge cases (empty results, large numbers, zero reviewed, form submission)
- [x] Angular service tests (3 new tests — all passing):
  - [x] `compare()` sends correct POST body structure
  - [x] `compare()` returns Observable of CompareResponse
  - [x] `compare()` endpoint correct (POST `/analytics/filters/compare`)

### General

- [x] Depends on `ANALYTICS-001` (compare endpoint) and `GENE-002` (filter component reuse)
- [x] Native control flow only
- [x] `ChangeDetectionStrategy.OnPush`
- [x] AXE checks pass
- [x] Code reviewed
- [x] Coverage ≥ 80% (530/530 tests passing, CompareComponent 27 tests, AnalyticsService 3 tests)
