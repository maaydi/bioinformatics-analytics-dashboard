# ARCH-001 — Implementation Journal

## Chronological Log

---

### 2026-08-12 — Requirements Analysis & Ambiguity Resolution

**Action:** Read full application specification (merged_output.md) covering:

- Design system quick reference and SCSS tokens
- API contract (all 6 endpoint groups)
- Project constitution and coding standards
- Domain model (full PostgreSQL schema, materialized views, indexes)
- Data format specification (UniProt .dat flat file)
- Glossary of domain terms
- Overview (architecture, NFRs, auth matrix, import spec, MVP roadmap)
- Implementation plan with all user stories and ticket mapping
- All existing implementation tickets: GENE-001 through GENE-003, AUTH-001, IMPORT-001, DETAIL-001, ANALYTICS-001,
  DASH-001, FILTER-001, EXPORT-001, COMPARE-001, PERF-001, CACHE-001, A11Y-001, REFACTOR-001, REMOTE-001
- Pending tickets: STRUCT-001, NLQ-001, PIPE-001 (with analyse.md resolutions)

**Outcome:** Understood the full system context:

- Monolith backend: Spring Boot 3+, JPA, Spring Batch, Spring Security JWT
- Frontend: Angular with standalone components, AG Grid, design system tokens
- Database: PostgreSQL with materialized views, GIN indexes, full-text search
- Existing provider architecture (REFACTOR-001) with postgres/uniprotKb providers
- Existing caching layer (CACHE-001) with Redis
- Performance tuning (PERF-001) with SEQUENCE IDs, JDBC batching, aggregate writers

**Decision:** ARCH-001 must preserve all existing capabilities while decomposing the monolith. The 3 pending features
(STRUCT-001, NLQ-001, PIPE-001) should be built as microservices from inception rather than extracted later.

---

### 2026-08-12 — Architecture Decisions Recorded

**Action:** Resolved 10 key ambiguities in `analyse.md`:

1. **Migration strategy:** Strangler Fig (incremental) over Big Bang
2. **Database strategy:** Two-stage — shared PostgreSQL with read replicas (Stage 1), then database-per-service via CDC
   (Stage 2)
3. **Communication:** Hybrid — REST for queries, Kafka for events
4. **Auth:** JWT at Gateway + service-level validation + service-to-service internal JWT
5. **Transactions:** Local transactions + saga orchestration (no 2PC)
6. **Service granularity:** 10 services (Gateway, Eureka, Config, Auth, Gene, Analytics, Import, Export, Structure, NLQ,
   Notification, Audit)
7. **Frontend:** Direct Gateway routing (no BFF for MVP)
8. **Read replica routing:** `AbstractRoutingDataSource` with `@Transactional(readOnly)` hint
9. **Circuit breakers:** Resilience4j with per-service defaults
10. **Configuration:** Spring Cloud Config Server with Git backend

**Outcome:** All blockers resolved. Implementation can proceed.

---

### 2026-08-12 — Service Decomposition Defined

**Action:** Mapped all existing and planned features to microservices:

| Feature / Ticket             | Target Service                    | Notes                                                      |
|------------------------------|-----------------------------------|------------------------------------------------------------|
| AUTH-001                     | `auth-service`                    | First extraction (lowest dependency)                       |
| GENE-001, GENE-002, GENE-003 | `gene-service`                    | Core domain, highest traffic                               |
| ANALYTICS-001, DASH-001      | `analytics-service`               | Read-only, read replica exclusive                          |
| IMPORT-001                   | `import-service`                  | Spring Batch, event publisher                              |
| EXPORT-001, PIPE-001         | `export-service`                  | Async batch export, file I/O heavy                         |
| FILTER-001                   | `gene-service` + `auth-service`   | Saved filters in gene schema, auth in auth schema          |
| COMPARE-001                  | `analytics-service`               | Compare endpoint moved to analytics                        |
| PERF-001                     | `gene-service` + `import-service` | Sequence strategy, batch tuning                            |
| CACHE-001                    | All services                      | Redis shared cache; post-import eviction in import-service |
| A11Y-001                     | `frontend` + `auth-service`       | Theme service frontend, password change in auth            |
| REFACTOR-001                 | `gene-service`                    | Provider dispatcher preserved                              |
| REMOTE-001                   | `gene-service`                    | UniProtKb provider implementation                          |
| STRUCT-001                   | `structure-service`               | New service (Phase 6)                                      |
| NLQ-001                      | `nlq-service`                     | New service (Phase 7)                                      |
| OPS-001                      | `audit-service`                   | Audit log consumer                                         |

