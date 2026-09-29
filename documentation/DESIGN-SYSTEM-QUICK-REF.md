# Frontend Design & UX Guidelines

This document is the authoritative reference for visual consistency, usability, accessibility, and ergonomics across the
frontend application. It documents the design system, conventions, and patterns already present in the codebase rather
than introducing a new style.

## 1. Introduction & Design Principles

The application is built on **Angular 21 (standalone, zoneless) + Angular Material 3**, with a custom design-token layer
(`frontend/src/styles/_design-system.scss` and `_theme-variables.scss`) layered on top of Material's own system tokens.
This document captures the conventions that are *already dominant* in the codebase and makes them mandatory, so new work
reinforces the existing system instead of adding a new one.

**Principles observed and now formalized:**

- **Compose Material, don't reinvent it.** Every interactive primitive (button, input, chip, spinner, dialog, tooltip,
  select) is Angular Material. Custom components (`InputComponent`, `RangeInputComponent`,
  `GenericAutocompleteComponent`, …) wrap Material + `ControlValueAccessor`, they don't replace it with bespoke HTML.
- **Signals-first, RxJS at the edges.** State is modeled with `input()`/`output()`/`computed()`/`linkedSignal`; RxJS is
  reserved for async orchestration (debounced autocomplete queries, `takeUntilDestroyed` cleanup), matching
  `senior-angular-dev.agent.md`.
- **Server-driven data.** Filtering, sorting, and pagination for large datasets (gene search) are pushed to the
  backend — the client renders whatever page it's given and re-queries on change. This is the model for any new
  data-heavy view; don't build client-side sort/filter for paginated server data.
- **Reactive Forms everywhere.** All user input goes through `FormGroup`/`FormControl`, never template-driven forms.
- **Design tokens exist and should be the only source of color, spacing, radius, and breakpoints**
  (`_design-system.scss`, `_theme-variables.scss`) — components that bypass them with raw hex/px/rem values are
  deviations from the standard, not an accepted alternate style (see §7).
- **Maturity varies by area.** The gene search/filter workflow (`genes-page`, `gene-filter`, `active-filters`,
  `global-search`, `result-header`, `genes-table`) is the most consistent, well-modeled part of the app and should be
  the reference implementation new features are modeled after. The dashboard/analytics area is where most token- and
  pattern-bypassing has crept in — treat it as needing alignment, not as an alternate convention.

## 2. Style Guide

### Colors

- Use CSS custom properties only: `--color-*` / `--chip-*` / `--status-*` from `_theme-variables.scss`, or Material's
  own `--mat-sys-*` tokens. Never write a bare hex/rgb value as the primary color — hex is only acceptable as the
  fallback arm of `var(--token, #fallback)`.
- Dark/light mode is a **class toggle** (`.light-theme` / `.dark-theme` on `<html>`, managed by `ThemeService`), not
  `prefers-color-scheme`. Any new themed value must be defined under both class blocks in `_theme-variables.scss`, not
  computed ad hoc per component.

### Typography

- Use the text mixins in `_design-system.scss` (`text-headline-sm`, `text-title-md/sm`, `text-body-md/sm`,
  `text-label-md/sm`, `text-caption`, `text-mono`) instead of hand-set `font-size`/`font-weight`/`line-height`
  combinations.
- Size scale: `xs` 12px, `sm` 14px, `base` 16px, `lg` 18px, `xl` 20px. Weights: regular 400, medium 500, semibold 600,
  bold 700. Line heights: tight 1.25, normal 1.5, relaxed 1.75.
- One base font family for the whole app — do not introduce a second body typeface (see §7 for the current Roboto/Inter
  conflict that needs resolving, not extending).

### Spacing & Sizing

- Use the spacing scale exclusively: `$spacing-xs` 4px, `sm` 8px, `md` 12px, `lg` 16px, `xl` 24px, `2xl` 32px. Use
  `$radius-xs/sm/md/lg/full` (2/4/6/8/999px) for corner radii.
- A raw `rem`/`px` margin, padding, or gap value in a component's `.scss` is only acceptable when no existing token fits
  **and** you've confirmed that with a design review — it is not a shortcut to skip the scale.

### Responsive Breakpoints

