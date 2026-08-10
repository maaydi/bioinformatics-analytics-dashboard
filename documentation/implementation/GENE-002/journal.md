# GENE-002 — Implementation Journal

---

## 2026-05-12

### Ticket created

- Created `overview.md` and `plan.md` from backlog stories US-7, US-8, US-9, US-10, US-27, US-28, US-29, US-30.
- Reviewed existing frontend structure: `features/genes/gene-filter/` folder exists; contents not yet verified as
  complete.
- `genes.service.ts` exists at `features/genes/genes.service.ts`; implementation status unknown.
- Signal store (`filters.store.ts`) is absent — needs to be created.
- Implementation not yet started.

## 2026-05-22

### Implementation review (actual state)

- Reviewed scope against `overview.md`, `plan.md`, and current implementation in `frontend/src/app/features/genes`,
  `frontend/src/app/shared/components/input`, and `frontend/src/app/shared/components/range-input`.

#### Done

- [x] Signal-based feature store exists and wires search/loading/error/result state (
  `frontend/src/app/features/genes/state/filters.store.ts`).
- [x] Filter panel component exists with reactive form and most required controls (
  `frontend/src/app/features/genes/gene-filter/gene-filter.component.ts`).
- [x] Global search debounce is implemented with `debounceTime(400)` and `distinctUntilChanged()` (
  `frontend/src/app/features/genes/gene-filter/gene-filter.component.ts`).
- [x] Evidence-level multi-select behavior is implemented and mapped into API snapshot (
  `frontend/src/app/features/genes/gene-filter/gene-filter.component.ts`).
- [x] Active filter chips are rendered above the table (
  `frontend/src/app/features/genes/genes-table/genes-table.component.ts`,
  `frontend/src/app/features/genes/genes-table/genes-table.component.html`).
- [x] Loading, error, and empty states are present in table rendering (
  `frontend/src/app/features/genes/genes-table/genes-table.component.html`).
- [x] `ChangeDetectionStrategy.OnPush` is set on key feature components (
  `frontend/src/app/features/genes/gene-filter/gene-filter.component.ts`,
  `frontend/src/app/features/genes/genes-table/genes-table.component.ts`,
  `frontend/src/app/features/genes/genes-page/genes-page.component.ts`).
- [x] Unit tests exist for `GeneFilterComponent` (
  `frontend/src/app/features/genes/gene-filter/gene-filter.component.spec.ts`).

#### Not done

- [x] `GeneFilterComponent` template does not render the required `Gene Name` field (`geneNamePrimary`) (
  `frontend/src/app/features/genes/gene-filter/gene-filter.component.html`).
- [x] Required cross-field validators are missing: `lengthRangeValidator` and `molecularWeightRangeValidator`; min > max
  is not blocked before submit (`frontend/src/app/features/genes/gene-filter/gene-filter.component.ts`).
- [x] Inline range validation errors for invalid min/max combinations are not implemented in the filter UI (
  `frontend/src/app/features/genes/gene-filter/gene-filter.component.html`).
- [x] Active filter chips are not dismissible and there is no per-chip clear action (
  `frontend/src/app/features/genes/genes-table/genes-table.component.html`,
  `frontend/src/app/features/genes/genes-table/genes-table.component.ts`).
- [x] "Clear All" clears local state but does not reload unfiltered table results as required (
  `frontend/src/app/features/genes/gene-filter/gene-filter.component.ts`,
  `frontend/src/app/features/genes/genes-page/genes-page.component.html`,
  `frontend/src/app/features/genes/state/filters.store.ts`).
- [x] Filter state is not persistent across page navigation because `GenesStore` is provided at page component level (
  `frontend/src/app/features/genes/genes-page/genes-page.component.ts`).
- [x] `GenesService` is not aligned with the planned/contract method surface (`search`, `getByAccession`, `exportCsv`
  contract-centric naming/signature) (`frontend/src/app/features/genes/genes.service.ts`,
  `documentation/implementation/GENE-002/plan.md`).
- [ ] `GenesService` unit tests are missing (`frontend/src/app/features/genes`).
- [x] Table uses `NgClass`/`[ngClass]` despite project constraint to avoid it (
  `frontend/src/app/features/genes/genes-table/genes-table.component.ts`,
  `frontend/src/app/features/genes/genes-table/genes-table.component.html`).
- [x] Shared components still explicitly set `standalone: true`, which conflicts with project Angular v20+ rule (
  `frontend/src/app/shared/components/input/input.component.ts`,
  `frontend/src/app/shared/components/range-input/range-input.component.ts`).

## 2026-05-23

### Post-fix status

#### Done

- [x] Added missing client-side validators in `GeneFilterComponent` aligned with `documentation/validation-rules.md`
  for:
  - `taxid > 0` (positive integer)
  - `globalSearch` max 200
  - `accession` max 20
  - `geneNamePrimary` max 100
  - `organism` max 300
  - `keywords` max 10 items and max 100 per item
    (`frontend/src/app/features/genes/gene-filter/gene-filter.component.ts`).
- [x] Added/updated inline validation messages in filter UI for newly enforced constraints (
  `frontend/src/app/features/genes/gene-filter/gene-filter.component.html`).
- [x] Shared controls `input` and `range-input` explicitly use `ChangeDetectionStrategy.OnPush` (
  `frontend/src/app/shared/components/input/input.component.ts`,
  `frontend/src/app/shared/components/range-input/range-input.component.ts`).
- [x] Added `GenesService` unit tests with success-path coverage for `listGenes`, `searchGenes`, `getGeneByAccession`,
  `exportCsv`, and `loadKeywords`, including request URL/method/body/params assertions (
  `frontend/src/app/features/genes/genes.service.spec.ts`).

#### Explicitly accepted (won't fix per user directive)

- [x] Keep current behavior where clear-all does not automatically reload unfiltered results.
- [x] Keep table hidden when no filters are applied.
- [x] Keep current payload spread behavior for `length` / `molecularWeight` in search snapshot.
