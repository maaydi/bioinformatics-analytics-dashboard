# COMPARE-001 — Compare-Mode Analytics

## Description

Implement the compare-mode feature on the Analytics page:

- Users define two independent filter sets (Set A and Set B).
- `POST /api/analytics/compare` returns side-by-side analytics subsets for both sets.
- Angular UI renders two side-by-side charts: length distribution and evidence distribution.
- A KPI row shows per subset: count, avg length, reviewed count, reviewed ratio.
- Warning when both filter sets are identical.

## Scope

| Layer            | Artifact                                                                       |
|------------------|--------------------------------------------------------------------------------|
| Backend endpoint | `POST /api/analytics/compare` in `AnalyticsController`                         |
| Backend service  | `AnalyticsService.compare(CompareRequestDto)`                                  |
| Backend DTOs     | `CompareRequestDto`, `CompareResponseDto`, `AnalyticsSubsetDto`                |
| Frontend         | Compare panel in `features/analytics/analytics.component` or new sub-component |
| Frontend model   | `CompareRequest`, `CompareResponse`, `AnalyticsSubset`                         |
| Frontend service | `AnalyticsService.compare()` method                                            |

## Acceptance Criteria

- [ ] `POST /api/analytics/compare` with two distinct filter sets returns `200` with
  `{ a: AnalyticsSubset, b: AnalyticsSubset }`.
- [ ] Each `AnalyticsSubset` contains: `count`, `avgLength`, `reviewedCount`, `reviewedRatio`, `lengthDistribution` (
  buckets), `evidenceDistribution` (5 levels).
- [ ] `POST /api/analytics/compare` with malformed filter body returns `400`.
- [ ] Frontend UI: two filter panels labeled "Set A" and "Set B" — each reuses `GeneFilterComponent` in a slim variant.
- [ ] Frontend: "Compare" button triggers the API call.
- [ ] Frontend: results render two side-by-side length-distribution charts and two evidence-distribution charts.
- [ ] Frontend: KPI row shows count, avg length, reviewed count, reviewed ratio for each set.
- [ ] Warning shown when Set A and Set B filters are identical: "Filter sets are identical — comparison is not
  meaningful."
- [ ] Loading, error, and empty states handled.
- [ ] Unit tests for `AnalyticsService.compare()` and `CompareComponent`.

## References

- `documentation/api-contract.md` §2 — Analytics Endpoints (compare endpoint)
- `documentation/plan.md` — US-13, US-14
