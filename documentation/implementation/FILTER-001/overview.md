# FILTER-001 — Saved Filter Feature

## Description

Implement the Saved Filters feature end-to-end:

- `GET /api/saved-filters` — list the authenticated user's saved filter sets.
- `POST /api/saved-filters` — persist a named filter set (user-scoped, unique name per user).
- `DELETE /api/saved-filters/{id}` — remove own filter set (ADMIN may delete any).
- Angular `SavedFiltersComponent` listing saved sets and applying them to the Gene Explorer.
- "Save Filters" dialog accessible from the Gene Explorer filter panel.

## Scope

| Layer              | Artifact                                                                               |
|--------------------|----------------------------------------------------------------------------------------|
| DB migration       | `V5__saved_filter.sql` — `saved_filter` table with unique `(user_id, name)` constraint |
| Entity             | `SavedFilter` entity                                                                   |
| Repository         | `SavedFilterRepository`                                                                |
| DTOs               | `SavedFilterDto`, `SavedFilterCreateRequest`                                           |
| Mapper             | `SavedFilterMapper` (MapStruct)                                                        |
| Service            | `SavedFilterService`                                                                   |
| Controller         | `SavedFilterController` — remove stubs                                                 |
| Frontend service   | `saved-filters.service.ts`                                                             |
| Frontend model     | `SavedFilter` Angular model                                                            |
| Frontend component | `saved-filters.component` — list view; `save-filter-dialog.component` — dialog         |

## Acceptance Criteria

- [ ] `GET /api/saved-filters` returns `200` with list of own saved filters (empty array if none).
- [ ] `POST /api/saved-filters` with valid name + filterJson returns `201` Created.
- [ ] `POST /api/saved-filters` with blank name returns `400`.
- [ ] `POST /api/saved-filters` with duplicate name (same user) returns `409`.
- [ ] `DELETE /api/saved-filters/{id}` on own filter returns `204`.
- [ ] `DELETE /api/saved-filters/{id}` on another user's filter by non-admin returns `403`.
- [ ] `DELETE /api/saved-filters/{id}` for non-existent id returns `404`.
- [ ] All endpoints return `401` without JWT.
- [ ] Frontend: saved filters list shows name, filter summary, creation date.
- [ ] Frontend: clicking a saved filter applies it to the Gene Explorer (`filtersStore` update + navigation).
- [ ] Frontend: deleting a saved filter removes it immediately from the list.
- [ ] Frontend: "Save Filters" dialog validates name (required, max 100 chars).
- [ ] Unit tests for `SavedFilterService` and `SavedFiltersComponent`.

## References

- `documentation/api-contract.md` §4 — Saved Filters Endpoints
- `documentation/domain-model.md` — `saved_filter` table
- `documentation/validation-rules.md` — saved filter validation rules
- `documentation/plan.md` — US-20, US-21
