# OPS-001 — Operations & Hardening

## Description

Implement operational and hardening features:

- **Audit log** — record admin actions (login, import upload, import cancel, saved-filter delete-other-user) in
  `audit_log` table.
- **Health & readiness endpoints** — Spring Boot Actuator liveness and readiness probes with DB + Flyway checks.
- **Pagination cap** — reject `size > 200` on all list endpoints.
- **Rate limiting** — 60 req/min per IP on `POST /api/genes/search` using Bucket4j.

## Scope

| Layer        | Artifact                                                                   |
|--------------|----------------------------------------------------------------------------|
| DB migration | `V6__audit_log.sql` — `audit_log` table                                    |
| Entity       | `AuditLog` entity                                                          |
| Repository   | `AuditLogRepository`                                                       |
| Service      | `AuditService` — `record(actor, action, targetType, targetId, status, ip)` |
| AOP / Filter | `@AfterReturning` aspect or event listener for auto-audit on admin actions |
| Config       | `application.yml` — Actuator config; Bucket4j config                       |
| Filter       | `RateLimitFilter` — intercepts `POST /api/genes/search`                    |
| Dependency   | `bucket4j-spring-boot-starter` added to `pom.xml`                          |

## Acceptance Criteria

- [ ] After `POST /api/auth/login` (success or failure), a row is inserted in `audit_log`.
- [ ] After `POST /api/admin/import/uniprot`, a row is inserted in `audit_log`.
- [ ] After `DELETE /api/saved-filters/{id}` by ADMIN on another user's filter, a row is inserted.
- [ ] `GET /actuator/health/liveness` returns `200 { status: "UP" }`.
- [ ] `GET /actuator/health/readiness` returns `200` only when DB is reachable and Flyway is up-to-date.
- [ ] `GET /api/genes?size=201` returns `400` with message "Page size must not exceed 200".
- [ ] `POST /api/genes/search?size=201` in request body returns `400`.
- [ ] Calling `POST /api/genes/search` 61 times in 1 minute from the same IP returns `429` on the 61st call with a
  `Retry-After` header.
- [ ] Actuator endpoints other than health are **not** publicly accessible.
- [ ] Unit tests for `AuditService`.

## References

- `documentation/plan.md` — US-38, US-39, US-40
- `documentation/overview.md` — NFRs §12
