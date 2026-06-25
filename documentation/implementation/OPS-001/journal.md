# OPS-001 — Implementation Journal

---

## 2026-06-24

### Work performed

- Added documentation related to the OPS-001 workstream.
- Implemented backend rate limiting using Bucket4j and added associated unit tests.
- Addressed failing tests: fixed unit test failures introduced during the Bucket4j work.

## 2026-06-23

### Work performed

- Enhanced service-level logging and strengthened controller audit hooks to capture richer context.
- Removed duplicated login audit row to prevent duplicate audit entries.

## 2026-06-22

### Work performed

- Decoupled `app_user` and `audit_log` by removing the direct relation between the tables (audit entries are now
  independent).
- Introduced a request context holder to collect endpoint metadata required by the audit subsystem.
- Added an authentication event listener to record login events to the audit log.
- Extended audit annotations to include `targetId` extraction for improved traceability.

## 2026-06-21

### Work performed

- Fixed several unit tests (frontend and backend) including a fix for delete-cascade handling on audit logs.
- Enforced the pagination cap on backend endpoints (size ≤ 200) to prevent overly large page requests.
- Implemented pagination support for the Saved Filters feature in the frontend.
- Updated `application.yml` actuator configuration for improved observability during OPS tasks.

## 2026-05-12

### Ticket created

- Created `overview.md` and `plan.md` from backlog stories US-38, US-39, US-40.
- Reviewed existing codebase: no `AuditLog` entity, service, or migration found. No `RateLimitFilter`. Actuator
  dependency likely present via `spring-boot-starter-actuator` but configuration not verified.
- Pagination cap (size ≤ 200) is not yet enforced in any controller.
- Bucket4j dependency is absent from `pom.xml`.
- Depends on `GENE-001`, `IMPORT-001`, and `FILTER-001` being in place so that audit hooks can be wired.
- Implementation not yet started.
