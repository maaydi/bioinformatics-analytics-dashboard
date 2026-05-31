# DASH-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Verify / install chart library dependency (ECharts or ApexCharts)
3. Define Angular models for analytics responses
4. Implement `DashboardService` (aggregate all analytics calls)
5. Implement `DashboardComponent` — KPI cards + chart shell
6. Implement length histogram chart
7. Implement reviewed ratio donut chart
8. Implement evidence levels chart with click-to-filter
9. Implement top organisms chart
10. Connect chart click events to `filtersStore` + router navigation
11. Write unit tests
12. Update documentation

## Status

- [x] Requirements analyzed
- [x] Chart library verified
- [x] Angular models defined
- [x] DashboardService implemented
- [x] DashboardComponent shell implemented
- [x] Length histogram chart implemented
- [x] Reviewed ratio donut implemented
- [x] Evidence levels chart implemented
- [x] Top organisms chart implemented
- [x] Chart click-to-filter wired
- [x] Unit tests written
- [x] Documentation updated
- [x] Code reviewed
- [x] Coverage ≥ 80%

---

## Detailed Checklist

### Dependencies

- [x] Verify chart library in `package.json` (prefer `ngx-echarts` + `echarts`)
- [ ] Import chart module in component (standalone import) — not used in current implementation (custom HTML/CSS charts)

### Angular Models (`core/models/` or `features/dashboard/models/`)

- [x] `dashboard-kpis.model.ts` equivalent defined in `core/models/analytics.model.ts` —
  `{ totalProteins, reviewedCount, unreviewedCount, organismCount, taxonCount, avgLength, avgMolecularWeight, minLength, maxLength }`
- [x] `length-bucket.model.ts` equivalent defined in `core/models/analytics.model.ts` —
  `{ bucket, rangeMin, rangeMax, count }`
- [x] `organism-count.model.ts` equivalent defined in `core/models/analytics.model.ts` —
  `{ organismName, taxid, total, reviewedCount, unreviewedCount, avgLength }`
- [x] `reviewed-ratio.model.ts` equivalent defined in `core/models/analytics.model.ts` — `{ reviewed, count }`
- [x] `evidence-level.model.ts` equivalent defined in `core/models/analytics.model.ts` —
  `{ evidenceLevel, label, count }`
- [x] `keyword-frequency.model.ts` equivalent defined in `core/models/analytics.model.ts` — `{ keyword, count }`

### Service (`features/dashboard/dashboard.service.ts`)

- [x] `getDashboardKpis(): Observable<DashboardKpis>`
- [x] `getLengthHistogram(): Observable<LengthBucket[]>`
- [x] `getByOrganism(limit?: number): Observable<OrganismCount[]>`
- [x] `getReviewedRatio(): Observable<ReviewedRatio[]>`
- [x] `getEvidenceLevels(): Observable<EvidenceLevel[]>`
- [x] `getKeywordFrequency(limit?: number): Observable<KeywordFrequency[]>`
- [x] Parallel loading achieved via independent chart components (each requests its own endpoint on init)
- [x] Inject `HttpClient` via `inject()`

### `DashboardComponent` (`features/dashboard/`)

- [x] `dashboard.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [x] `dashboard.component.html` — external template
- [x] `dashboard.component.scss`
- [x] KPI state signals implemented (`kpiCards`, `kpiLoading`, `kpiError`)
- [x] Signal-based loading/error handling implemented
- [x] On init: analytics requests run in parallel through chart component initialization
- [ ] KPI cards section:
  - [x] Total Proteins card
  - [x] Reviewed Count card
  - [x] Distinct Organisms card
  - [x] Average Length card
  - [x] "Last updated" footer omitted (timestamp not exposed by current API)
- [ ] Charts section:
  - [x] Length histogram (bar chart)
  - [x] Reviewed ratio (donut chart)
  - [x] Evidence levels (horizontal bar)
  - [x] Top 10 organisms (horizontal bar chart)
- [x] Chart click handler: `onEvidenceLevelClick(level: number)` → update `filtersStore`, navigate to `/genes`
- [x] Loading state implemented
- [x] Error state implemented
- [x] Empty state implemented
- [x] `@if` / `@for` only (no `*ngIf` / `*ngFor`)

### Tests

- [x] `DashboardComponent` unit tests:
  - [x] Shows loading state during fetch
  - [x] Shows KPI values after successful fetch
  - [x] Shows error state on API failure
    - [x] Retry button triggers re-fetch
    - [x] Evidence level click updates filter store and navigates
- [x] `DashboardService` unit tests (HttpClientTestingModule):
  - [x] Each method sends correct GET request
    - [ ] `forkJoin` used — verify via parallel request expectation

### General

- [x] Native control flow only
- [x] `ChangeDetectionStrategy.OnPush`
- [ ] AXE checks pass
- [x] Code reviewed
- [x] Coverage ≥ 80% (dashboard scope: 97.60% statements, 93.38% branches, 100% functions, 97.26% lines)
