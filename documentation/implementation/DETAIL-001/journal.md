# DETAIL-001 — Implementation Journal

---

## 2026-05-12

### Ticket created

- Created `overview.md` and `plan.md` from backlog stories US-15, US-16, US-17.
- Reviewed existing frontend structure: `features/gene-detail/gene-detail.component.{ts,html,scss}` files exist;
  implementation status to be verified (likely stub).
- `ProteinDetail` Angular model is absent — needs creation.
- Implementation not yet started.

## 2026-05-26

### Code review — `gene-detail` implementation

- Scope reviewed:
  - `frontend/src/app/features/gene-detail/gene-detail.component.ts`
  - `frontend/src/app/features/gene-detail/gene-detail.component.html`
  - `frontend/src/app/features/gene-detail/gene-detail.component.spec.ts`
  - `frontend/src/app/features/genes/genes.service.ts`
  - `frontend/src/app/features/genes/genes.service.spec.ts`
  - `frontend/src/app/core/models/protein.model.ts`

#### Findings (triaged)

- **High** — API contract mismatch on feature DTO naming:
  - `documentation/api-contract.md` defines `FeatureItem.type`
  - frontend model uses `featureType` in `frontend/src/app/core/models/protein.model.ts`
  - template rendering and class tracking rely on `feature.featureType` in
    `frontend/src/app/features/gene-detail/gene-detail.component.html`
- **High** — 404 handling not implemented end-to-end:
  - `frontend/src/app/features/genes/genes.service.ts` has no 404 mapping to domain error
  - `frontend/src/app/features/gene-detail/gene-detail.component.ts` collapses all HTTP errors into generic message
  - `frontend/src/app/features/gene-detail/gene-detail.component.html` has no dedicated not-found state/back link
- **Medium** — Route param typing risk:
  - `id` is consumed as `input.required<number>()` in `frontend/src/app/features/gene-detail/gene-detail.component.ts`,
    while router path params are strings at runtime without explicit transform/parse
- **Medium** — Unit test coverage gaps vs ticket acceptance:
  - missing not-found behavior tests and link attribute assertions in
    `frontend/src/app/features/gene-detail/gene-detail.component.spec.ts`
  - missing 404 mapping test in `frontend/src/app/features/genes/genes.service.spec.ts`

#### Validation runs

- `npm test` (frontend): **PASS** (17 files, 164 tests)
- `npx ng test --watch=false --coverage`: **PASS** with coverage report generated
  - Global coverage reported:
    - Statements: **77.31%**
    - Branches: **82.16%**
    - Functions: **75.86%**
    - Lines: **82.21%**
  - `gene-detail` feature coverage remains below target in multiple dimensions (e.g. statements/lines)

#### Status impact

- Ticket remains **not done**:
  - 404 handling incomplete
  - API/model contract alignment required
  - tests missing for required error/security cases
  - coverage gate not yet fully satisfied for this feature scope