- Use the `ds.respond-to('sm'|'md'|'lg'|'xl'|'2xl')` / `ds.respond-up-to(...)` mixins and their backing variables
  (`$breakpoint-sm` 640px, `md` 1024px, `lg` 1366px, `xl` 1920px, `2xl` 2560px, tuned for HD/FHD desktop monitors).
  Never hand-write a raw `@media (min-width: …px)` query with a bespoke breakpoint value.

### Accessibility Requirements

- Every interactive or status-bearing element gets appropriate ARIA: `aria-label` on icon-only controls and landmark
  regions, `role="status"`/`"alert"` + `aria-live` on loading/error messaging, `aria-current="page"` on the active nav
  link, `[attr.aria-busy]` while a view is loading. This is already the dominant pattern (navbar, breadcrumbs, loading
  spinner, genes-table) — keep extending it, don't regress it.
- Use the shared `focus-ring` / `:focus-visible` styling from `_design-system.scss`; don't remove or override default
  focus outlines.
- Any dialog (`MatDialog`) must trap focus and be dismissible via Escape and a visible close action.
- Color is never the only signal for state (error/success/warning) — pair it with an icon, label, or text, consistent
  with existing status chip patterns.

### Interaction Patterns

- Hover/press transitions: standardize on **`150ms ease-in-out`** for micro-interactions (button/row hover, chip
  toggle) — this is already the single most common value in the codebase; new code should not introduce another
  duration/easing pair for the same kind of interaction.
- Debounce free-text search inputs (≈300ms) before firing a query; submit-triggered filter panels (the main gene filter
  form) apply on an explicit "Apply Filters" action, not per-keystroke — pick one of these two models per control and
  don't mix them within the same form.
- Destructive or state-changing actions on a saved/named entity (delete a saved filter, remove an import) must go
  through `ConfirmDialogComponent`, not a native `confirm()` or an unconfirmed immediate action.

## 3. Component Catalog

Reusable components live in `frontend/src/app/shared/components/`. Feature-level components (`genes-table`,
`gene-filter`, `active-filters`, `global-search`, `result-header`) are built generically enough to reuse and should be
treated as part of the catalog even though they currently sit under `features/genes/`.

