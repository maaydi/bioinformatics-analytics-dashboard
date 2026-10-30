# ARCH-002 — Implementation Journal

## Chronological Log

---

### 2026-09-29 — Ticket analysis

**Action:** Created `overview.md` (problem statement C1–C12, goals, AC-1 … AC-10) and `analyse.md` (inventory,
decisions D1–D11, open questions Q1–Q7).

**Outcome:** Implementation blocked until the 🔴 questions (Q2, Q3, Q5) are answered.

---

### 2026-09-29 — Answers to Q1–Q7 received

| Q  | Answer                                                                       |
|----|------------------------------------------------------------------------------|
| Q1 | Schema `gene_data` (dashboard will be renamed `gene-service`)                |
| Q2 | Stage B deferred to ARCH-001 Phase 10                                        |
| Q3 | Merge: new data updates existing data (light file, then detailed API import) |
| Q4 | No delete — add or merge only                                                |
| Q5 | Configurable freshness, default 60 s                                         |
| Q6 | Gateway header-trust hardening in scope (traceability)                       |
| Q7 | Legacy view refresh kept in parallel until parity is proven                  |

**Outcome:** All blockers lifted.

---

### 2026-09-29 — Plan rev. 1 drafted

**Action:** First phased `plan.md` (phases 0–8).

---

### 2026-09-29 — Plan rev. 2 (review against answers and codebase)

**Action:** Reviewed rev. 1 against Q1–Q7 and the code. Verified in the code:

- `domain-model.md` / `ProteinEntry`: `entry_version` is the UniProt `DT` version, so it cannot be the event aggregate
  version.
- `JwtGatewayFilter.shouldNotFilter`: public-endpoint match with `path::contains` and no stripping of inbound
  `X-User-*`.
- `GatewayUserAuthenticationFilter`: every service authenticates from `X-User-*` headers.
- `SavedFilterClient` (import-service): forges `X-User-Id` / `X-User-Role` on a public route.
- Dashboard Flyway history lives in `public` (no `default-schema`); other services use their own schema (`auth`,
  `analytics`, `import_batch`, `export_data`).
- `analytics-service` local `application.yml`: `spring.flyway.default-schema: auth` (wrong fallback).
- `KafkaProducerConfig`: `ADD_TYPE_INFO_HEADERS = true`.
- Dashboard migrations are mostly unqualified; V21 is `public`-qualified (fresh-install leftover in `gene_data`);
  functions `fts_match` (V3) and `trg_protein_entry_search_vector` (V1) must be moved explicitly.

**Outcome:** `plan.md` rev. 2 with 15 findings (F1–F15), normative specs (schema/roles, merge rules, internal API,
events, trust model, configuration), 10 phases (P0–P9), new AC-11 (trust model) and AC-12 (merge), and a corrected
rollout order (import writer switch before analytics backfill).

**Next steps:** P0 — align `analyse.md` and `overview.md` (tasks 0.1, 0.2), update `domain-model.md`, create
`event-catalog.md`, capture the baseline.

---

### 2026-10-30 — Shared audit publisher implemented; platform phase remains partial

**Action:** Updated `plan.md` to rev. 5 and marked item 1.8 complete.
Producer instrumentation now lives in `com.bioinformatics.common.audit`:
`@Auditable`, `AuditAspect`, `AuditContextHolder`, `AuditContextFilter`,
`AuditEventPublisher`, `AuditPublisherConfig` and `AuditPublisherProperties`.
The contract/enums are reused from `shared-models`; no audit entity, repository,
search service, retention job or audit datasource was added to this package.

Dashboard gene and saved-filter callers use the shared annotation/enums. Its
old audit persistence/search classes, duplicated types, executor and obsolete
tests have been removed. Rate limiting remains local. Dashboard Kafka settings
were added to Config Server configuration. Audit-service has base/dev/prod
config files for `audit_log`, `classpath:migration`, manual Kafka acknowledgements
and production port `8089`.

**Behavior:** The bounded publisher snapshots caller/security/request context
before async execution, emits UUID-keyed `AuditLogEvent` records on
`audit.events.log`, and logs queue/send failures without replacing the original
business result or exception. It carries actor, source application, correlation,
target/status/time and key/value HTTP metadata. Correlation falls back from the
incoming header to MDC `traceId` to a generated UUID. Delivery remains
best-effort; the catalog outbox and reliable catalog event work in P4 are not
implemented by this change.

**Verification during implementation:** 21 new audit checks passed across
`AuditAspectTest`, `AuditEventPublisherTest`, `AuditContextFilterTest`,
`AuditPublisherConfigTest` and `AuditPublisherConsumerTest`. The embedded-Kafka
test exercises the real shared JSON producer/consumer configuration and
`AuditConsumer`; persistence and Redis idempotency are mocked. The clean targeted
reactor run and final IDE build passed. Existing
`AuditLogMapperDecoratorTest` and `SavedFilterControllerTest` were also selected
in the successful final run. The reproducible Maven command is recorded in the
ARCH-001 journal's same-date entry. These are implementation-session results;
test reports were subsequently removed and no fresh suite or coverage measurement
was run for this documentation update.

**Phase/acceptance status:** Only 1.8 is closed. P-1, P0 and P1 remain open;
AC-13 and AC-14 are partially evidenced, not accepted. The producer work was
implemented ahead of the plan's complete pre-start approval/baseline sequence;
that does not waive its gates. Specifically:

- The module is still `backend/dashboard` with the dashboard Java/runtime identity;
  the gene-service rename (1.7) remains pending.
- Audit configuration is Kafka-property-conditioned and discovered by the
  existing common component scan; explicit auto-configuration imports, dedicated
  feature opt-in and common dependency/JPA decoupling (1.1–1.3, 1.9) remain pending.
- Kafka still uses type headers. The header-free round-trip and end-to-end
  servlet/Feign/Kafka correlation propagation in 1.4–1.6 are not satisfied by the
  audit-only transport check.
- Legacy dashboard migrations and rows are untouched. Historical transfer vs
  archive/purge, Flyway baseline/history, exclusive audit credentials, producer
  access denial and database placement approval remain open.
- Audit retention, batch insertion, consumer retry/DLT/metrics qualification and
  real-storage/authorization tests remain open. The controller implements
  `POST /api/v1/admin/audit`; reconcile it with the planned GET contract and
  verify gateway routing before rollout.
- No production cutover, independent-startup, golden-file, manifest, load,
  full-reactor-test or JaCoCo acceptance gate is closed by this work.

**Outcome:** Shared audit production and transport compatibility are implemented;
ARCH-001 Phase 8 and ARCH-002 P1 continue with the remaining ownership,
refactor, operational and qualification work.

---

## Coverage Tracking

| Component                         | Target | Current | Status      |
|-----------------------------------|--------|---------|-------------|
| dashboard (catalog, outbox)       | ≥ 80 % | —       | Not started |
| import-service (writer, ACL)      | ≥ 80 % | —       | Not started |
| analytics-service (projection)    | ≥ 80 % | —       | Not started |
| api-gateway (trust filters)       | ≥ 80 % | —       | Not started |
| common-starter (auto-config, S2S, audit) | ≥ 80 % | Not measured | Audit producer implemented; broader auto-config/S2S work pending |
| audit-service (consumer boundary) | ≥ 75 % (ARCH-001) | Not measured | Embedded-Kafka contract check passed; storage/authorization/retention qualification pending |

**Last Updated:** 2026-10-30
