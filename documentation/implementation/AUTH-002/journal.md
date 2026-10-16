# AUTH-002 — Journal

## 2026-10-16 — Keycloak BFF Integration Specification

### Work performed

- Review of implementation folder conventions and constitution; research of AUTH-001, ARCH-001, and ARCH-002 decisions.
- Identification of the existing state: Angular stores tokens in `sessionStorage`, the interceptor adds Bearer,
  `auth-service` is stateless with CSRF disabled, the gateway handles HMAC JWTs, and the frontend proxy routes directly
  to it. None of these implementations have been modified.
- Drafting of [overview.md](overview.md), with the four required sections: BFF architecture and login flow,
  Keycloak/PostgreSQL Compose, Spring Boot specification, Angular specification.
- Addition of business API migration to the BFF, trust boundaries, encrypted server-side storage, CSRF protection, and
  verifiable acceptance criteria.
- Clarification of limits: Strict same-site requires a tested OIDC flow; a genuine authenticated `403` does not trigger
  re-login; historical administrator names are distinguished from the Keycloak 26 bootstrap interface.
- Creation of the [plan](plan.md), including security gates and the required coverage threshold; reference in
  the [catalogue](../README.md).

### Checks actually performed

| Control                                                                                              | Result / scope                                                                                           |
|------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| Consultation of the Keycloak release API                                                             | Stable release `26.8.1`, published on `2026-10-13`, confirmed at the time of drafting                    |
| Consultation of the official PostgreSQL registry                                                     | Existence of tag `16.15-alpine` verified; no CVE audit or image download performed                       |
| Consultation of Maven Central POM                                                                    | Existence of `spring-boot-starter-session-data-redis:4.0.6` confirmed; no dependency installation        |
| Inspections during document drafting                                                                 | Presentation corrections for YAML and Angular `DOCUMENT` import; local links checked during finalization |
| Extraction of YAML blocks into a temporary directory, then `docker compose ... config --format json` | Valid Compose schema; Spring placeholders preserved so they are not interpreted as Compose substitutions |
| Node assertions on the analyzed configuration                                                        | Expected image versions, DB network `internal`, no published ports, and cookie attributes conform        |
| `bash -n` on the extracted SQL provisioning and healthcheck command                                  | Valid Bash syntax; neither SQL nor network healthcheck executed against a real service                   |
| Targeted IDE build check on existing documents                                                       | Success; no build issues returned; this is not a compile/test of future BFF components                   |
| Targeted whitespace check in Git                                                                     | No issues reported in the documents; no Git staging, commit, or restore commands executed                |

Temporary validation files were removed. Configuration checks used dummy values and `/dev/null` for secret paths; no
production secret was read or displayed. No container was started. Pre-existing worktree changes were not restored or
merged into this delivery.

### Status and limits

- **Drafting: complete. Runtime integration: `not-started`.**
- No change to `docker-compose.yml`, backend/frontend sources, configuration repository, or active API contract.
- No unit, integration, end-to-end, or penetration tests executed; no application coverage result claimed.
  Java/TypeScript examples are documentation excerpts, not new compiled and delivered components.
- The real deployment, provisioning scripts, image digests, CVE reports, backup restoration, Strict/MFA/browser
  compatibility, and multi-instance tests remain to be produced per the plan.
- Closure of implementation remains blocked until AC-01 to AC-14, security reviews, and coverage of at least **80%** for
  new backend/frontend components are demonstrated.