| Component                                 | Purpose                                                                   | Use when / avoid when                                                                                    | Key API                                                                                                         | Notes                                                                                        |
|-------------------------------------------|---------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `LoadingSpinnerComponent`                 | Standard loading indicator (`mat-spinner`, `role="status"`)               | Use for **every** async loading state. Avoid building a bespoke skeleton/spinner.                        | No inputs/outputs                                                                                               | Reused 10+ places; `genes-table` currently doesn't use it (§7)                               |
| `EmptyStateComponent`                     | Zero-results placeholder (icon + message)                                 | Use for **every** "no data" state                                                                        | `message`, `icon` inputs                                                                                        | Built but currently unused anywhere — adopt it, don't hand-roll another empty-state div (§7) |
| `ConfirmDialogComponent`                  | Generic yes/no confirmation modal                                         | Use before any destructive action (delete, overwrite)                                                    | Data via `MAT_DIALOG_DATA` `{title, message, confirmLabel, cancelLabel}`; resolves `boolean`                    | Built but not yet wired into delete flows — use it, don't add a native `confirm()`           |
| `BreadcrumbsComponent`                    | Hierarchical breadcrumb nav                                               | Use on any detail page reached from a list                                                               | `items: BreadcrumbItem[]` (`label`, `routerLink?`, `isActive?`)                                                 | Full ARIA pattern already implemented — model other nav on this                              |
| `InputComponent`                          | Text input wrapping `MatFormField`/`MatInput` as a `ControlValueAccessor` | Use for any text field inside a Reactive Form                                                            | `title`, `isSearch`, `placeholder`, `hintLabel` inputs; value via CVA                                           | —                                                                                            |
| `RangeInputComponent`                     | Min/max numeric range CVA + validator                                     | Use for any numeric range filter                                                                         | `label`, `hintLabel`, `minPlaceholder`, `maxPlaceholder`; emits `{minGreaterThanMax:true}`                      | —                                                                                            |
| `GenericAutocompleteComponent`            | Server-backed single/multi-select autocomplete CVA                        | Use for any lookup-style filter field                                                                    | `field` (required), `multiSelect`, `placeholder`                                                                | Debounced via RxJS (`switchMap`/`debounceTime`)                                              |
| `LimitSelectorComponent`                  | "Top N" numeric picker with presets                                       | Use where a chart/list needs a user-adjustable result cap                                                | `min`, `max`, `defaultValue` (all required); emits `limitChange`                                                | —                                                                                            |
| `DashboardKpiCardComponent`               | Single KPI stat card                                                      | Use for any single-number summary metric                                                                 | `title`, `label`, `value` (required), `unit`                                                                    | —                                                                                            |
| `GenesTableComponent`                     | AG Grid wrapper for paginated/sortable result sets                        | Use for large, server-paginated tabular data. Avoid for small in-app lists (use `mat-table` instead, §7) | `data`, `errorMessage`, `loading`, `chipsCount` inputs; `updateSortDirection`, `rowClick`, `retryClick` outputs | Sorting delegated server-side; pairs with `ResultHeaderComponent` for pagination             |
| `GeneFilterComponent`                     | Reactive-form filter panel                                                | Use as the reference implementation for any multi-field filter panel                                     | `displayMode`, `title`, `showSubmitButton`, `value` inputs; `filterChange`, `filterClear` outputs               | Reused in two contexts (`genes-page`, `compare`) via `displayMode`                           |
| `ActiveFiltersComponent`                  | Renders active filters as removable chips + CSV export trigger            | Use anywhere filters need a visual, removable summary                                                    | `filters`, `isExportinProgress` inputs; `exportCsv`, `filterRemoved`, `setChipsCount` outputs                   | Uses `mat-chip-set`; shares `.criteria-chip-set` styling with `saved-filters`                |
| `GlobalSearchComponent`                   | Debounced free-text search box                                            | Use for a single primary search entry point per page                                                     | `filters` input; `filterChange`, `retrySearch` outputs                                                          | ≈300ms debounce via `setTimeout`                                                             |
| `ResultHeaderComponent`                   | Pagination bar (`MatPaginator` over a `PagedResponse`)                    | Use alongside any server-paginated result set                                                            | `data` input; `updatePage` output                                                                               | Pairs with `GenesTableComponent`                                                             |
| `SaveFiltersDialogComponent`              | Modal to name and persist a filter snapshot                               | Use as the model for "save this configuration" flows                                                     | No `@Input`/`@Output`; data via `MatDialogRef`                                                                  | Reports success/failure via `MatSnackBar`                                                    |
| `AccountSettingsComponent`                | User account settings modal                                               | Feature-specific, not a generic pattern                                                                  | Data via `MatDialogRef`                                                                                         | Opened from `NavbarComponent`                                                                |
| `NavbarComponent` / `MainLayoutComponent` | App shell: top toolbar + router outlet                                    | Singleton — don't duplicate; extend in place for new top-level nav items                                 | No inputs/outputs                                                                                               | No responsive/mobile nav yet (§7)                                                            |

**Not in the catalog / avoid copying:** the `dashboard-*` and `compare-*` chart components under
`shared/components/analytics/` are domain-specific visualizations (bound to one dataset each), not generic UI
primitives — use them as a model for the loading/error/empty **structure** they share, not as a reusable component to
instantiate elsewhere.

## 4. Component Creation Checklist

Before adding a new component, verify all of the following:

- [ ] **Reuse check** — does something in §3's catalog (`shared/components/`, or `genes-table`/`gene-filter`/
  `active-filters`/`global-search`/`result-header`) already do this? A near-duplicate (e.g. another empty state, another
  retry button) is a defect, not a new component.
- [ ] **Naming** — does it follow the existing `app-<kebab-case>` selector convention and folder-per-component layout
  (`.ts`/`.html`/`.scss`/`.spec.ts`)?
- [ ] **Tokens** — does it use `_design-system.scss`/`_theme-variables.scss` tokens for color, spacing, radius, and
  breakpoints instead of hardcoded values?
- [ ] **Mobile layout** — does it use `ds.respond-to()`/`respond-up-to()` and remain usable at the `sm` breakpoint,
  rather than only being designed for desktop/HD?
- [ ] **Loading, empty, and error states** — does it use `LoadingSpinnerComponent` and `EmptyStateComponent` for those
  states (not a new bespoke skeleton or placeholder), and a `mat-stroked-button` retry action consistent with the chart
  components' pattern?
- [ ] **Accessibility** — does it have appropriate ARIA roles/labels, visible focus (`focus-ring` mixin), and, if it's a
  dialog, focus trapping and Escape-to-close?
- [ ] **Visual consistency** — does it look and behave like the closest existing analog (same button variant, same chip
  styling, same error markup)?
