# DASH-001 — Implementation Journal

---

## 2026-05-12

### Ticket created

- Created `overview.md` and `plan.md` from backlog stories US-32, US-33, US-34, US-35.
- Reviewed existing frontend: `features/dashboard/dashboard.component.{ts,html,scss}` and `dashboard.service.ts` exist;
  implementation status to be verified.
- Chart library dependency (`ngx-echarts` or `ng-apexcharts`) to be confirmed in `package.json`.
- `ANALYTICS-001` (backend) must be completed first — this ticket depends on analytics endpoints being live.
- Implementation not yet started.

## 2026-05-31

### Dashboard data wiring and tests

- Replaced dashboard mock data with live API integration through `DashboardService` in all dashboard chart components.
- Added loading, error, and empty states for KPI cards, length histogram, reviewed ratio, evidence levels, and top
  organisms.
- Added/updated unit tests for all dashboard components and dashboard container.
- Ran targeted dashboard tests: `34/34` passing.
- Ran coverage for dashboard scope: `97.60% statements`, `93.38% branches`, `100% functions`, `97.26% lines`.
- Remaining open item: chart click-to-filter navigation to `/genes` is not yet wired.

### DASH-001 completion pass

- Implemented evidence-level click-to-filter flow: clicking an evidence row now updates `GenesStore` with
  `evidenceLevels: [level]` and navigates to `/genes`.
- Added retry actions for all dashboard error states (KPI container, histogram, reviewed ratio, evidence levels,
  top organisms).
- Updated `GenesPageComponent` initialization to preserve preloaded filters (`activeFilters`) so dashboard-driven
  navigation keeps the selected evidence filter.
- Expanded unit tests for:
  - dashboard container retry + evidence click navigation wiring,
  - evidence levels retry + output emission,
  - histogram / reviewed ratio / top organisms retry behavior.
- Ran targeted verification:
  -
  `npm test -- --include "src/app/features/dashboard/**/*.spec.ts" --include "src/app/features/genes/genes-page/genes-page.component.spec.ts"`
  - Result: `8/8` files passing, `50/50` tests passing.
- Ran focused coverage with:
  -
  `npm test -- --coverage --include "src/app/features/dashboard/**/*.spec.ts" --include "src/app/features/genes/genes-page/genes-page.component.spec.ts"`
  - Dashboard files remain above threshold (examples: `dashboard.component.ts` 96.55% statements,
    `dashboard.service.ts` 100%, evidence/ratio/histogram/top-organisms components ≥ 97% statements).
- Remaining manual activity: AXE audit still pending (`plan.md` keeps this unchecked).

### Evidence row click follow-up fix

- Addressed a runtime issue where clicking an evidence row did not trigger navigation reliably from the dashboard.
- Moved the click-to-filter behavior directly into `dashboard-evidence-levels.component.ts` so the component now updates
  `GenesStore` and navigates to `/genes` itself.
- Removed the now-unnecessary parent output wiring from `dashboard.component.html` / `dashboard.component.ts`.
- Re-ran focused verification:
  -
  `npm test -- --include "src/app/features/dashboard/components/dashboard-evidence-levels/dashboard-evidence-levels.component.spec.ts" --include "src/app/features/dashboard/dashboard/dashboard.component.spec.ts" --include "src/app/features/genes/genes-page/genes-page.component.spec.ts"`
  - `npm test -- --include "src/app/features/dashboard/**/*.spec.ts"`
- Result: all targeted tests passing after the fix.

