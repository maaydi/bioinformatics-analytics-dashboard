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
- [ ] Length histogram chart implemented
- [ ] Reviewed ratio donut implemented
- [ ] Evidence levels chart implemented
- [ ] Top organisms chart implemented
- [ ] Chart click-to-filter wired
- [ ] Unit tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Dependencies

- [x] Verify chart library in `package.json` (prefer `ngx-echarts` + `echarts`)
- [ ] Import chart module in component (standalone import)

### Angular Models (`core/models/` or `features/dashboard/models/`)

- [ ] `dashboard-kpis.model.ts` —
  `{ totalProteins, reviewedCount, unreviewedCount, organismCount, taxonCount, avgLength, avgMolecularWeight, minLength, maxLength }`
- [ ] `length-bucket.model.ts` — `{ bucket, rangeMin, rangeMax, count }`
- [ ] `organism-count.model.ts` — `{ organismName, taxid, total, reviewedCount, unreviewedCount, avgLength }`
- [ ] `reviewed-ratio.model.ts` — `{ reviewed, count }`
- [ ] `evidence-level.model.ts` — `{ evidenceLevel, label, count }`
- [ ] `keyword-frequency.model.ts` — `{ keyword, count }`

### Service (`features/dashboard/dashboard.service.ts`)

- [ ] `getDashboardKpis(): Observable<DashboardKpis>`
- [ ] `getLengthHistogram(): Observable<LengthBucket[]>`
- [ ] `getByOrganism(limit?: number): Observable<OrganismCount[]>`
- [ ] `getReviewedRatio(): Observable<ReviewedRatio[]>`
- [ ] `getEvidenceLevels(): Observable<EvidenceLevel[]>`
- [ ] `getKeywordFrequency(limit?: number): Observable<KeywordFrequency[]>`
- [ ] Use `forkJoin` to load all data in parallel on init
- [ ] Inject `HttpClient` via `inject()`

### `DashboardComponent` (`features/dashboard/`)

- [ ] `dashboard.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `dashboard.component.html` — external template
- [ ] `dashboard.component.scss`
- [ ] Signal `kpis = signal<DashboardKpis | null>(null)`
- [ ] Signal `loadState = signal<'loading' | 'ready' | 'error'>('loading')`
- [ ] On init: `forkJoin` all analytics calls; set signals on success/error
- [ ] KPI cards section:
    - [ ] Total Proteins card
    - [ ] Reviewed Count card
    - [ ] Distinct Organisms card
    - [ ] Average Length card
    - [ ] "Last updated" footer (if backend exposes refresh timestamp — otherwise omit)
- [ ] Charts section:
    - [ ] Length histogram (bar chart)
    - [ ] Reviewed ratio (donut chart)
    - [ ] Evidence levels (horizontal bar or pie chart)
    - [ ] Top 10 organisms (horizontal bar chart)
- [ ] Chart click handler: `onEvidenceLevelClick(level: number)` → update `filtersStore`, navigate to `/genes`
- [ ] Loading skeleton (grid of card-shaped skeletons)
- [ ] Error state with Retry button
- [ ] `@if` / `@for` only (no `*ngIf` / `*ngFor`)

### Tests

- [ ] `DashboardComponent` unit tests:
    - [ ] Shows loading state during fetch
    - [ ] Shows KPI values after successful fetch
    - [ ] Shows error state on forkJoin failure
    - [ ] Retry button triggers re-fetch
    - [ ] Evidence level click updates filter store and navigates
- [ ] `DashboardService` unit tests (HttpClientTestingModule):
    - [ ] Each method sends correct GET request
    - [ ] `forkJoin` used — verify via parallel request expectation

### General

- [ ] Native control flow only
- [ ] `ChangeDetectionStrategy.OnPush`
- [ ] AXE checks pass
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
