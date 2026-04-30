# AUTH-001 — JWT Authentication Endpoints

## Description

Implement the JWT-based authentication layer:
- `POST /api/auth/login` — accepts credentials, returns an access + refresh token pair.
- `POST /api/auth/refresh` — accepts a refresh token, returns a new token pair.

## Scope

| Layer | Artifact |
|---|---|
| Backend entity | `AppUser` (id, username, password, role) |
| Backend DTOs | `LoginRequest`, `RefreshRequest`, `TokenResponse` |
| Backend service | `AuthService` — bcrypt verification, JWT signing |
| Backend controller | `AuthController` |
| Backend security | `JwtAuthFilter`, `SecurityConfig` |
| Frontend service | `AuthService` (`login()`, `refresh()`, `logout()`) |
| Frontend model | `LoginRequest`, `TokenResponse` |
| Frontend component | `LoginComponent` + reactive form |
| Frontend guard | `AuthGuard`, `AdminGuard` |
| Frontend interceptor | `authInterceptor` — attaches Bearer token |

## Acceptance Criteria

- [x] `POST /api/auth/login` with valid credentials returns `200` and a JWT pair.
- [x] `POST /api/auth/login` with wrong credentials returns `401`.
- [x] `POST /api/auth/login` with a malformed body (missing fields) returns `400`.
- [x] `POST /api/auth/refresh` with a valid refresh token returns a new JWT pair.
- [x] `POST /api/auth/refresh` with an expired/invalid token returns `401`.
- [x] Protected routes redirect unauthenticated users to `/login`.
- [x] `ROLE_ADMIN` guard blocks `ROLE_USER` on `/admin/**` routes.
- [x] Tokens are stored in `sessionStorage` (cleared on tab close).
