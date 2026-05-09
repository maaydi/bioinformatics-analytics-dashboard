# AUTH-001 — Implementation Journal

---

## 2026-04-30

### Initial implementation (prior to session)

- Created `AppUser` entity and `AppUserRepository`.
- Implemented `AuthService` with bcrypt password verification and JJWT HS256 token signing.
- Created `AuthController` exposing `POST /api/auth/login` and `POST /api/auth/refresh`.
- Configured `SecurityConfig` and `JwtAuthFilter`.
- Created `LoginComponent` with a reactive form (`username` + `password` fields).
- Implemented `authInterceptor` to attach the Bearer token to every outgoing request.
- Added `AuthGuard` and `AdminGuard` to protect frontend routes.

### Bug — HTTP 400 on login (`POST /api/auth/login`)

**Symptom:** The login form returned "Invalid credentials. Please try again." for valid test accounts (`user_test / password`, `admin_test / admin123`). The backend returned `400 Bad Request`.

**Root cause:** `LoginRequest.java` applied a `@Pattern` constraint requiring the password to contain at least one uppercase letter, one lowercase letter, and one digit. This is a registration-time complexity rule, not a login rule. The test passwords `password` and `admin123` did not satisfy the pattern, so `@Valid` rejected the request before the service layer could perform bcrypt comparison.

A secondary issue: the Angular `LoginComponent` applied `Validators.minLength(8)` on the password field, which would block submission for any password shorter than 8 characters — including passwords that are stored and valid in the database.

**Fix 1 — Backend** (`LoginRequest.java`):
- Removed `@Size(min = 8)` and `@Pattern` from the `password` field.
- Kept only `@NotBlank` — presence validation is sufficient at login; correctness is enforced by bcrypt in the service.

**Fix 2 — Frontend** (`login.component.ts`):
- Removed `Validators.minLength(8)` from the `password` control.
- Kept only `Validators.required`.

### Additional fix — Angular `NG02801` warning

**Symptom:** Angular logged `NG02801: HttpClient is not configured to use fetch APIs`.

**Fix** (`app.config.ts`): Added `withFetch()` to `provideHttpClient(withFetch(), withInterceptors([authInterceptor]))`.

### Bug — `ROLE_ADMIN` guard blocked admins (`isAdmin()` always returned `false`)

**Symptom:** The `adminGuard` was redirecting all users — including admins — away from `/admin/import`.

**Root cause 1 — Backend:** `JwtUtil.generateAccessToken()` only included `type: "access"` in the extra claims. No `roles` claim was emitted, so the JWT payload carried no authority information.

**Root cause 2 — Frontend:** `AuthService.extractRoles()` had a `// TODO` placeholder that returned `[]` unconditionally, meaning `isAdmin()` always returned `false`.

**Fix 1 — Backend** (`JwtUtil.java`):
- Added `roles` claim to the access token: a comma-separated string of authority names derived from `UserDetails.getAuthorities()`.

**Fix 2 — Frontend** (`auth.model.ts`):
- Added `roles: string` field to `JwtPayload` interface.

**Fix 3 — Frontend** (`auth.service.ts`):
- Implemented `extractRoles()`: base64-decodes the JWT payload segment, parses JSON, splits the `roles` comma-separated string into a `UserRole[]`.

All eight acceptance criteria are now met.

Added `spring-boot-devtools` dependency (`<optional>true</optional>`) to `pom.xml`.
Updated `start-dev.sh` to use `mvn spring-boot:run -DskipTests` instead of `mvn clean install spring-boot:run -DskipTests` to avoid the unnecessary install phase on every dev startup.

## 2026-05-08

### Unit tests added for AuthService

- Added unit tests for `AuthService` to validate login and refresh flows and error handling:
	- `backend/src/test/java/com/bioinformatics/dashboard/auth/service/AuthServiceTest.java`
		- login_success: verifies AuthenticationManager is invoked and JWT pair is returned
		- refresh_success: validates refresh-token path issues a new JWT pair
		- refresh_invalid: verifies invalid refresh token triggers BadCredentialsException

### Integration tests added for authentication endpoints

- Added integration tests that exercise the real HTTP endpoints and the persistence layer using Testcontainers (
  PostgreSQL via the ContainerDatabaseDriver):
	- `backend/src/test/java/com/bioinformatics/dashboard/auth/controller/AuthControllerIntegrationTest.java`
	- Tests cover:
		- `POST /api/auth/login` — success path returns access + refresh tokens and expiry
		- `POST /api/auth/refresh` — exchanging a valid refresh token returns a new token pair
