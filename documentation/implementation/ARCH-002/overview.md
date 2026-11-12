# ARCH-002 — Protein Catalog Ownership & Database-per-Service Enforcement

## Description

`ARCH-001` split the monolith into services (Strangler Fig), but we kept one shared library, `libs/common-starter`,
that still holds the **protein catalog domain**: the JPA entities, the Spring Data repositories, the read service, and
the JPA `Specification` used to filter proteins. Every service that pulls in this starter also gets the
`protein_entry` schema definition, and two of them (`import-service`, `analytics-service`) read or write the
dashboard's tables directly.

So the platform behaves like a **distributed monolith**: services are deployed separately, but they share a schema,
share persistence code, and have to be released together.

This ticket makes the refactored **`gene-service` the single owner** of the protein catalog bounded context:

- the `protein_entry` aggregate and its child tables (`protein_feature`, `protein_keyword`, `keyword`,
  `protein_go_term`, `go_term`, `cross_reference`, `host_organism`, `protein_comment`, `protein_publication`)
- its JPA entities, repositories, specifications and services
- its Flyway migrations and the `gene_data` schema / credentials

Other services reach catalog data **only** through:

- **Synchronous calls:** OpenFeign clients (resolved through Eureka) against a versioned internal API
  (`/internal/v1/**`), using decoupled DTOs.
- **Asynchronous catalog events:** Kafka events published by `gene-service` through a transactional outbox.

For analytics, Kafka ingestion completion is a **refresh trigger**, not a feed of protein rows: analytics calls the
gene-service statistics API and saves the returned aggregate results in its own refresh tables/views. Filtered and
compare queries use the same owner-side aggregation API; no per-protein CQRS projection is built.

Audit events follow a separate path: common-starter's producer-side `@Auditable` aspect publishes to the already-existing
`audit.events.log` topic; the new audit-service owns its separate schema, persistence, search API and retention. The
producer library and producer services do not connect to audit storage.

---

## Problem Statement — Evidence from the Codebase

| #   | Coupling point                                       | Location                                                                                                                                                                                      | Consequence                                                                                                      |
|-----|------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| C1  | Catalog JPA entities in a shared library             | `libs/common-starter/.../common/gene/entity/*` (8 entities incl. `ProteinEntry`)                                                                                                              | Every service carries the dashboard's schema; one column change forces all services to be rebuilt and redeployed |
| C2  | Catalog repositories and service in a shared library | `common/gene/repository/*` (7 repos), `common/gene/service/ProteinEntryService`, `common/gene/specification/GeneSpecification`                                                                | Persistence logic is duplicated across runtimes; no single owner                                                 |
| C3  | Component-scan leakage                               | `CommonAutoConfiguration` → `@ComponentScan("com.bioinformatics.common")`                                                                                                                     | `ProteinEntryService` and the UniProt REST clients are registered as beans in **every** service                  |
| C4  | Import writes directly to the catalog tables         | `import-service/.../writer/ProteinAggregateItemWriter`, `ProteinEntryWriterConfig`                                                                                                            | Two writers for one aggregate; invariants live outside the owner                                                 |
| C5  | Analytics reads the catalog tables directly (JPA)    | `AnalyticsProteinRepository extends ProteinEntryRepository`, `AnalyticsViewProteinRepositoryImpl` (Criteria on `ProteinEntry`), `PostgresFilteredAnalyticsService` (uses `GeneSpecification`) | Analytics breaks whenever the catalog schema changes                                                             |
| C6  | Analytics materialized views read across schemas     | `analytics-service/.../V1__analytics_schema.sql` → `FROM public.protein_entry`, `public.keyword`, `public.protein_keyword`                                                                    | A schema-level dependency that no API contract can protect                                                       |
| C7  | Schema name shared as a compile-time constant        | `shared-models/.../db/DbSchema.GENES_SCHEMA = "public"`                                                                                                                                       | Other services know where the dashboard stores its data                                                          |
| C8  | One database user for every service                  | `.env` → `COMMON_DATASOURCE_*` injected into every container through `env_file: .env`                                                                                                         | Nothing stops any service from reading or writing any table                                                      |
| C9  | Startup coupling                                     | `docker-compose.yml` → `import-service.depends_on.backend: service_healthy`                                                                                                                   | `import-service` can only start after the dashboard's Flyway has created the tables it validates                 |
| C10 | Shared cache eviction                                | `import-service/.../PostImportCacheEvictionListener` clears **every** cache of every `CacheManager` in shared Redis                                                                           | The import service manages another service's cache                                                               |
| C11 | Kafka type headers with fully qualified class names  | `KafkaProducerConfig` → `ADD_TYPE_INFO_HEADERS = true`; `ViewRefreshRequestedEvent` shared through `shared-models`                                                                            | Consumers must have the producer's exact class on their classpath                                                |
| C12 | Internal endpoints reachable through the gateway     | `api-gateway.yml` → `discovery.locator.enabled: true`                                                                                                                                         | `/dashboard/**` is routed automatically, so any future `/internal/**` endpoint would be public                   |

