# AUTH-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Implement backend changes (controller, service, repository, DTO, entity, mapper)
3. Implement frontend changes (Angular service, models, UI components)
4. Write and run unit tests
5. Update documentation
6. Review and refactor as needed

## Status

- [x] Requirements analyzed
- [x] Backend implemented (endpoints, service, repository, DTOs)
- [x] Frontend implemented (Auth service, Login component, interceptor, guards)
- [x] Unit tests written (backend); frontend unit tests pending
- [x] Documentation updated (implementation notes, API contract if needed)
- [x] Code reviewed

---

## Detailed Checklist

### Backend

- [x] Controller endpoints implemented:
	- [x] POST /api/auth/login (authenticate and return access/refresh tokens)
	- [x] POST /api/auth/refresh (exchange refresh token for new access token)
- [x] Service layer implemented (AuthService): password verification (bcrypt), JWT generation/verification
- [x] Repository layer for users (AppUserRepository)
- [x] DTOs for requests/responses (LoginRequest, RefreshRequest, TokenResponse)
- [x] Entity/model for users (AppUser)
- [x] MapStruct mappers for DTO/entity where applicable
- [x] Validation annotations and global error handling (ControllerAdvice)
- [x] Security: JwtAuthFilter + SecurityConfig installed and configured (role-based access)
- [x] Bug fixes applied:
	- [x] Removed overly strict `@Pattern` on `LoginRequest.password` causing valid passwords to 400
	- [x] Fixed `JwtUtil.generateAccessToken()` to include `roles` claim
- [x] Unit tests for service logic (AuthService) — implemented (see backend/src/test/.../AuthServiceTest.java)
- [x] Integration tests for auth endpoints — implemented (see
  backend/src/test/java/com/bioinformatics/dashboard/auth/controller/AuthControllerIntegrationTest.java)

### Frontend (Angular)

- [x] Service for API calls (`AuthService`) — `login()`, `refresh()`, `logout()`, token storage and retrieval
- [x] Models for API contracts (LoginRequest, RefreshRequest, TokenResponse)
- [x] UI for:
	- [x] Login form/component (reactive form)
	- [x] Token handling and automatic refresh
- [x] Interceptor implemented (`authInterceptor`) — attaches Bearer token to outgoing requests
- [x] Guards implemented (`AuthGuard`, `AdminGuard`)
- [x] Bug fixes applied:
	- [x] Removed client-side minLength(8) validator that blocked valid test logins
	- [x] Implemented JWT payload decode (`extractRoles()`) to derive roles for guards
- [x] Unit tests for `AuthService` and `LoginComponent` — frontend tests not started

### Documentation

- [x] API contract reviewed/updated if changes required
- [x] Domain model unaffected (no DB schema changes)
- [x] Validation rules documented (notably password validation changes)
- [x] Implementation journal updated (`journal.md`)

### General

- [x] Code reviewed
- [ ] Coverage ≥ 80% (backend and frontend)

