# GENE-003 — Genes Table AG Grid Component

## Description

Implement the Gene Explorer table page in Angular using AG Grid:

- Paginated, server-side data table with sortable columns.
- Row click navigates to the Gene Detail page (`/genes/{id}`).
- Column definitions matching `ProteinSummaryDto` fields.
- Loading, error, and empty states.
- Reacts to filter store changes (re-fetches on any filter update).

## Scope

| Layer           | Artifact                                                                   |
|-----------------|----------------------------------------------------------------------------|
| Page component  | `features/genes/genes-page/genes-page.component` — composes filter + table |
| Table component | `features/genes/genes-table/genes-table.component` — AG Grid, `OnPush`     |
| Service         | `genes.service.ts` — `list()` and `search()` calls                         |
| Routing         | `genes` lazy-loaded route resolving to `GenesPageComponent`                |

## Acceptance Criteria

- [ ] Table renders with columns: Accession, Gene Name, Protein Name, Organism, Length, Reviewed (badge), Evidence
  Level (badge), Keywords.
- [ ] Default page size is 50; page size can be changed to 100 or 200.
- [ ] Clicking a column header sorts ascending; clicking again sorts descending; third click resets to default (
  `id ASC`).
- [ ] Clicking a row navigates to `/genes/{id}`.
- [ ] Table reacts to filter store changes and re-fetches data.
- [ ] Empty state "No proteins found" shown when result count = 0.
- [ ] Loading skeleton shown during fetch.
- [ ] Error state with "Retry" button shown on request failure.
- [ ] Pagination controls show: current page, total pages, total result count.
- [ ] `ChangeDetectionStrategy.OnPush` on all components.
- [ ] Unit tests for `GenesTableComponent` (mock service).

## References

- `documentation/api-contract.md` §1 — `GET /api/genes`, `POST /api/genes/search`
- `documentation/plan.md` — US-4, US-5, US-6, US-36