---

## Goals

1. **Single Responsibility:** target `gene-service` is the only runtime that contains `ProteinEntry` and related catalog persistence code.
2. **Database-per-Service:** only the `gene-service` database role can read or write `gene_data`. Every other
   role gets `permission denied`.
3. **Explicit contracts:** a thin `backend/libs/gene-model-lib` module publishes reused cross-service DTOs, Feign
   interfaces and reference event schemas. Service-specific/internal types remain local. The library contains no JPA,
   Spring Data, component scan or auto-configured beans.
4. **Reliable events:** catalog changes are published with at-least-once delivery through a transactional outbox.
   Consumers are idempotent.
5. **Infrastructure-only starter:** `common-starter` keeps cross-cutting concerns only (security, error handling,
   tracing, resilience, Kafka and datasource plumbing). Every feature is opt-in.
6. **Zero user-visible regression:** the public contracts in `documentation/api-contract.md` do not change, and the
   frontend is not modified.
7. **Separated audit flow:** common-starter provides producer-side `@Auditable` AOP instrumentation, the existing
   Kafka topic transports events asynchronously, and audit-service alone owns audit storage, search APIs and retention.

## Non-Goals

- Changing public `/api/**` routes or the internal `/internal/v1/**` contracts. The runtime/module identity changes to
  `gene-service` as part of this ticket.
- Change Data Capture with Debezium (ARCH-001 Phase 10). The outbox table is designed so CDC can replace the polling
  relay later.
- Replacing the catalog schema with a physically separate PostgreSQL cluster (ARCH-001 Phase 10).
- Building `export-service`, `nlq-service` or `structure-service` features. They only **consume** the new client
  once they exist.

---

## Target Architecture

```text
Browser -> API Gateway -> explicit /api/** service routes
                         /internal/** denied

import-service -- Feign chunk writes + completion --> gene-service
  | owns parsing/import_job                            | owns ProteinEntry + merge
  v                                                    | local search + statistics API
import_batch (import_svc)                               v
                                               gene_data (gene_service_svc only)
                                                       |
                                               transactional outbox -> Kafka
                                                       |
                             catalog.protein-ingestion.completed.v1
                                                       |
                                                       v
analytics-service refresh coordinator -- Feign statistics --> gene-service
  |
  +--> atomic save of aggregate results in analytics-owned refresh tables/views
  +--> cached KPI reads from those results
  +--> live filtered/compare requests use gene-service statistics API

catalog.protein-entry.changed.v1 remains a catalog contract for other consumers;
analytics does not consume per-entry events or maintain a protein projection.
export-service (later) uses gene-service internal read APIs.
```

### Module and Service Identity Map

