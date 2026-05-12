# A11Y-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Implement `StateHostComponent` (shared loading/error/empty/ready states)
3. Integrate `StateHostComponent` into all list/table/chart views
4. Implement `ThemeService` + theme toggle in navbar
5. Audit and fix WCAG AA color contrast in both themes
6. Audit and fix keyboard navigation across all pages
7. Implement `PUT /api/auth/password` endpoint (backend)
8. Implement password change form (frontend profile/settings page)
9. Add `@axe-core/playwright` to e2e suite
10. Write unit tests for `ThemeService` and `StateHostComponent`
11. Update documentation

## Status

- [ ] Requirements analyzed
- [ ] StateHostComponent implemented
- [ ] State integration in all views done
- [ ] ThemeService implemented
- [ ] Theme toggle in navbar implemented
- [ ] WCAG AA contrast verified and fixed
- [ ] Keyboard navigation audited and fixed
- [ ] PUT /api/auth/password implemented
- [ ] Password change form implemented
- [ ] axe-core e2e configured
- [ ] Unit tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Shared `StateHostComponent` (`shared/state-host/`)

- [ ] `state-host.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `state-host.component.html` — external template
- [ ] `state-host.component.scss`
- [ ] `input() state: 'loading' | 'error' | 'empty' | 'ready'`
- [ ] `input() errorMessage: string` (optional, shown in error state)
- [ ] `output() retry` event emitted from "Retry" button
- [ ] `ng-content` slot for `ready` state content
- [ ] Skeleton layout for `loading` state (generic card skeleton)
- [ ] Error block for `error` state with icon + message + Retry button
- [ ] Empty state block for `empty` state with icon + message slot

### State Integration

- [ ] `GenesTableComponent` — wire `StateHostComponent`
- [ ] `DashboardComponent` — wire `StateHostComponent`
- [ ] `AnalyticsComponent` — wire `StateHostComponent` per chart
- [ ] `SavedFiltersComponent` — wire `StateHostComponent`
- [ ] `ImportAdminComponent` — verify states already present

### Theme (`core/services/theme.service.ts`)

- [ ] `ThemeService` — `signal<'light' | 'dark'>` initialized from `localStorage`
- [ ] `toggle()` method — flips theme, updates `document.documentElement` class, persists to `localStorage`
- [ ] `theme` signal used in app root to set `class="light-theme"` or `class="dark-theme"` on `<html>`

### Layout — Navbar Theme Toggle (`layout/`)

- [ ] Add theme toggle button (sun/moon icon) to navbar
- [ ] Button calls `ThemeService.toggle()`
- [ ] `aria-label` reflects current state: "Switch to dark theme" / "Switch to light theme"
- [ ] Theme preference survives page reload

### SCSS / Theming

- [ ] Define CSS custom properties for both themes in `styles.scss`
- [ ] Verify contrast ratios ≥ 4.5:1 for all text/background combinations
- [ ] Both themes verified with browser DevTools / contrast checker

### Keyboard Navigation Audit

- [ ] All buttons, links, inputs reachable with Tab key
- [ ] Dialogs/modals trap focus and close on Escape
- [ ] AG Grid keyboard navigation enabled (`suppressKeyboardEvent` not blocking Tab)
- [ ] Chart interactivity accessible (or aria-label describing chart content as fallback)

### Backend — `PUT /api/auth/password`

- [ ] `ChangePasswordRequest` DTO: `{ @NotBlank currentPassword, @NotBlank @Pattern newPassword }` (pattern: ≥ 12 chars,
  at least one uppercase, one lowercase, one digit)
- [ ] `AuthService.changePassword(ChangePasswordRequest, AppUser currentUser)`:
    - [ ] Verify `currentPassword` via bcrypt; throw `BadCredentialsException` (401) if wrong
    - [ ] Validate `newPassword` complexity; throw `PasswordComplexityException` (400) if fails
    - [ ] Encode new password and save
    - [ ] Revoke all refresh tokens for the user
- [ ] Controller: `@PutMapping("/api/auth/password")`, returns `200 OK`
- [ ] Exception mappings in `GlobalExceptionHandler`

### Frontend — Password Change Form

- [ ] New route `/profile` or `/settings` (or modal from navbar)
- [ ] `ChangePasswordComponent` — reactive form: `currentPassword`, `newPassword`, `confirmPassword`
- [ ] Cross-field validator: `newPassword === confirmPassword`
- [ ] Client-side complexity rules displayed as inline hints (show/fail on blur)
- [ ] On success: show toast "Password changed. Please log in again." → call `AuthService.logout()`
- [ ] On wrong current password: show inline error "Current password is incorrect"

### E2E — axe-core

- [ ] Install `@axe-core/playwright` as a dev dependency
- [ ] Add `axe-smoke.spec.ts` that visits each main route and runs axe; fails on severity ≥ "serious"
- [ ] Register in CI pipeline (`package.json` scripts or GitHub Actions)

### Tests

- [ ] `StateHostComponent` unit tests:
    - [ ] Renders skeleton in `loading` state
    - [ ] Renders error block and emits `retry` in `error` state
    - [ ] Renders empty block in `empty` state
    - [ ] Renders `ng-content` in `ready` state
- [ ] `ThemeService` unit tests:
    - [ ] Initial theme loaded from `localStorage`
    - [ ] `toggle()` flips theme and updates `localStorage`

### General

- [ ] This ticket is the last milestone — all other tickets should be complete before E2E axe sweep
- [ ] `rel="noopener noreferrer"` on all `target="_blank"` links (security)
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
