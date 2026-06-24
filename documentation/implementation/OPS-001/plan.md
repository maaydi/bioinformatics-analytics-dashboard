# OPS-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Create DB migration for `audit_log` table
3. Implement `AuditLog` entity, repository, and `AuditService`
4. Wire audit recording to auth, import, and saved-filter delete events
5. Configure Spring Boot Actuator (liveness + readiness + Flyway check)
6. Implement pagination cap (size ≤ 200) on all list endpoints
7. Add Bucket4j dependency and implement `RateLimitFilter`
8. Write unit tests for `AuditService`
9. Write integration tests for rate-limiting and pagination cap
10. Update documentation

## Status

- [x] Requirements analyzed
- [x] DB migration created
- [x] AuditLog entity + repository + service implemented
- [x] Audit hooks wired
- [x] Actuator configured
- [x] Pagination cap implemented
- [x] Rate limiting implemented
- [x] Unit tests written
- [x] Integration tests written
- [x] Documentation updated
- [x] Code reviewed
- [x] Coverage ≥ 80%

---

## Detailed Checklist

### Database Migration (`V6__audit_log.sql`)

- [x] `audit_log` table: `id BIGSERIAL PK`, `actor_user_id BIGINT FK → app_user(id)`, `action VARCHAR(100) NOT NULL`,
  `target_type VARCHAR(100)`, `target_id VARCHAR(255)`, `status VARCHAR(20) NOT NULL`, `ip_address VARCHAR(45)`,
  `created_at TIMESTAMP NOT NULL DEFAULT now()`
- [x] Index on `(actor_user_id, created_at DESC)` for admin audit view queries
- [x] Index on `created_at DESC` for global admin audit queries

### Backend — Entity

- [x] `AuditLog` entity — fields: `actorUserId`, `action`, `targetType`, `targetId`, `status`, `ipAddress`, `createdAt`
- [x] `AuditAction` enum: `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `IMPORT_UPLOAD`, `IMPORT_CANCEL`,
  `SAVED_FILTER_DELETE_OTHER`

### Backend — Repository

- [x] `AuditLogRepository extends JpaRepository`

### Backend — Service

- [x] 
  `AuditService.record(Long actorUserId, AuditAction action, String targetType, String targetId, String status, String ipAddress)` —
  `@Async` to avoid blocking the request thread

### Backend — Audit Hooks

- [x] `AuthService.login()` — record `LOGIN_SUCCESS` or `LOGIN_FAILURE` after authentication attempt
- [x] `ImportService.triggerImport()` — record `IMPORT_UPLOAD` after job creation
- [x] `SavedFilterService.delete()` — record `SAVED_FILTER_DELETE_OTHER` when ADMIN deletes another user's filter

### Backend — Actuator Configuration (`application.yml`)

- [x] Enable `health` endpoint only: `management.endpoints.web.exposure.include=health`
- [x] Configure liveness probe: `management.endpoint.health.probes.enabled=true`
- [x] Add Flyway health indicator: `management.health.flyway.enabled=true`
- [x] Add DB health indicator (enabled by default)
- [x] Secure actuator: ensure `/actuator/**` is accessible without auth but only `health` sub-paths

### Backend — Pagination Cap

- [x] Add `@Max(200)` and `@Min(1)` validation on `size` parameter in:
  - [x] `GeneController.listGenes()` `@RequestParam int size`
  - [x] `GeneSearchRequest.size` field
  - [x] `ImportAdminController` (import job list)
  - [x] `SavedFilterController` (if pagination added)
- [x] Global exception handler maps `ConstraintViolationException` → `400` with message "Page size must not exceed 200"

### Backend — Rate Limiting (Bucket4j)

- [x] Add `bucket4j-spring-boot-starter` to `pom.xml`
- [x] Configure in `application.yml`:
  ```yaml
  bucket4j:
    enabled: true
    filters:
      - cache-name: rate-limit-cache
        url: /api/genes/search
        http-response-body: '{"status":429,"error":"Too Many Requests","message":"Rate limit exceeded. Retry after {retry-after} seconds."}'
        rate-limits:
          - cache-key: getRemoteAddr()
            bandwidths:
              - capacity: 60
                time: 1
                unit: minutes
  ```
- [x] `Retry-After` header included in 429 response

### Tests

- [x] `AuditServiceTest` — unit:
  - [x] `record()` saves `AuditLog` entity with correct fields
  - [x] Async — does not block calling thread
- [x] Integration tests:
  - [x] `GET /api/genes?size=201` → 400
  - [x] `POST /api/genes/search` with `"size": 201` → 400
  - [x] Actuator liveness → 200
  - [x] Actuator readiness → 200 with DB up

### General

- [x] Actuator non-health endpoints remain unexposed
- [x] Audit log does not log sensitive data (no passwords, no tokens)
- [x] Code reviewed
- [x] Coverage ≥ 80%