| Component | Target module/artifact | Runtime ID and Java root | Preserved boundary |
|-----------|------------------------|--------------------------|--------------------|
| Catalog owner | `backend/services/gene-service` / `gene-service` | `gene-service` / `com.bioinformatics.geneservice` | Former `backend/dashboard`; public `/api/**`, internal `/internal/v1/**`, catalog objects, and Kafka contracts stay stable. |
| Audit event producer | `backend/libs/common-starter` | `common-starter` / `com.bioinformatics.common.audit` | Producer annotation, AOP, event context, and Kafka publishing only. |
| Audit persistence owner | `backend/services/audit-service` / `audit-service` | `audit-service` / `com.bioinformatics.auditservice` | Owns `audit_log`, migrations, persistence, search authorization, and retention. |
| Shared catalog contracts | `backend/libs/gene-model-lib` / `gene-model-lib` (planned) | Library; no runtime identity / `com.bioinformatics.gene.model.v1` | Reused contracts only; internal/service-specific types stay in their services. |

Apply runtime identity changes atomically across Config Server, discovery, gateway, Feign clients, Compose, images,
Kubernetes selectors, probes, and secrets. Do not change public API paths or introduce a `dashboard` runtime alias.

The legacy end-of-import refresh remains in parallel until parity sign-off (Q7). The final path has no cross-schema
SQL reads. Cached aggregates remain readable during a gene-service outage; refresh and live filtered/compare calls
depend on that API and expose errors explicitly. Configurable refresh scheduling defaults to 60 seconds; actual API
and publication time are measured separately.

### Audit Event Flow (separate from catalog persistence)

```text
Producer service @Auditable method
  → common-starter AOP aspect creates an audit event
  → existing Kafka topic: audit.events.log
  → audit-service consumes and persists to its audit_log schema
  → audit-service serves authorized, paginated search and applies retention
```

`common-starter` and producer services have no audit database credentials and do not contain audit entities, repositories,
search or retention logic. The audit-service schema is separate from `gene_data`; producer publication is asynchronous.
The audit event preserves actor, source-service and correlation ID as separate fields. Existing audit rows and Flyway
history are transferred/baselined or explicitly archived/purged according to the approved P−1 handoff plan.

---

## Acceptance Criteria

### AC-1 — Module isolation

```
Given the backend is built with ./mvnw verify
When the ArchUnit rules and the Maven Enforcer rules run
Then no class annotated with @Entity, @Repository or a catalog @Service exists in libs/common-starter
  and the class ProteinEntry exists only in the gene-service module
  and audit persistence entities and repositories exist only in audit-service
  and common-starter contains no audit JPA, datasource or migration dependency
  and import-service, analytics-service and export-service do not depend on spring-boot-starter-data-jpa
      entities from another service
  and the build fails if any of these rules is violated
```

### AC-2 — Database ownership

```
Given the Stage A database provisioning has run
When analytics_svc, import_svc or audit_service_svc accesses gene_data.protein_entry
Then PostgreSQL returns "permission denied for schema gene_data"
  and auth_svc, export_svc and every other non-owner role are also denied access to gene_data
  and only gene_service_svc (through gene_data_rw) can read or write the catalog schema on the primary
  and gene_service_svc has read-only access on the physical replica
```

### AC-3 — Import through the owner's API

```
Given an admin uploads a UniProt .dat file
When the import-service Spring Batch job processes a chunk
Then the chunk is sent to POST /internal/v1/protein-ingestions/{ingestionId}/entries on gene-service (via Eureka)
  and gene-service upserts the entries idempotently by accession
  and at the end import-service calls POST /internal/v1/protein-ingestions/{ingestionId}/completion
  and the import job reaches COMPLETED with the same processed and skipped counts as the pre-migration baseline
```

### AC-4 — Resilience of the import write path

```
Given gene-service is unavailable during an import
When a chunk write fails with 503 or a connection error
Then the chunk is retried with exponential backoff (max 3 attempts)
  and after that the step fails and the job is marked FAILED with an explicit error message
  and restarting the job resumes from the last committed chunk without creating duplicate proteins
```

### AC-5 — Reliable catalog events

