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

### 2026-11-06 — My Decision alignment; plan rev. 6

**Action:** Updated `plan.md`, `analyse.md` and `overview.md` from the D1–D4 My Decision notes:

- D1: name the shared contract module `backend/libs/gene-model-lib`; reuse common cross-service DTOs while keeping
  service-specific/internal types local.
- D2: retain Feign chunk writes, with import-service parsing/orchestration and gene-service merge/persistence.
- D3: replace the analytics CQRS proposal with Kafka ingestion completion → gene-service statistics API →
  atomic persistence of aggregate results in analytics-owned refresh tables/views. Remove the entry-event consumer,
  protein projection, duplicated filter specification and analytics republish/backfill requirements.
- D4: retain the catalog transactional outbox and polling relay, including reliable ingestion-completed markers.
  Per-entry events remain separate catalog contracts, not analytics input.

**Outcome:** P2/P4/P5/P6 contracts, configuration, rollout, risks and AC-6/AC-7 now describe the API-backed refresh.
Q7 legacy shadow/parity remains mandatory. Cached aggregates remain readable during catalog outages; refreshes and
live filtered/compare requests depend on gene-service and must expose failures. This is documentation-only work:
no implementation phase, runtime behavior, coverage target or acceptance gate is closed.

---

### 2026-11-06 — P1 platform implementation and remaining acceptance gates

**Action:** Replaced the common package scan with individually listed conditional auto-configurations. The starter's
infrastructure dependencies are optional, and each business service declares its own MVC/security/data/Kafka/cache/AOP/
observability dependencies. The gateway remains reactive and does not depend on common-starter. Legacy catalog
entities/repositories/services require `app.legacy-catalog.enabled=true` in gene/import/export/analytics; UniProt clients
additionally require their HTTP/retry dependencies. Auth/audit no longer scan catalog entities or repositories.
Provider-header routing is explicitly registered by the shared MVC feature, independently of UniProt/JPA; analytics
requires it even when UniProt clients are absent. Analytics' JPA slice explicitly imports the legacy feature.

Audit production now requires `app.audit.enabled=true`, uses validated bounded pool settings and starts on a classpath
without JPA/JDBC/Flyway/Batch/Redis or servlet APIs. All six existing producers opt in through service configuration.
The existing AOP/event contract, actor identity, source service and best-effort/logged failure policy remain unchanged.
Ordinary database-backed controller tests disable audit production explicitly; embedded-Kafka tests enable and exercise it.
No audit persistence/runtime dependency or audit credential was added to the producer feature.

Kafka producers disable Java type headers; both audit and legacy refresh listeners select their concrete default
value type and ignore sender type headers. Wildcard trusted packages are removed. A nullable global consumer group is
omitted rather than inserted into Kafka's null-rejecting configuration map; existing listener-specific groups remain
authoritative. The custom factory honors `spring.kafka.listener.auto-startup`, including isolated integration tests.
Servlet, Feign and Kafka interceptors propagate `X-Correlation-Id` using MDC `correlationId`, restoring prior context on
success/failure. IDs are bounded to the audit schema's 100-character limit; invalid IDs are logged without echoing their
contents and replaced with UUIDs. Audit workers and import-refresh sends/callbacks preserve their envelope correlation;
all six business-service logging configurations expose the MDC key.

The module/package/runtime move to gene-service was already present when this task began. Completed remaining wiring:
development/migration scripts, build-script help, Compose import/export dependencies and gene Dockerfile path, gateway
gene circuit-breaker/time-limiter names, and current project/operator documentation. The reactor, Config Server files,
Feign target, gateway destination, Kubernetes selectors/config/secret references and probes use `gene-service`; no
compatibility alias is needed by current callers. Product-wide `bio-dashboard` namespace/image prefixes and JWT audience
are intentionally unchanged. Public/internal paths, migration contents/history, database objects and Kafka topics were
not changed.

**Integration boundary:** ARCH-001 Phase 8 now specifies the existing authorized, paginated
`POST /api/v1/admin/audit` search contract rather than a new GET API. Existing topic ownership, separate audit storage,
retention requirements and the historical-data/Flyway approval gate remain explicit.

**Verification evidence:**

- `mvn -q -fae -f backend\pom.xml test` passed with 883 tests across 153 suites, zero failures/errors/skips,
  including PostgreSQL-backed service/API tests.
  Earlier runs caught and led to fixes for the optional Zipkin sender, absent global Kafka group, analytics legacy
  entity registration and provider-filter placement; expectations were not relaxed to hide those regressions.
- `mvn -q -f backend\pom.xml -DskipTests package` passed for the complete reactor after the successful test run.
- `PlatformAutoConfigurationTest` covers feature switches, master disable, optional-classpath isolation, existing
  datasource pools, audit opt-in/missing transport, invalid pool settings and MVC provider routing without UniProt/JPA.
- `ViewRefreshTransportTest` and `AuditPublisherConsumerTest` use embedded Kafka and the real respective consumers,
  assert absence of `__TypeId__`, and check consumer MDC. Database/idempotency collaborators are mocked in these
  transport tests; separate audit persistence tests remain distinct evidence.
- `CorrelationPropagationTest` covers the servlet/Feign/Kafka chain, missing/invalid IDs, failure cleanup and worker
  context restoration. The import publisher test checks envelope/MDC correlation consistency at send time.
- `docker compose config --quiet` passes. Offline checks parse all 79 Kubernetes manifests and verify gene selectors,
  ports, config/secret references and probes; all Compose build/Dockerfile paths and service dependencies resolve.
  Config Server/service YAML, logging XML and optional dependency declarations also pass structural checks.
- Actual Config Server YAML route checks preserve the gene public path set and audit destination, with matching
  gene circuit-breaker/time-limiter IDs. Kubernetes client dry-run could not contact a cluster API; offline checks are
  not a deployed-manifest or gateway acceptance claim.

**Acceptance remains open:** P0 public-response golden files were not captured before this change. Config Server Git
publishing, independent deployed startup/route qualification and production rollout have not been performed. Existing
shared environment-backed database credentials do not demonstrate producer denial of audit storage. Historical audit
transfer versus archive/purge, count/key verification and Flyway baseline approval remain unresolved; no data was
moved/purged and no migration was replayed. Retention remains ARCH-001 Phase 8 work. P1 items 1.1-1.8 are implemented,
while the remaining 1.9/1.10 release gates and phase acceptance remain open. No new coverage percentage is claimed.

---

## Coverage Tracking

| Component                         | Target | Current | Status      |
|-----------------------------------|--------|---------|-------------|
| gene-service (catalog, outbox)    | ≥ 80 % | —       | P1 groundwork complete; catalog/outbox work pending |
| import-service (writer, ACL)      | ≥ 80 % | —       | Not started |
| analytics-service (API-backed refresh) | ≥ 80 % | — | Not started |
| api-gateway (trust filters)       | ≥ 80 % | —       | Not started |
| common-starter (auto-config, S2S, audit) | ≥ 80 % | Not measured | P1 auto-config/correlation/audit implemented; S2S work remains P3 |
| audit-service (consumer boundary) | ≥ 75 % (ARCH-001) | Not measured | Embedded-Kafka contract check passed; storage/authorization/retention qualification pending |

**Last Updated:** 2026-11-06
