# GENE-003 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Verify / install AG Grid dependency (`ag-grid-angular`, `ag-grid-community`)
3. Implement `GenesTableComponent` with AG Grid server-side row model
4. Implement `GenesPageComponent` (composes filter + table)
5. Wire filter store to table re-fetch
6. Implement loading, error, and empty states
7. Configure lazy-loaded route for `genes`
8. Write unit tests
9. Update documentation

## Status

- [x] Requirements analyzed
- [x] AG Grid dependency verified
- [x] GenesTableComponent implemented with AG Grid
- [x] GenesPageComponent implemented
- [x] Filter → table wiring done
- [x] UI states implemented (loading skeleton, error + retry, empty)
- [x] Route configured
- [x] Unit tests completed for key GENE-003 interactions
- [x] Documentation updated
- [x] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Dependencies

- [x] Verify `ag-grid-angular` and `ag-grid-community` in `package.json`
- [x] Import standalone `AgGridAngular` in component

### `GenesTableComponent` (`features/genes/genes-table/`)

- [x] `genes-table.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [x] `genes-table.component.html` — `<ag-grid-angular>` template
- [x] `genes-table.component.scss`
- [x] Input-driven rendering from paged search response in store container flow
- [x] `output()` `rowClick` event emitting `ProteinSummary`
- [x] Responsive column-width tuning (fit-to-container + truncation + tooltips)
- [x] Column definitions:
  - [x] `accession` — sortable, linkable
  - [x] `geneNamePrimary` — sortable
  - [x] `proteinFullName` — sortable
  - [x] `organismName` — sortable
  - [x] `length` — sortable, right-aligned
  - [x] `reviewed` — boolean badge renderer (`Reviewed` / `Unreviewed`)
  - [x] `evidenceLevel` — badge renderer (1–5 with label)
  - [x] `keywords` — compact renderer (first 3 + overflow)
- [~] Server-side pagination via store + `POST /api/genes/search` (no AG Grid enterprise row model)
- [x] Sort event → update store sort field and direction
- [x] Page size options: 50, 100, 200 (via paginator)
- [x] Loading overlay (custom skeleton)
- [x] Empty overlay "No proteins found"
- [x] Error state: error block with Retry

### `GenesPageComponent` (`features/genes/genes-page/`)

- [x] `genes-page.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [x] `genes-page.component.html` — hosts `GeneFilterComponent` + `GenesTableComponent`
- [x] `genes-page.component.scss`
- [x] Subscribes to `filtersStore` changes; passes updated request to table
- [x] Row click handler: `router.navigate(['/genes', id])`

### Routing

- [x] Lazy-loaded route: `{ path: 'genes', loadComponent: () => GenesPageComponent }`
- [x] Route registered in `app.routes.ts` behind `authGuard`

### Tests

- [x] `GenesTableComponent` unit tests:
  - [x] Renders column headers
  - [x] Emits `rowClicked` on row click
  - [x] Shows empty overlay when data = [] with required copy
  - [x] Shows error state on request error and exposes Retry action
  - [x] Emits sort updates for asc/desc/reset cycle
- [x] `GenesPageComponent` unit tests:
  - [x] Navigates to detail on row click
  - [x] Retries search with active filters

### General

- [x] No `ngClass` / `ngStyle` — `class` / `style` bindings only
- [x] Native control flow only (`@if`, `@for`)
- [ ] AXE checks pass
- [x] Code reviewed
- [ ] Coverage ≥ 80%
