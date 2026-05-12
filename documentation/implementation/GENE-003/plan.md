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

- [ ] Requirements analyzed
- [ ] AG Grid dependency verified
- [ ] GenesTableComponent implemented
- [ ] GenesPageComponent implemented
- [ ] Filter → table wiring done
- [ ] All UI states implemented
- [ ] Route configured
- [ ] Unit tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Dependencies

- [ ] Verify `ag-grid-angular` and `ag-grid-community` in `package.json`
- [ ] Import `AgGridModule` (or standalone `AgGridAngular`) in component

### `GenesTableComponent` (`features/genes/genes-table/`)

- [ ] `genes-table.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `genes-table.component.html` — `<ag-grid-angular>` template
- [ ] `genes-table.component.scss`
- [ ] `input()` for `searchRequest: GeneSearchRequest`
- [ ] `output()` `rowClicked` event emitting `ProteinSummary`
- [ ] Column definitions:
    - [ ] `accession` — sortable, linkable
    - [ ] `geneNamePrimary` — sortable
    - [ ] `proteinFullName` — sortable
    - [ ] `organismName` — sortable
    - [ ] `length` — sortable, right-aligned
    - [ ] `reviewed` — boolean badge renderer (`Reviewed` / `Unreviewed`)
    - [ ] `evidenceLevel` — badge renderer (1–5 with label)
    - [ ] `keywords` — chip list renderer (first 3 + overflow)
- [ ] Server-side pagination: `datasource` calling `GenesService.search()`
- [ ] Sort event → update store sort field and direction
- [ ] Page size options: 50, 100, 200 (validated at 200 max)
- [ ] Loading overlay (AG Grid built-in + custom skeleton)
- [ ] Empty overlay "No proteins found"
- [ ] Error state: catch HTTP errors, display error block with Retry

### `GenesPageComponent` (`features/genes/genes-page/`)

- [ ] `genes-page.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `genes-page.component.html` — hosts `GeneFilterComponent` + `GenesTableComponent`
- [ ] `genes-page.component.scss`
- [ ] Subscribes to `filtersStore` changes; passes updated request to table
- [ ] Row click handler: `router.navigate(['/genes', id])`

### Routing

- [ ] Lazy-loaded route: `{ path: 'genes', loadComponent: () => GenesPageComponent }`
- [ ] Route registered in `app.routes.ts` behind `authGuard`

### Tests

- [ ] `GenesTableComponent` unit tests:
    - [ ] Renders column headers
    - [ ] Emits `rowClicked` on row click
    - [ ] Shows empty overlay when data = []
    - [ ] Shows error state on service error
- [ ] `GenesPageComponent` unit tests:
    - [ ] Passes filter store value to table component
    - [ ] Navigates to detail on row click

### General

- [ ] No `ngClass` / `ngStyle` — `class` / `style` bindings only
- [ ] Native control flow only (`@if`, `@for`)
- [ ] AXE checks pass
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
