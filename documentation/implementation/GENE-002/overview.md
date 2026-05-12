# GENE-002 — Gene Filter Angular Component

## Description

Implement the Gene Explorer filter panel in Angular:

- Reactive form with all filter fields defined in `POST /api/genes/search`.
- Active filter chips displayed above the results table.
- "Clear All" action that resets the filter store.
- Shared signal-based filter store (`filters.store.ts`) consumed by both filter panel and table.

## Scope

| Layer            | Artifact                                                                           |
|------------------|------------------------------------------------------------------------------------|
| Feature store    | `features/genes/state/filters.store.ts` — signal store holding `GeneSearchRequest` |
| Filter component | `features/genes/gene-filter/gene-filter.component` — reactive form, chip list      |
| Service          | `genes.service.ts` — `search(request)` calling `POST /api/genes/search`            |
| Models           | `GeneSearchRequest`, `ProteinSummary`, `PagedResponse<T>` Angular interfaces       |
| Validation       | Cross-field validators: `lengthRangeValidator`, `molecularWeightRangeValidator`    |

## Acceptance Criteria

- [ ] Filter panel renders fields: Global Search, Accession, Entry Name, Gene Name, Protein Name, Organism, TaxID,
  Reviewed toggle, Length Min/Max, Molecular Weight Min/Max, Evidence Levels (multi-select 1–5), Keywords (chip input),
  GO Term ID, GO Aspect, Feature Type, Cross-Ref Source.
- [ ] Typing in Global Search debounces 400 ms (`debounceTime(400) + distinctUntilChanged()`).
- [ ] Setting Length Min > Length Max shows inline error and blocks submission.
- [ ] Setting Molecular Weight Min > Max shows inline error and blocks submission.
- [ ] Evidence Levels multi-select sends array of integers to API.
- [ ] Active filters render as dismissible chips above the results table; removing a chip clears that field.
- [ ] "Clear All Filters" resets every field and reloads the table with no filters.
- [ ] Filter state persists when navigating between pages (signal store survives component destroy).
- [ ] Loading, error, and empty states handled.
- [ ] `ChangeDetectionStrategy.OnPush` on all components.
- [ ] Unit tests for `GeneFilterComponent` and `GenesService`.

## References

- `documentation/api-contract.md` §1 — `POST /api/genes/search` request schema
- `documentation/validation-rules.md` — client-side validation rules
- `documentation/plan.md` — US-7, US-8, US-9, US-10, US-27, US-28, US-29, US-30
