# A11Y-001 — Implementation Journal

---

## 2026-05-12

### Ticket created

- Created `overview.md` and `plan.md` from backlog stories US-36, US-37, US-41, US-42.
- Reviewed existing frontend: no `StateHostComponent` found in `shared/`. Theme toggle absent from `layout/`. Password
  change form absent.
- Reviewed existing backend: `PUT /api/auth/password` endpoint is absent from `AuthController`.
- This ticket is the final milestone — should be worked on after all feature tickets (GENE-001 through COMPARE-001,
  OPS-001) are complete.
- `@axe-core/playwright` is not yet installed.
- Implementation not yet started.

---

## 2026-06-29

- Installed `@axe-core/playwright` and `@playwright/test` for e2e accessibility testing.
- Created `playwright.config.ts` configuration.
- Added E2E accessibility suite in `frontend/e2e/axe-smoke.spec.ts` matching WCAG guidelines and failing on `serious` or
  `critical` impacts.
- Added `test:e2e` to `package.json` scripts.
- Updated `plan.md` to reflect axe-core implementation.

### Commits (by date)

#### 2026-06-29 — commits

- `b079afc` — A11Y-001 frontend unit tests
- `d4f8243` — A11Y-001 axe-core e2e configured
- `f899d30` — A11Y-001 Make JSON serialization uniform for refresh-token cache manager
- `94ea9e9` — A11Y-001 Frontend — Password Change Form
- `de0f99a` — A11Y-001 AuthController integration test
- `2d268af` — A11Y-001 logout backend implementation
- `5dd8d0e` — A11Y-001 handle 401 responses — trigger token refresh or redirect to login
- `c4a4168` — A11Y-001 Revoke all refresh tokens for the user cache evict after token expires

#### 2026-06-28 — commits

- `6b921e2` — A11Y-001 Revoke all refresh tokens for the user
- `79644f4` — A11Y-001 update frontend to call backend api after SSR render
- `7ec90ac` — A11Y-001 update frontend guard to load theme at startup and after refresh
- `ba8c0c5` — A11Y-001 PUT /api/auth/password implemented
- `c87c559` — A11Y-001 change password dto
- `5059826` — A11Y-001 Add update password action
- `9c77c59` — A11Y-001 fix start spring warning ( builder default)
- `070343c` — A11Y-001 remove deprecated version

#### 2026-06-27 — commits

- `efdc6c3` — A11Y-001 Theme toggle style refactor : remove duplication / rename variables
- `1d59f0c` — A11Y-001 Theme toggle style variables updated

#### 2026-06-26 — commits

- `6d629a9` — A11Y-001 Theme toggle in navbar implemented
- `ae5f38f` — A11Y-001 ThemeService implemented
- `a213b10` — A11Y-001 update dev script to make it interactive

#### 2026-06-25 — commits

- `4e5c6a0` — A11Y-001 Start feature



