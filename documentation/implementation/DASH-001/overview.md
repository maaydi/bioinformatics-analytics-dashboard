# DASH-001 — Dashboard Page (KPI Cards + Charts)

## Description

Implement the Dashboard page in Angular:

- KPI cards sourced from `GET /api/analytics/dashboard-kpis`.
- Length histogram chart sourced from `GET /api/analytics/length-histogram`.
- Reviewed ratio donut chart sourced from `GET /api/analytics/reviewed-ratio`.
- Evidence levels bar/pie chart sourced from `GET /api/analytics/evidence-levels`.
- Top organisms chart sourced from `GET /api/analytics/by-organism`.
- "Last updated" timestamp footer on each card.
- Chart click events that push filter values into the shared `filtersStore` and navigate to the Gene Explorer.

## Scope

| Layer          | Artifact                                                                             |
|----------------|--------------------------------------------------------------------------------------|
| Page component | `features/dashboard/dashboard.component`                                             |
| Service        | `features/dashboard/dashboard.service.ts` — calls all analytics endpoints            |
| Angular models | `DashboardKpis`, `LengthBucket`, `OrganismCount`, `ReviewedRatio`, `EvidenceLevel`   |
| Charts library | ECharts (`ngx-echarts`) or ApexCharts (`ng-apexcharts`) — verify existing dependency |

## Acceptance Criteria

- [ ] KPI cards display: Total Proteins, Reviewed Count, Distinct Organisms, Average Length, Top 5 Organisms.
- [ ] Each KPI card shows "Last updated: `<timestamp>`" footer (from materialized view refresh time).
- [ ] Length histogram renders 100-AA buckets; hover tooltip shows range, count, and percentage.
- [ ] Reviewed ratio donut shows two segments with counts and percentages in tooltip.
- [ ] Evidence levels chart shows 5 labelled entries.
- [ ] Top organisms bar chart shows top 10 by default.
- [ ] Clicking an evidence level segment navigates to `/genes` with that evidence level pre-filtered.
- [ ] Loading skeleton shown while data is being fetched.
- [ ] Error state with "Retry" button shown on any endpoint failure.
- [ ] `ChangeDetectionStrategy.OnPush` on all components.
- [ ] Unit tests for `DashboardComponent` and `DashboardService`.

## References

- `documentation/api-contract.md` §2 — Analytics Endpoints
- `documentation/plan.md` — US-32, US-33, US-34, US-35
