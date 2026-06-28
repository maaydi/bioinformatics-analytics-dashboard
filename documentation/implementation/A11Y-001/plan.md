# A11Y-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Implement `ThemeService` + theme toggle in navbar
3. Audit and fix WCAG AA color contrast in both themes
4. Audit and fix keyboard navigation across all pages
5. Implement `PUT /api/auth/password` endpoint (backend)
6. Implement password change form (frontend profile/settings page)
7. Add `@axe-core/playwright` to e2e suite
8. Write unit tests for `ThemeService` and `StateHostComponent`
9. Update documentation

## Status

- [x] Requirements analyzed
- [x] ThemeService implemented
- [x] Theme toggle in navbar implemented
- [x] WCAG AA contrast verified and fixed
- [x] Keyboard navigation audited and fixed
- [x] PUT /api/auth/password implemented
- [ ] Password change form implemented
- [ ] axe-core e2e configured
- [ ] Unit tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Theme (`core/services/theme.service.ts`)

- [x] `ThemeService` — `signal<'light' | 'dark'>` initialized from `localStorage`
- [x] `toggle()` method — flips theme, updates `document.documentElement` class, persists to `localStorage`
- [x] `theme` signal used in app root to set `class="light-theme"` or `class="dark-theme"` on `<html>`

### Layout — Navbar Theme Toggle (`layout/`)

- [x] Add theme toggle button (sun/moon icon) to navbar
- [x] Button calls `ThemeService.toggle()`
- [x] `aria-label` reflects current state: "Switch to dark theme" / "Switch to light theme"
- [x] Theme preference survives page reload

### SCSS / Theming

- [x] Define CSS custom properties for both themes in `styles.scss` (using material variables across _design-system)
- [x] Verify contrast ratios ≥ 4.5:1 for all text/background combinations
- [x] Both themes verified with browser DevTools / contrast checker

### Keyboard Navigation Audit

- [x] All buttons, links, inputs reachable with Tab key
- [x] Dialogs/modals trap focus and close on Escape
- [x] AG Grid keyboard navigation enabled (`suppressKeyboardEvent` not blocking Tab)
- [x] Chart interactivity accessible (or aria-label describing chart content as fallback)

### Backend — `PUT /api/auth/password`

- [x] `ChangePasswordRequest` DTO: `{ @NotBlank currentPassword, @NotBlank @Pattern newPassword }` (pattern: ≥ 12 chars,
  at least one uppercase, one lowercase, one digit)
- [x] `AuthService.changePassword(ChangePasswordRequest, AppUser currentUser)`:
  - [x] Verify `currentPassword` via bcrypt; throw `BadCredentialsException` (401) if wrong
  - [x] Validate `newPassword` complexity; throw `PasswordComplexityException` (400) if fails
  - [x] Encode new password and save
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

- [ ] `ThemeService` unit tests:
    - [ ] Initial theme loaded from `localStorage`
    - [ ] `toggle()` flips theme and updates `localStorage`

### General

- [ ] This ticket is the last milestone — all other tickets should be complete before E2E axe sweep
- [ ] `rel="noopener noreferrer"` on all `target="_blank"` links (security)
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
