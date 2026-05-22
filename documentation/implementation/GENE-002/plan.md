# GENE-002 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Define Angular models (`GeneSearchRequest`, `ProteinSummary`, `PagedResponse<T>`)
3. Implement `filters.store.ts` signal store
4. Implement `GenesService.search()` method
5. Implement `GeneFilterComponent` reactive form + cross-field validators
6. Implement active-filter chip list
7. Implement "Clear All" action
8. Connect filter changes to table reload (via store)
9. Write unit tests
10. Update documentation

## Status

- [x] Requirements analyzed
- [x] Angular models defined
- [x] Signal store implemented
- [ ] GenesService updated
- [x] GeneFilterComponent implemented
- [x] Chip list implemented
- [x] Clear All implemented
- [x] Integration with table complete
- [ ] Unit tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Models (`core/models/`)

- [ ] `gene-search-request.model.ts` — all optional fields typed per API contract
- [ ] `protein-summary.model.ts` — flat projection matching `ProteinSummaryDto`
- [ ] `paged-response.model.ts` — generic `PagedResponse<T>` interface (may already exist)

### Signal Store (`features/genes/state/filters.store.ts`)

- [ ] `filtersStore` created with `signal<GeneSearchRequest>({})` default
- [ ] `setFilter(partial: Partial<GeneSearchRequest>)` updater
- [ ] `clearFilters()` updater (resets to `{}`)
- [ ] `computed()` for derived active-chip list (non-null, non-default fields)

### Service (`features/genes/genes.service.ts`)

- [ ] `search(request: GeneSearchRequest): Observable<PagedResponse<ProteinSummary>>`
- [ ] `getById(id: number): Observable<ProteinDetail>`
- [ ] `exportCsv(request: Omit<GeneSearchRequest, 'page'|'size'|'sort'|'direction'>): Observable<Blob>`
- [ ] Inject `HttpClient` via `inject()`

### Filter Component (`features/genes/gene-filter/`)

- [ ] `gene-filter.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `gene-filter.component.html` — reactive form using native control flow
- [ ] `gene-filter.component.scss` — modular SCSS
- [ ] Reactive form with `FormBuilder`:
    - [ ] `globalSearch` — text, debounce 400 ms
    - [ ] `accession`, `entryName`, `geneNamePrimary`, `proteinFullName` — text
    - [ ] `organism`, `taxid`, `lineage` — text / number
    - [ ] `reviewed` — nullable boolean (tri-state)
    - [ ] `lengthMin`, `lengthMax` — number with cross-field validator
    - [ ] `molecularWeightMin`, `molecularWeightMax` — number with cross-field validator
    - [ ] `evidenceLevels` — multi-select checkbox group (values 1–5)
    - [ ] `keywords` — chip input (comma-separated → string[])
    - [ ] `goTermId` — text with pattern validation `GO:\d{7}`
    - [ ] `goAspect` — select (P, F, C)
    - [ ] `featureType`, `crossRefSource` — text
- [ ] Cross-field validators:
    - [ ] `lengthRangeValidator` — error if `lengthMin > lengthMax`
    - [ ] `molecularWeightRangeValidator` — error if min > max
- [ ] `output()` `filtersChanged` event (or direct store update on value changes)
- [ ] Active chip list: `@for` over computed chip list; chip `close` calls `clearField(field)`
- [ ] "Clear All" button calls `clearFilters()` on store
- [ ] Loading, error, empty states wired to parent table state

### Tests

- [ ] `GeneFilterComponent` unit tests (Jest/Karma):
    - [ ] Form renders all controls
    - [ ] lengthMin > lengthMax shows validation error
    - [ ] Clear All resets form values
    - [ ] debounce does not emit immediately on each keystroke
- [ ] `GenesService` unit tests (HttpClientTestingModule):
    - [ ] `search()` sends correct POST body
    - [ ] `exportCsv()` sends correct request and returns Blob

### General

- [ ] AXE accessibility check passes (labels, focus, contrast)
- [ ] `ngClass`/`ngStyle` not used — `class` / `style` bindings only
- [ ] No `*ngIf` / `*ngFor` — native control flow (`@if`, `@for`) only
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
