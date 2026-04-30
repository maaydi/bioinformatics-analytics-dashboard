# AUTH-001 — Plan

## Tasks

| # | Task | Status |
|---|---|---|
| 1 | Backend: `AppUser` entity + `AppUserRepository` | ✅ done |
| 2 | Backend: `LoginRequest` / `RefreshRequest` / `TokenResponse` DTOs | ✅ done |
| 3 | Backend: `AuthService` — bcrypt verify + JWT sign/verify | ✅ done |
| 4 | Backend: `AuthController` — `/api/auth/login` and `/api/auth/refresh` | ✅ done |
| 5 | Backend: `JwtAuthFilter` + `SecurityConfig` | ✅ done |
| 6 | Frontend: `AuthService` — `login()`, `refresh()`, `logout()`, token storage | ✅ done |
| 7 | Frontend: `LoginComponent` reactive form | ✅ done |
| 8 | Frontend: `authInterceptor` — attaches Bearer token | ✅ done |
| 9 | Frontend: `AuthGuard` + `AdminGuard` | ✅ done |
| 10 | Bug fix: `LoginRequest` password `@Pattern` caused 400 on valid test passwords | ✅ done |
| 11 | Bug fix: Frontend `minLength(8)` validator on login form prevented submission | ✅ done |
| 12 | Bug fix: `JwtUtil.generateAccessToken()` missing `roles` claim — `isAdmin()` always `false` | ✅ done |
| 13 | Bug fix: Frontend `extractRoles()` TODO placeholder — implemented JWT payload decode | ✅ done |
| 14 | Unit tests — backend `AuthService` | 🔲 not-started |
| 15 | Unit tests — frontend `AuthService` + `LoginComponent` | 🔲 not-started |