- [ ] **Change detection & state model** — is it `OnPush`, and does it use signals (`input()`/`output()`/`computed()`)
  rather than legacy `@Input()`/`@Output()` or unnecessary RxJS?
- [ ] **Forms** — if it accepts user input, is it a Reactive Forms `ControlValueAccessor` (like `InputComponent`/
  `RangeInputComponent`), not a template-driven form?

## 5. UX Consistency Rules

1. **Similar actions must look and behave the same everywhere.** One retry action = one visual treatment
   (`mat-stroked-button`, consistent icon and label) across genes-table, charts, and import history — not three
   different implementations.
2. **Similar data must be displayed using the same pattern.** A paginated result set uses `MatPaginator` with the
   numbered-page style already established (`ResultHeaderComponent`, import history, saved filters); a table of items
   uses either AG Grid (large, server-paginated/sorted data) or `mat-table` (small, in-app lists) — never a hand-rolled
   plain `<table>`.
3. **Feedback must be immediate for user actions.** Any async action (export, save, delete) shows a loading/disabled
   state on the triggering control and a success/error `MatSnackBar`, matching the pattern already used in
   `GeneFilterComponent`'s save flow.
4. **Error messages must be actionable.** An error state includes a retry action wherever the underlying operation is
   retryable (as charts and import already do), not just a static message.
5. **Primary actions must always be visually identifiable.** One primary action per view/panel, styled
   `mat-raised-button`/`mat-flat-button`; secondary actions use `mat-stroked-button`; tertiary/inline actions use
   `mat-button`. Don't mark two actions in the same context as equally prominent.
6. **Navigation patterns must remain consistent across screens.** All top-level navigation lives in the single
   `NavbarComponent` toolbar with `routerLinkActive`; a new top-level feature is added there, not via a separate ad hoc
   nav element.
7. **Filter and search interactions follow the established model.** Free-text search is debounced (≈300ms); structured
   multi-field filtering is submit-triggered via an explicit "Apply Filters" action; active filters are shown as
   removable chips (`ActiveFiltersComponent`'s `.criteria-chip-set` pattern). Don't mix instant-apply and
   submit-triggered controls within the same filter panel.
8. **Form validation is shown one way.** Every invalid field shows a Material `mat-error` inside its `mat-form-field`,
   gated by `control.touched && control.hasError('x')` — not a standalone `<p class="error-text">` or an ungated error.
9. **Destructive actions are confirmed.** Delete/overwrite/remove actions on named or persisted data go through
   `ConfirmDialogComponent` before executing.

## 6. Contribution Instructions: Before Creating a New Component

1. **Search for an existing component to reuse** — check `frontend/src/app/shared/components/` and the §3 Component
   Catalog first. Building a second empty-state or a second retry button is the single most common mistake found in this
   codebase today (§7) — don't repeat it.
2. **Search for an existing pattern implementing the same behavior** — even if no component matches exactly, a feature
   component (e.g. `gene-filter`, `active-filters`) may already solve the same interaction problem. Copy its structure
   rather than inventing a new one.
3. **Reuse design tokens and shared styles** — `@use "design-system" as ds;` for spacing/typography/breakpoint mixins,
   and the `--color-*`/`--mat-sys-*` custom properties for color. No new raw hex/px/rem values for things the scale
   already covers.
4. **Validate accessibility and responsiveness** — run through the accessibility and mobile-layout items in the §4
   checklist before opening a PR, not after.
5. **Ensure consistency with documented UX patterns** — check the component against §5's UX Consistency Rules (button
   hierarchy, error/loading/empty patterns, confirmation on destructive actions).
6. **Add documentation and usage examples** — add the new component to the §3 Component Catalog table (purpose, when to
   use/avoid, key API) so the next contributor finds it in step 1 instead of rebuilding it.

## 7. Inconsistencies Found & Recommendations

