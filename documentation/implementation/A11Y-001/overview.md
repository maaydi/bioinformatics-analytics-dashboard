# A11Y-001 — Accessibility & UX Polish

## Description

Ensure the full application meets WCAG AA accessibility standards and provides a polished UX:

- All pages pass `axe-core` checks with zero violations of severity ≥ "serious".
- Keyboard navigation works for all interactive elements.
- Color contrast ≥ 4.5:1 for normal text in both light and dark themes.
- Loading, error, and empty states present on every list/table/chart view.
- Shared `StateHostComponent` for consistent state rendering.
- Light/dark theme toggle persisted in `localStorage`.
- Change own password feature (US-41).

## Scope

| Layer            | Artifact                                                                  |
|------------------|---------------------------------------------------------------------------|
| Shared component | `shared/state-host/state-host.component` — discriminated union states     |
| Theme service    | `core/services/theme.service.ts` — toggle and persist theme               |
| Layout component | `layout/` — theme toggle button in navbar                                 |
| Backend (auth)   | `PUT /api/auth/password` — change own password; revoke all refresh tokens |
| Frontend         | Password change form in a user profile/settings page                      |
| E2E testing      | `@axe-core/playwright` configured in e2e suite                            |

## Acceptance Criteria

- [ ] Every list, table, and chart view has a loading skeleton, error block with Retry, and empty state message.
- [ ] Shared `StateHostComponent` accepts `{ status: 'loading' | 'error' | 'empty' | 'ready', data?, error? }` and
  renders the correct state.
- [ ] All interactive elements reachable and operable via keyboard (Tab, Enter, Space, Escape for dialogs).
- [ ] `axe-core` reports 0 violations of severity "serious" or "critical" on all page routes.
- [ ] Color contrast ≥ 4.5:1 for normal text in both themes.
- [ ] Theme toggle in the navbar switches light/dark; preference persists across page reloads.
- [ ] `PUT /api/auth/password` with valid current password + new password (≥ 12 chars, mixed case + digit) returns
  `200`.
- [ ] `PUT /api/auth/password` with wrong current password returns `401`.
- [ ] `PUT /api/auth/password` with non-compliant new password returns `400`.
- [ ] After successful password change, all existing refresh tokens are revoked.
- [ ] Unit tests for `ThemeService` and `StateHostComponent`.
- [ ] E2E axe smoke test added to CI pipeline.

## References

- `documentation/plan.md` — US-36, US-37, US-41, US-42
- `documentation/overview.md` — NFRs §12.3 Accessibility
