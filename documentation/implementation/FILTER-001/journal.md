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
