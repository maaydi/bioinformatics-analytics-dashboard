# FILTER-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Create DB migration for `saved_filter` table
3. Implement `SavedFilter` entity and `SavedFilterRepository`
4. Implement DTOs and MapStruct mapper
5. Implement `SavedFilterService`
6. Complete `SavedFilterController` (remove stubs)
7. Implement `SavedFiltersComponent` and `SavedFiltersService` in Angular
8. Implement "Save Filters" dialog component
9. Wire apply-filter action to `filtersStore`
10. Write unit tests (backend + frontend)
11. Write integration tests for all three endpoints
12. Update documentation

## Status

- [x] Requirements analyzed
- [x] DB migration created
- [x] Entity and repository implemented
- [x] DTOs and mapper implemented
- [x] SavedFilterService implemented
- [ ] SavedFilterController completed
- [ ] Angular service implemented
- [ ] SavedFiltersComponent implemented
- [ ] Save dialog implemented
- [ ] Apply-filter wiring done
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Database Migration (`V5__saved_filter.sql`)

- [x] `saved_filter` table: `id BIGSERIAL PK`, `user_id BIGINT FK → app_user(id)`, `name VARCHAR(100) NOT NULL`,
  `filter_json JSONB NOT NULL`, `created_at TIMESTAMP NOT NULL DEFAULT now()`
- [x] Unique constraint `ux_saved_filter_user_name (user_id, name)`
- [x] Index on `user_id` for listing queries

### Backend — Entity

- [x] `SavedFilter` entity with `@ManyToOne AppUser owner`, `name`, `filterJson` (`@JdbcTypeCode(SqlTypes.JSON)`),
  `createdAt`

### Backend — Repository

- [x] `SavedFilterRepository extends JpaRepository`
- [x] `findByOwnerOrderByCreatedAtDesc(AppUser owner): List<SavedFilter>`
- [x] `existsByOwnerAndName(AppUser owner, String name): boolean`

### Backend — DTOs

- [x] `SavedFilterDto` — `{ id, name, filterJson, createdAt }`
- [x] `SavedFilterCreateRequest` — `{ @NotBlank @Size(max=100) name, @NotNull @Valid filterJson: GeneSearchRequest }`

### Backend — Mapper

- [x] `SavedFilterMapper` (MapStruct): `toDto(SavedFilter)`, `toEntity(SavedFilterCreateRequest, AppUser owner)`

### Backend — Service

- [x] `SavedFilterService.listForCurrentUser()` — resolve `AppUser` from security context
- [x] `SavedFilterService.create(SavedFilterCreateRequest)` — check uniqueness → throw `DuplicateFilterNameException` (
  409) if duplicate
- [x] `SavedFilterService.delete(Long id)` — check ownership; throw `AccessDeniedException` (403) for non-owner
  non-admin; throw `SavedFilterNotFoundException` (404) if absent

### Backend — Controller

- [ ] Remove stubs in `SavedFilterController`
- [ ] Wire `SavedFilterService`
- [ ] `GET /api/saved-filters` → `200 List<SavedFilterDto>`
- [ ] `POST /api/saved-filters` → `201 SavedFilterDto`
- [ ] `DELETE /api/saved-filters/{id}` → `204 No Content`
- [ ] Exception mapping in `GlobalExceptionHandler`: `DuplicateFilterNameException → 409`,
  `SavedFilterNotFoundException → 404`, `AccessDeniedException → 403`

### Backend — Tests

- [ ] `SavedFilterServiceTest` — unit:
    - [ ] `listForCurrentUser` returns only current user's filters
    - [ ] `create` with duplicate name throws exception
    - [ ] `delete` own filter succeeds
    - [ ] `delete` other user's filter by non-admin throws `AccessDeniedException`
- [ ] `SavedFilterControllerIntegrationTest` — Testcontainers:
    - [ ] `GET /api/saved-filters` — 200 with user's filters
    - [ ] `POST /api/saved-filters` — 201
    - [ ] `POST /api/saved-filters` duplicate — 409
    - [ ] `DELETE /api/saved-filters/{id}` own — 204
    - [ ] `DELETE /api/saved-filters/{id}` other user — 403

### Frontend — Models

- [ ] `saved-filter.model.ts` — `{ id, name, filterJson: GeneSearchRequest, createdAt: string }`
- [ ] `saved-filter-create-request.model.ts` — `{ name: string, filterJson: GeneSearchRequest }`

### Frontend — Service (`features/saved-filters/saved-filters.service.ts`)

- [ ] `list(): Observable<SavedFilter[]>`
- [ ] `create(request: SavedFilterCreateRequest): Observable<SavedFilter>`
- [ ] `delete(id: number): Observable<void>`

### Frontend — `SavedFiltersComponent` (`features/saved-filters/`)

- [ ] `saved-filters.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `saved-filters.component.html` — external template
- [ ] `saved-filters.component.scss`
- [ ] Signal `filters = signal<SavedFilter[]>([])`
- [ ] Load list on init via `SavedFiltersService.list()`
- [ ] `@for` list: name, creation date, filter summary (derived from `filterJson`)
- [ ] "Apply" button: update `filtersStore` with `filterJson`, navigate to `/genes`
- [ ] "Delete" button: call `delete(id)`, update signal list immediately (optimistic)
- [ ] Loading, error, empty states

### Frontend — `SaveFilterDialogComponent`

- [ ] Dialog/modal triggered from `GeneFilterComponent` "Save Filters" button
- [ ] Reactive form: `name` (required, maxLength 100)
- [ ] On submit: call `SavedFiltersService.create()`, close dialog, show success toast

### Tests

- [ ] `SavedFiltersComponent` unit tests:
    - [ ] Lists filters from service
    - [ ] Apply updates store and navigates
    - [ ] Delete removes item from list
- [ ] `SavedFiltersService` unit tests (HttpClientTestingModule):
    - [ ] `list()` sends `GET /api/saved-filters`
    - [ ] `create()` sends `POST /api/saved-filters`
    - [ ] `delete()` sends `DELETE /api/saved-filters/1`

### General

- [ ] Security: users can only see/delete their own filters (enforced backend — not just frontend)
- [ ] Native control flow only
- [ ] AXE checks pass
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
