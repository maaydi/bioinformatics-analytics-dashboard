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
- [ ] Audit hooks wired
- [ ] Actuator configured
- [ ] Pagination cap implemented
- [ ] Rate limiting implemented
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

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

- [ ] `AuthService.login()` — record `LOGIN_SUCCESS` or `LOGIN_FAILURE` after authentication attempt
- [ ] `ImportService.triggerImport()` — record `IMPORT_UPLOAD` after job creation
- [ ] `SavedFilterService.delete()` — record `SAVED_FILTER_DELETE_OTHER` when ADMIN deletes another user's filter

### Backend — Actuator Configuration (`application.yml`)

- [ ] Enable `health` endpoint only: `management.endpoints.web.exposure.include=health`
- [ ] Configure liveness probe: `management.endpoint.health.probes.enabled=true`
- [ ] Add Flyway health indicator: `management.health.flyway.enabled=true`
- [ ] Add DB health indicator (enabled by default)
- [ ] Secure actuator: ensure `/actuator/**` is accessible without auth but only `health` sub-paths

### Backend — Pagination Cap

- [ ] Add `@Max(200)` and `@Min(1)` validation on `size` parameter in:
    - [ ] `GeneController.listGenes()` `@RequestParam int size`
    - [ ] `GeneSearchRequest.size` field
    - [ ] `ImportAdminController` (import job list)
    - [ ] `SavedFilterController` (if pagination added)
- [ ] Global exception handler maps `ConstraintViolationException` → `400` with message "Page size must not exceed 200"

### Backend — Rate Limiting (Bucket4j)

- [ ] Add `bucket4j-spring-boot-starter` to `pom.xml`
- [ ] Configure in `application.yml`:
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
- [ ] `Retry-After` header included in 429 response

### Tests

- [ ] `AuditServiceTest` — unit:
    - [ ] `record()` saves `AuditLog` entity with correct fields
    - [ ] Async — does not block calling thread
- [ ] Integration tests:
    - [ ] `GET /api/genes?size=201` → 400
    - [ ] `POST /api/genes/search` with `"size": 201` → 400
    - [ ] Actuator liveness → 200
    - [ ] Actuator readiness → 200 with DB up

### General

- [ ] Actuator non-health endpoints remain unexposed
- [ ] Audit log does not log sensitive data (no passwords, no tokens)
- [ ] Code reviewed
- [ ] Coverage ≥ 80%
