# AUTH-002 — Implementation Plan

Normative reference: [technical specification](overview.md). Date: 2026-10-16.

**Integration status: `not-started`.** Only the documentary production is completed in this delivery. A checked
documentary box is neither implementation nor security validation.

## 1. Documentary deliverables

| Task                                                                   | Status | Result                                                                                   |
|------------------------------------------------------------------------|--------|------------------------------------------------------------------------------------------|
| Review the existing state and decisions AUTH-001 / ARCH-001 / ARCH-002 | `done` | Gaps identified: browser tokens, CSRF disabled, trust in headers, direct gateway routing |
| Specify the BFF architecture and Strict/OIDC constraints               | `done` | Same-site HTTPS origins and confidential Code + PKCE flow                                |
| Specify Compose, Spring configuration, and Angular contracts           | `done` | Excerpts and operational requirements in `overview.md`                                   |
| Define migration, tests, and acceptance criteria                       | `done` | AC-01 to AC-14; 80% coverage threshold                                                   |
| Reference AUTH-002 in the catalog                                      | `done` | Integration status distinct from drafting status                                         |

## 2. Future implementation phases

All the following tasks are `not-started`.

| Phase                           | Owner                                   | Tasks / dependencies                                                                                                                                                                                           | Exit conditions                                                                 |
|---------------------------------|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| P0 — Preparation                | Architecture / IAM / Security           | Confirm owned domains, certificates, internal issuer resolution, role matrix, SSO logout policy, SLOs; identify legacy accounts to migrate                                                                     | Signed design; verifiable Strict/MFA flow; no unresolved cross-site dependency  |
| P1 — IAM Infrastructure         | Platform / DBA                          | Pinned and analyzed images, dedicated PostgreSQL and application role, external secrets, realm/client provisioning, TLS proxy, management ACLs, backup/restore; provision shared TLS/ACL Redis                 | AC-13, backup/restore evidence, removal of bootstrap account                    |
| P2 — BFF Session                | Backend / IAM                           | OAuth2 Client Code+PKCE, CSRF, CookieSerializer, encrypted storage, fixation, idle/absolute durations, `/me`, `/csrf`, logout, Problem Details errors; distributed refresh rotation and back-channel logout    | AC-01 to AC-06, AC-08, AC-09, AC-12, unit and integration tests                 |
| P3 — Relay and Resource Servers | Backend / Platform                      | Inventory method+route+destination, streaming relay, header and cookie cleanup, Keycloak JWT/audience validation in gateway/services, removal of trust in `X-User-*`; no return to auth-service via gateway    | AC-10 and AC-11; all business APIs operate through BFF without bypass           |
| P4 — Angular session-only       | Frontend                                | SessionService, CsrfState, credentials interceptor, guards, single-flight coordinator, 401/403 errors; removal of Bearer/refresh/sessionStorage; anonymous SSR strategy                                        | AC-02, AC-03, and AC-07; guard/interceptor browser tests                        |
| P5 — Coordinated migration      | Architecture / Backend / Frontend / IAM | Match `(iss, sub)` and historical data, remove legacy endpoints, separate machine accounts, change Nginx/proxy dev routing, publish Config Server changes actually consumed; update API contract and consumers | AC-14; no browser emission of tokens; documented secure rollback                |
| P6 — Approval and operations    | Security / QA / Platform                | End-to-end tests, targeted pentest, CVE reports, load/streaming, secret rotation, monitoring, canary, and restoration drill                                                                                    | All ACs validated, coverage ≥ 80%, residual risks accepted, evidence in journal |

**Critical dependencies:** P1 before P2; P2/P3/P4 validated together before P5. Removing Angular tokens without an
operational BFF relay would break business APIs and is not a deployable production increment. No fallback to the browser
JWT model is allowed.

## 3. Validation gates

- [ ] App/IAM domains are actually same-site HTTPS, callback is exact, and DNS/CA for browser/JVM are validated.
- [ ] Initial login flow, SSO, MFA, and errors are tested across the supported browser matrix with Strict unchanged.
- [ ] Opaque cookie, fixation, CSRF/origin, encrypted storage, and absolute expiration are verified.
- [ ] Role mapping and JWT/audience validation are tested with malformed claims and forged headers.
- [ ] Revocation, local/remote logout, and concurrent refresh are tested across multiple BFFs; residual JWT downstream
  validity is acceptable.
- [ ] No access/refresh/ID tokens are present in storage, JS cookies, HTML, final URLs, responses, or logs.
- [ ] Interceptors are limited to the API origin; authenticated `403` does not loop and concurrent `401`s are
  deduplicated.
- [ ] No gateway/service bypass, SSRF, authenticated external redirection, or cookie leakage through the relay.
- [ ] Large imports/exports, cancellation, timeouts, and streaming are validated without full buffering.
- [ ] Legacy endpoints are disabled, old refresh tokens invalidated, user data correctly associated.
- [ ] Bootstrap secrets removed, images/digests analyzed, and rotation/restoration procedures tested.
- [ ] API contracts, Config Server Git configuration, runbooks, and consumers are synchronized.
- [ ] Unit/integration/E2E reports and backend/frontend coverage ≥ 80% are recorded.
- [ ] Security/Architecture review, canary, monitoring, and maintenance/rollback procedure are approved.

## 4. Follow-up rules

Record in [journal.md](journal.md) the commands and results actually obtained, environments, versions, and test
evidence. Do not declare a criterion validated from the single YAML example or a mocked test. Any deviation from the BFF
model, cookie attributes, or absence of browser tokens requires an explicit architecture decision and security review.