| Issue                                                                                                         | Where found                                                                                                                                                                         | Recommendation                                                                                                                                         |
|---------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Base font-family conflict: Material theme sets Roboto, but `_design-system.scss` mixins apply Inter           | `styles.scss`, `_design-system.scss:13-14`                                                                                                                                          | Pick one typeface and apply it consistently through both the Material theme config and the design-system mixins                                        |
| Material theme instantiated twice (root theme in `styles.scss` + light/dark theme in `_theme-variables.scss`) | `styles.scss:1-30`, `_theme-variables.scss:5-26`                                                                                                                                    | Consolidate into a single theme definition source                                                                                                      |
| Spacing scale bypassed with raw rem/px values                                                                 | `dashboard.component.scss`, `dashboard-kpi-card.component.scss`, `navbar.component.scss:91`                                                                                         | Migrate to `ds.$spacing-*`; extend the scale (e.g. add a 6px step) if a genuinely new value is needed, rather than hand-picking one                    |
| Two incompatible breakpoint systems: `ds.$breakpoint-*` mixins vs. ad hoc raw `@media` px values              | `dashboard.component.scss`, `analytics.component.scss`, several `dashboard-*` chart components                                                                                      | Migrate every ad hoc `@media` query to `ds.respond-to()`/`respond-up-to()`                                                                             |
| Hover/transition timing fragmented across 6+ duration/easing combinations for equivalent interactions         | `genes-table`, `dashboard-*`, `compare-evidence-level`, `styles.scss:235`                                                                                                           | Introduce `$transition-fast/base` tokens in `_design-system.scss` and migrate existing components to them                                              |
| `EmptyStateComponent` built but never used; every feature hand-rolls its own empty state (5+ variants)        | `genes-table`, `import-admin`, `saved-filters`, `gene-detail`, `analytics`                                                                                                          | Adopt `EmptyStateComponent` everywhere; remove the duplicated inline markup                                                                            |
| `ConfirmDialogComponent` and `CustomHeaderSortComponent` built but unused                                     | `shared/components/confirm-dialog`, `shared/components/custom-header-sort`                                                                                                          | Wire `ConfirmDialogComponent` into delete/destructive flows (saved filters); either wire up or remove `CustomHeaderSortComponent`                      |
| `genes-table` uses a bespoke skeleton loader instead of `LoadingSpinnerComponent`                             | `genes-table.component.html:24-31`                                                                                                                                                  | Standardize on `LoadingSpinnerComponent`, or promote a shared skeleton component if skeleton loading is the intended direction for tables specifically |
| Three different "Retry" button implementations for the same action                                            | `genes-table` (plain `<button>`), chart components (`mat-stroked-button`), `import-admin` (`mat-stroked-button` + custom class)                                                     | Standardize on one `mat-stroked-button` + icon treatment                                                                                               |
| Field-validation errors rendered 3 different ways                                                             | `save-filters-dialog`/`account-settings` (`mat-error` in field), `gene-filter` (`mat-error` outside field + custom class), `login`/`global-search` (plain `<p class="error-text">`) | Standardize on Material `mat-error` inside `mat-form-field`, gated by `.touched && hasError()`                                                         |
| `error-banner` class reused with inconsistent icon/`role="alert"` presence                                    | `import-admin` (icon + `role="alert"`), `analytics` (text only), `account-settings` (no `role`)                                                                                     | Extract one shared error-banner partial with fixed markup and required `role="alert"`                                                                  |
| Native `<select>` and `mat-select` used interchangeably for the same dropdown interaction                     | `analytics.component.html`, `import-admin.component.html` (native `<select>`) vs. `gene-filter.component.html` (`mat-select`)                                                       | Standardize on `mat-select`                                                                                                                            |
| Three table technologies with no shared "data table" abstraction                                              | AG Grid (`genes-table`), `mat-table` (`import-admin`), plain `<table>` (`saved-filters`, `gene-detail`)                                                                             | Use AG Grid only for large server-paginated/sorted data; use `mat-table` for everything else; migrate the plain `<table>` instances                    |
| `MatSidenavModule` imported but unused; no responsive/mobile navigation exists                                | `main-layout.component.ts:2`                                                                                                                                                        | Either implement a responsive nav drawer for small viewports, or remove the unused import                                                              |
| No skip links or focus traps despite otherwise solid ARIA labeling                                            | App-wide; dialogs (`MatDialog` usages)                                                                                                                                              | Add a skip-to-content link in `MainLayoutComponent`; enable focus trapping on all dialogs                                                              |
| Instant-apply and submit-triggered controls mixed within the same filter form                                 | `gene-filter.component.ts:225-232` (evidence-level checkboxes apply instantly; the rest of the form requires "Apply Filters")                                                       | Pick one interaction model per form, or visually separate instant filters from submit-triggered ones                                                   |

**Last Updated**: Sep 29, 2026  
**Design System Version**: 2.0  
**Target Displays**: HD (1366×768), FHD (1920×1080)