```
Given a protein entry is created or updated in gene-service
When the database transaction commits
Then one ProteinEntryChangedEvent (CREATED or UPDATED) is written to the outbox in the same transaction
  and is published to catalog.protein-entry.changed.v1 with key = accession within 5 s (p95 in steady state)
  and no event is published if the transaction rolls back
  and the message carries no Java type header (__TypeId__)
  and ingestion completion commits exactly one completion marker to the outbox after all chunks succeed
  and the completion marker is eventually published to catalog.protein-ingestion.completed.v1 across relay restarts
```

### AC-6 — API-backed analytics refresh

```
Given analytics-service consumes catalog.protein-ingestion.completed.v1 after an import
When a refresh is due under the configured debounce and maximum scheduling delay (default 60 s)
Then it durably records the completion marker before acknowledging Kafka
  and calls gene-service POST /internal/v1/protein-entries/statistics with a scoped service token
  and atomically saves the returned complete aggregate set in analytics-owned refresh tables/views
  and keeps the last complete aggregate set readable until the replacement publication commits
  and dashboard KPIs/distributions match the legacy path and gene-service's counts
  and duplicate/out-of-order markers and restarts do not lose pending refresh work or corrupt published results
  and a marker received during a refresh causes a subsequent fetch
  and API/storage failure preserves the last complete set, records a visible failure and allows bounded retry/recovery
  and analytics does not read catalog tables or consume per-entry events on the new path
```

### AC-7 — Filtered analytics parity

```
Given a fixture set of at least 30 GeneSearchRequest combinations (all filter fields covered)
When the filtered analytics total is compared with gene-service POST /internal/v1/protein-entries/search totalElements
Then both counts are equal for every fixture
  and filtered/compare requests use gene-service statistics and retain the existing public JSON shape
  and all aggregate values match the legacy path on the same stable fixture dataset
```

### AC-8 — Internal API is not public

```
Given the gateway configuration after this ticket
When a client calls http://gateway:8080/gene-service/internal/v1/protein-entries/P12345
Then the gateway returns 404
  and a direct call to gene-service without a service token returns 401
  and a direct call to gene-service with a user access token returns 403
```

### AC-9 — Contract safety

```
Given the gene-model-lib module version N
When a consumer receives a payload that contains an unknown field
Then deserialization succeeds (tolerant reader)
  and the consumer contract tests (WireMock stubs) and the provider verification tests pass in CI
```

### AC-10 — No functional regression and independent startup

```
Given the full docker-compose stack
When each service is started on its own
Then it starts without waiting for gene-service to be healthy (import-service no longer depends_on backend)
  and /api/genes/**, /api/analytics/**, /api/admin/import/** keep the exact contracts in api-contract.md
  and the Playwright smoke suite (frontend/e2e) passes
```

### AC-11 — Hardened identity trust

```
Given a request supplies forged X-User-* or other internal headers
When it enters the gateway on any route, including a public authentication route
Then the gateway strips those headers before routing
  and public-route bypasses match exact HTTP method and path, not a path substring
  and discovery-based service routes and /internal/** are not exposed through the gateway (404)
  and services derive user identity only from validated bearer-token claims, never from X-User-* headers
  and a direct /internal/** call without a token returns 401, a user token returns 403,
      and a valid service token with the required audience and scope is authorized
  and correlation ID, actor and service identity remain distinct in propagated request and audit context
```

### AC-12 — Merge semantics

```
Given a light protein record is followed by a detailed record for the same accession
When gene-service merges both records
Then present non-null fields enrich/overwrite current values, absent fields and empty collections do not erase data,
  child collections merge additively by natural key and are never deleted,
  lower UniProt entry versions are stale no-ops, and replaying the same input is idempotent
  and each material change increments technical revision once and emits one entry-change event
```

### AC-13 — gene-service refactor and common audit instrumentation

