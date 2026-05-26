# DETAIL-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Define `ProteinDetail` and nested item Angular models
3. Implement `GenesService.getById()` method
4. Implement `GeneDetailComponent` — header + tab shell
5. Implement each tab sub-section (sequence, features, GO, xrefs, comments, publications)
6. Implement 404 handling
7. Configure lazy-loaded route `/genes/:id`
8. Write unit tests
9. Update documentation

## Status

- [x] Requirements analyzed
- [x] Angular models defined
- [ ] GenesService.getById() implemented
- [x] GeneDetailComponent header implemented
- [x] Tab sections implemented
- [ ] 404 handling implemented
- [x] Route configured
- [ ] Unit tests written
- [x] Documentation updated
- [x] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Models (`core/models/` or `features/gene-detail/models/`)

- [x] `protein-detail.model.ts` — implémenté dans `core/models/protein.model.ts`
- [ ] `feature-item.model.ts` — contrat attendu `{ type, startPos, endPos, note, featureId }` (implémentation actuelle:
  `featureType`)
- [x] `go-term-item.model.ts` — implémenté dans `core/models/protein.model.ts`
- [x] `cross-reference-item.model.ts` — implémenté dans `core/models/protein.model.ts`
- [x] `comment-item.model.ts` — implémenté dans `core/models/protein.model.ts`
- [x] `publication-item.model.ts` — implémenté dans `core/models/protein.model.ts`
- [x] `host-organism-item.model.ts` — implémenté dans `core/models/protein.model.ts`

### Service (`features/genes/genes.service.ts`)

- [ ] `getById(id: number): Observable<ProteinDetail>` — `GET /api/genes/:id` (implémenté sous le nom `getGeneById`)
- [ ] Error mapping: HTTP 404 → propagate as `ProteinNotFoundError` for component handling

### `GeneDetailComponent` (`features/gene-detail/`)

- [x] `gene-detail.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [x] `gene-detail.component.html` — three-file rule (external template)
- [x] `gene-detail.component.scss`
- [ ] `ActivatedRoute` via `inject()` — read `:id` param (actuel: `input()` + `withComponentInputBinding`)
- [x] Signal `protein = signal<ProteinDetail | null>(null)` (actuel: `proteinDetails`)
- [ ] Signal `loadState = signal<'loading' | 'ready' | 'error' | 'not-found'>('loading')`
- [ ] On init: call `getById(id)`, set signals accordingly (état `not-found` manquant)
- [x] Header section: Accession, Entry Name, Protein Full Name, Organism, Reviewed badge, Evidence badge, Length, MW
- [ ] Tab shell (Angular Material tabs or native `<details>` / custom):
  - [x] **Overview tab** — summary fields (dates, sequence version, checksum, lineage)
  - [ ] **Sequence tab** — monospace AA sequence block, length label, feature highlights (highlight des features non
    implémenté)
  - [x] **Features tab** — `@for` table over `protein().features`
  - [x] **GO Terms tab** — `@for` table over `protein().goTerms`
  - [x] **Cross References tab** — `@for` table with external anchor `target="_blank" rel="noopener noreferrer"`
  - [x] **Comments tab** — rendu implémenté en table (`@for`)
  - [x] **Publications tab** — `@for` list over `protein().publications`; PubMed ID linked to
      `https://pubmed.ncbi.nlm.nih.gov/{pubmedId}`
- [x] Lazy load: fetch all data in one call; lazy-activate tabs (no separate per-tab requests needed for v1 since single
  endpoint returns full detail)
- [ ] 404 state: `@if (loadState() === 'not-found')` shows "Protein not found" message with back link

### Routing

- [x] Route: `{ path: 'genes/:id', loadComponent: () => GeneDetailComponent }` behind `authGuard`
- [x] Link from `GenesTableComponent` row click: `router.navigate(['/genes', row.id])`

### Tests

- [ ] `GeneDetailComponent` unit tests:
    - [ ] Renders header fields from mock protein data
    - [ ] Shows loading state during fetch
    - [ ] Shows not-found state on 404 error
    - [ ] Cross-reference links have `target="_blank"` and `rel="noopener noreferrer"`
    - [ ] Features tab renders table rows
- [ ] `GenesService.getById()` test:
  - [x] Sends `GET /api/genes/1`
    - [ ] Maps 404 correctly

### General

- [x] External links use `rel="noopener noreferrer"` (security requirement)
- [x] Native control flow only (`@if`, `@for`)
- [ ] AXE checks pass
- [x] Code reviewed
- [ ] Coverage ≥ 80%