**Outcome:** Clear ownership boundaries established. No feature left unassigned.

---

### 2026-08-12 — Implementation Plan Drafted

**Action:** Created comprehensive `plan.md` with:

- 10 migration phases (0–10)
- Detailed checklists per phase (infrastructure, DB schema, API, events, tests)
- Shared library specification (`common-starter`)
- Docker Compose infrastructure stack
- Service-to-service communication patterns
- Frontend adaptation strategy
- Testing strategy (unit, integration, E2E, load)
- Risk register with 10 identified risks and mitigations

**Key metrics:**

- Estimated effort: XL (16–20 weeks)
- Services to extract: 10
- Infrastructure components: 4 (Gateway, Eureka, Config, Kafka)
- Database stages: 2 (shared with replicas → per-service)
- Test coverage target: ≥ 80% per service

**Outcome:** Plan ready for review and phased execution.

---

### 2026-08-12 — Documentation Artifacts Created

**Action:** Generated all 4 required ticket artifacts:

1. `documentation/implementation/ARCH-001/analyse.md` — 10 resolved ambiguities, 5 open questions
2. `documentation/implementation/ARCH-001/overview.md` — Scope, decomposition diagram, 10 acceptance criteria
3. `documentation/implementation/ARCH-001/plan.md` — 10 phases, detailed checklists, testing strategy, risk register
4. `documentation/implementation/ARCH-001/journal.md` — This file

**Next Steps (pending prioritization):**

1. Create `documentation/event-catalog.md` — formal Kafka topic definitions
2. Create `documentation/db-migration-strategy.md` — Flyway schema separation guide
3. Create `documentation/service-auth-spec.md` — service-to-service JWT contract
4. Create `docs/runbooks/local-dev-setup.md` — developer onboarding
5. Bootstrap Phase 0: initialize `infrastructure/` and `libs/common-starter/`

---

### 2026-08-14 — Common Starter Implementation Completed

**Action:** Implemented the shared `common-starter` module under `backend/libs/common-starter/` with the foundational
Spring Boot auto-configuration needed by the extracted services.

**Outcome:** Phase 0.1 is now complete at the code level:

- Shared auto-configuration entry point is in place
- JWT validation, security defaults, and global exception handling are provided centrally
- Routing data source support, Kafka producer/consumer configuration, resilience defaults, tracing, and WebClient
  configuration are available for downstream services

**Notes:** Local publication and service dependency adoption remain tracked separately in `plan.md`.

---

## Coverage Tracking

| Component            | Coverage Target | Current | Status         |
|----------------------|-----------------|---------|----------------|
| common-starter       | ≥ 80%           | TBD     | ✅ Implemented |
| api-gateway          | ≥ 75%           | 0%      | ⏳ Not started |
| auth-service         | ≥ 85%           | 0%      | ⏳ Not started |
| gene-service         | ≥ 85%           | 0%      | ⏳ Not started |
| analytics-service    | ≥ 80%           | 0%      | ⏳ Not started |
| import-service       | ≥ 80%           | 0%      | ⏳ Not started |
| export-service       | ≥ 80%           | 0%      | ⏳ Not started |
| structure-service    | ≥ 80%           | 0%      | ⏳ Not started |
| nlq-service          | ≥ 80%           | 0%      | ⏳ Not started |
| notification-service | ≥ 75%           | 0%      | ⏳ Not started |
| audit-service        | ≥ 75%           | 0%      | ⏳ Not started |

---

**Last Updated:** 2026-08-14
