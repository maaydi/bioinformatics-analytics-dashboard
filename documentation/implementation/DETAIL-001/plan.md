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
- [x] GenesService.getById() implemented
- [x] GeneDetailComponent header implemented
- [x] Tab sections implemented
- [ ] 404 handling implemented
- [x] Route configured
- [ ] Unit tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Models (`core/models/` or `features/gene-detail/models/`)

- [ ] `protein-detail.model.ts` — extends `ProteinSummary` with all relational fields
- [ ] `feature-item.model.ts` — `{ type, startPos, endPos, note, featureId }`
- [ ] `go-term-item.model.ts` — `{ goId, aspect, description, evidenceCode }`
- [ ] `cross-reference-item.model.ts` — `{ source, identifier, secondaryId, tertiaryInfo }`
- [ ] `comment-item.model.ts` — `{ type, text }`
- [ ] `publication-item.model.ts` — `{ refNumber, pubmedId, doi, authors, title, journal }`
- [ ] `host-organism-item.model.ts` — `{ taxid, name }`

### Service (`features/genes/genes.service.ts`)

- [ ] `getById(id: number): Observable<ProteinDetail>` — `GET /api/genes/:id`
- [ ] Error mapping: HTTP 404 → propagate as `ProteinNotFoundError` for component handling

### `GeneDetailComponent` (`features/gene-detail/`)

- [ ] `gene-detail.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `gene-detail.component.html` — three-file rule (external template)
- [ ] `gene-detail.component.scss`
- [ ] `ActivatedRoute` via `inject()` — read `:id` param
- [ ] Signal `protein = signal<ProteinDetail | null>(null)`
- [ ] Signal `loadState = signal<'loading' | 'ready' | 'error' | 'not-found'>('loading')`
- [ ] On init: call `getById(id)`, set signals accordingly
- [ ] Header section: Accession, Entry Name, Protein Full Name, Organism, Reviewed badge, Evidence badge, Length, MW
- [ ] Tab shell (Angular Material tabs or native `<details>` / custom):
    - [ ] **Overview tab** — summary fields (dates, sequence version, checksum, lineage)
    - [ ] **Sequence tab** — monospace AA sequence block, length label, feature highlights
    - [ ] **Features tab** — `@for` table over `protein().features`
    - [ ] **GO Terms tab** — `@for` table over `protein().goTerms`
    - [ ] **Cross References tab** — `@for` table with external anchor `target="_blank" rel="noopener noreferrer"`
    - [ ] **Comments tab** — `@for` list over `protein().comments`
  - [x] **Publications tab** — `@for` list over `protein().publications`; PubMed ID linked to
      `https://pubmed.ncbi.nlm.nih.gov/{pubmedId}`
- [ ] Lazy load: fetch all data in one call; lazy-activate tabs (no separate per-tab requests needed for v1 since single
  endpoint returns full detail)
- [ ] 404 state: `@if (loadState() === 'not-found')` shows "Protein not found" message with back link

### Routing

- [ ] Route: `{ path: 'genes/:id', loadComponent: () => GeneDetailComponent }` behind `authGuard`
- [ ] Link from `GenesTableComponent` row click: `router.navigate(['/genes', row.id])`

### Tests

- [ ] `GeneDetailComponent` unit tests:
    - [ ] Renders header fields from mock protein data
    - [ ] Shows loading state during fetch
    - [ ] Shows not-found state on 404 error
    - [ ] Cross-reference links have `target="_blank"` and `rel="noopener noreferrer"`
    - [ ] Features tab renders table rows
- [ ] `GenesService.getById()` test:
    - [ ] Sends `GET /api/genes/1`
    - [ ] Maps 404 correctly

### General

- [ ] External links use `rel="noopener noreferrer"` (security requirement)
- [ ] Native control flow only (`@if`, `@for`)
- [ ] AXE checks pass
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