```
Given the former dashboard module runs as gene-service and a producer opts into common audit instrumentation
When a method annotated with @Auditable completes
Then the module/runtime identity is gene-service with no stale dashboard package or service ID
  and the common-starter aspect creates and publishes an audit event to the existing audit.events.log topic
  and the event carries actor, source service and correlation ID as distinct fields
  and neither common-starter nor the producer service requires audit database credentials or audit persistence classes
```

### AC-14 — Audit-service owns the asynchronous audit lifecycle

```
Given audit-service is running with its own audit_log schema and Kafka consumer
When it consumes an audit event from audit.events.log
Then it persists the event idempotently and makes it available through an authorized, paginated/search API
  and retention policies are enforced by audit-service only
  and consumer restart/re-delivery tests show no silent loss or duplicate audit records
  and producers/common-starter cannot read or write the audit schema
```

---

## Key Design Decisions (summary — full rationale in `analyse.md`)

| ID | Decision | Status |
|----|----------|--------|
| D1 | `backend/libs/gene-model-lib` contains reused cross-service DTOs/contracts; service-specific and internal types stay local | Confirmed My Decision C |
| D2 | Import-service parses and sends Feign upsert chunks to gene-service; gene-service owns persistence and merge | Confirmed My Decision A |
| D3 | Ingestion completion triggers an API-backed statistics refresh into analytics-owned aggregate tables; no protein projection | Confirmed My Decision B (adapted) |
| D4 | Transactional outbox and polling relay publish catalog changes and ingestion completion | Confirmed My Decision OK |
| D5 | Entry-change events use technical `revision`; one compacted event per accession plus an ingestion-completion marker | Confirmed design |
| D6 | Stage A schema/role isolation is mandatory; Stage B physical split is deferred to ARCH-001 Phase 10 | Confirmed Q2 |
| D7 | Catalog objects move to `gene_data`; audit schema and migration ownership remain separate | Confirmed Q1/Q8 |
| D8 | Retain a single `common-starter` with explicit opt-in auto-configuration and optional infrastructure dependencies | Confirmed design; P1 implementation delivered, qualification gates remain |
| D9 | `/internal/**` is gateway-private and requires scoped service JWTs; users authenticate from validated bearer tokens | Confirmed design |
| D10 | Use OpenFeign for cross-service clients, with portable mapping annotations and contract tests | Confirmed design |
| D11 | Re-import merges present fields, preserves absent data, adds/merges children, and ignores lower source versions | Confirmed Q3/Q4; replaces earlier proposal |
| D12 | Strip inbound identity/internal headers, use exact public-route matching, and trust validated JWT claims only | Confirmed Q6 |
| D13 | Analytics freshness scheduling is configurable (60-second default); measure API duration and aggregate age separately | Confirmed Q5 |
| D14 | Rename the former dashboard module/runtime to `gene-service`; preserve public and internal API contracts | Confirmed Q1 |
| D15 | common-starter produces audit events over existing Kafka; audit-service alone owns audit persistence, APIs and retention | Confirmed Q8 |

---

## References

- `documentation/implementation/ARCH-001/overview.md`, `plan.md`, `analyse.md` — the earlier service extraction
- `documentation/domain-model.md` — authoritative schema (must be updated in Phase 9)
- `documentation/api-contract.md` — public REST contract (unchanged)
- `documentation/implementation/PERF-001/` — import throughput baseline (sequence allocation, batch sizes)
- `documentation/implementation/CACHE-001/` — cache names and eviction hooks
- Spring Cloud OpenFeign — https://docs.spring.io/spring-cloud-openfeign/reference/
- Transactional Outbox — https://microservices.io/patterns/data/transactional-outbox.html
- Database per Service — https://microservices.io/patterns/data/database-per-service.html

---

**Ticket Created**: 2026-09-29 **Depends on**: ARCH-001 Phases 0–3 (done)
**Estimated Effort**: L (plan rev. 12 estimates ≈ 54 eng-days / 8–10 weeks for 1–2 engineers;
re-estimate P6 after the aggregate-contract inventory; Stage B DB split excluded)
