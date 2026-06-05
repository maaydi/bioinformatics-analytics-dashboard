# FILTER-001 — Implementation Journal

---

## 2026-05-12

### Ticket created

- Created `overview.md` and `plan.md` from backlog stories US-20, US-21.
- Reviewed existing backend: `SavedFilterController.java` exists as a stub — all methods throw
  `UnsupportedOperationException`. No service, entity, repository, or DTOs found in `savedfilter` package.
- Reviewed existing frontend: `features/saved-filters/saved-filters.component.{ts,html,scss}` and
  `saved-filters.service.ts` exist; implementation status to be verified.
- DB migration for `saved_filter` table is absent — needs `V5__saved_filter.sql`.
- Implementation not yet started.

---

## 2026-06-05

### Code Review Completed

**Branch State**: `implementation/filter-001` (2 commits ahead of `origin/implementation/filter-001`)

**Latest Commits**:

- `31aac5f` FILTER-001 Toast when save/delete filter
- `d4b0f9e` FILTER-001 Dialog/modal triggered "Save Filters" button

**Files Changed** (last 2 commits):

- `documentation/implementation/FILTER-001/plan.md` (28 lines modified)
- `frontend/src/app/features/genes/gene-filter/gene-filter.component.html` (8 lines added)
- `frontend/src/app/features/genes/gene-filter/gene-filter.component.ts` (69 lines added)
- `frontend/src/app/features/genes/save-filters-dialog/save-filters-dialog.component.ts` (38 lines added)
- `frontend/src/app/features/genes/save-filters-dialog/save-filters-dialog.component.html` (19 lines added)
- `frontend/src/app/features/genes/save-filters-dialog/save-filters-dialog.component.scss` (12 lines added)
- `frontend/src/app/features/saved-filters/saved-filters.component.ts` (21 lines added)
- `frontend/src/app/features/saved-filters/saved-filters.component.spec.ts` (2 lines modified)
- `frontend/src/styles.scss` (49 lines added)

**Code Review Report**: `documentation/implementation/FILTER-001/code-review-report.md`

**Key Findings**:

**Critical Blockers**:

1. **Spec Violation**: POST /api/saved-filters returns 200 OK instead of 201 Created -- FIXED

- File: `SavedFilterController.java:47`
- Impact: Violates REST convention and API contract
- Comment indicates tests are written to match wrong code, not spec

2. **Error Message Bug**: Delete failure shows "Failed to save" instead of "Failed to delete" -- FIXED

- File: `SavedFiltersComponent.ts:327`
- Impact: Misleading user feedback

🟠 **High Priority**:

3. Controller tests expect wrong HTTP status (200 instead of 201) -- FIXED

- Files: `SavedFilterControllerTest.java` (3 test methods)
- Must be updated when fix #1 is applied

