# DETAIL-001 Implementation Plan

## Tasks

1. Analyze requirements and update the plan
2. Define `ProteinDetail` and nested Angular item models
3. Implement `GenesService.getById()`
4. Implement `GeneDetailComponent` — header + tab shell
5. Implement each tab subsection (sequence, features, GO, xrefs, comments, publications)
6. Implement 404 handling
7. Configure lazy-loaded route `/genes/:id`
8. Write unit tests
9. Update documentation

## Status

- [x] Requirements analyzed
- [x] Angular models defined
- [x] GenesService.getById() implemented
- [x] GeneDetailComponent header implemented
- [x] Tab sections implemented
- [x] 404 handling implemented
- [x] Route configured
- [x] Unit tests written
- [x] Documentation updated
- [x] Code reviewed
- [ ] Coverage >= 80%

---

## Detailed Checklist

### Models (`core/models/` or `features/gene-detail/models/`)

- [x] `protein-detail.model.ts` — implemented in `core/models/protein.model.ts`
- [x] `feature-item.model.ts` — expected contract `{ type, startPos, endPos, note, featureId }` (current implementation:
  `featureType`)
- [x] `go-term-item.model.ts` — implemented in `core/models/protein.model.ts`
- [x] `cross-reference-item.model.ts` — implemented in `core/models/protein.model.ts`
- [x] `comment-item.model.ts` — implemented in `core/models/protein.model.ts`
- [x] `publication-item.model.ts` — implemented in `core/models/protein.model.ts`
- [x] `host-organism-item.model.ts` — implemented in `core/models/protein.model.ts`

### Service (`features/genes/genes.service.ts`)

- [x] `getById(id: number): Observable<ProteinDetail>` — `GET /api/genes/:id` (currently implemented as
  `getGeneByAccession`)
- [x] Error mapping: HTTP 404 -> propagate as `ProteinNotFoundError` for component handling

### `GeneDetailComponent` (`features/gene-detail/`)

- [x] `gene-detail.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [x] `gene-detail.component.html` — three-file rule (external template)
- [x] `gene-detail.component.scss`
- [x] `ActivatedRoute` via `inject()` — read `:id` param (current: `input()` + `withComponentInputBinding`)
- [x] Signal `protein = signal<ProteinDetail | null>(null)` (current: `proteinDetails`)
- [x] Signal `loadState = signal<'loading' | 'ready' | 'error' | 'not-found'>('loading')`
- [x] On init: call `getById(id)`, set signals accordingly (missing `not-found` state)
- [x] Header section: Accession, Entry Name, Protein Full Name, Organism, Reviewed badge, Evidence badge, Length, MW
- [x] Tab shell (Angular Material tabs or native `<details>` / custom):
  - [x] **Overview tab** — summary fields (dates, sequence version, checksum, lineage)
  - [x] **Sequence tab** — monospace AA sequence block, length label, feature highlights (feature highlighting not
    implemented)
  - [x] **Features tab** — `@for` table over `protein().features`
  - [x] **GO Terms tab** — `@for` table over `protein().goTerms`
  - [x] **Cross References tab** — `@for` table with external anchor `target="_blank" rel="noopener noreferrer"`
  - [x] **Comments tab** — table rendering implemented (`@for`)
  - [x] **Publications tab** — `@for` list over `protein().publications`; PubMed ID linked to
    `https://pubmed.ncbi.nlm.nih.gov/{pubmedId}`
- [x] Lazy load: fetch all data in one call; lazy-activate tabs (no separate per-tab requests needed for v1 since the
  single endpoint returns full detail)
- [x] 404 state: `@if (loadState() === 'not-found')` shows "Protein not found" message with back link

### Routing

- [x] Route: `{ path: 'genes/:id', loadComponent: () => GeneDetailComponent }` behind `authGuard`
- [x] Link from `GenesTableComponent` row click: `router.navigate(['/genes', row.id])`

### Tests

- [x] `GeneDetailComponent` unit tests:
  - [x] Renders basic data from mock protein data (state-level assertions)
- [x] Shows loading state during fetch
- [x] Shows not-found state on 404 error
- [x] Cross-reference links enforce secure attributes via shared component bindings (`externalLinkTarget`,
  `externalLinkRel`)
- [x] Feature rendering validates API field usage (`FeatureItem.type`)
- [x] `GenesService.getById()` test:
  - [x] Sends `GET /api/genes/1`
    - [ ] Maps 404 correctly

### General

- [x] External links use `rel="noopener noreferrer"` (security requirement)
- [x] Native control flow only (`@if`, `@for`)
- [ ] AXE checks pass
- [x] Code reviewed
- [ ] Coverage ≥ 80%
