# ARCH-002 — Protein Catalog Ownership & Database-per-Service Enforcement

## Description

`ARCH-001` split the monolith into services (Strangler Fig), but we kept one shared library, `libs/common-starter`,
that still holds the **protein catalog domain**: the JPA entities, the Spring Data repositories, the read service, and
the JPA `Specification` used to filter proteins. Every service that pulls in this starter also gets the
`protein_entry` schema definition, and two of them (`import-service`, `analytics-service`) read or write the
dashboard's tables directly.

So the platform behaves like a **distributed monolith**: services are deployed separately, but they share a schema,
share persistence code, and have to be released together.

This ticket makes the **`dashboard` service the single owner** of the protein catalog bounded context:

- the `protein_entry` aggregate and its child tables (`protein_feature`, `protein_keyword`, `keyword`,
  `protein_go_term`, `go_term`, `cross_reference`, `host_organism`, `protein_comment`, `protein_publication`)
- its JPA entities, repositories, specifications and services
- its Flyway migrations and its database schema / credentials

Other services reach catalog data **only** through:

- **Synchronous calls:** OpenFeign clients (resolved through Eureka) against a versioned internal API
  (`/internal/v1/**`), using decoupled DTOs.
- **Asynchronous events:** Kafka events published by `dashboard` through a transactional outbox.

> The Eureka service ID stays `dashboard`. Renaming it to `gene-service` / `catalog-service` (ARCH-001 Phase 4 naming)
> is **out of scope**. It can be done later behind the same client module.

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

1. **Single Responsibility:** `dashboard` is the only runtime that contains `ProteinEntry` and related persistence code.
2. **Database-per-Service:** only the `dashboard` database role can read or write the catalog schema. Every other
   role gets `permission denied`.
3. **Explicit contracts:** a thin `dashboard-client` module publishes DTOs, the Feign interfaces and the event
   payload schemas. It contains no JPA, no Spring Data and no auto-configured beans.
4. **Reliable events:** catalog changes are published with at-least-once delivery through a transactional outbox.
   Consumers are idempotent.
5. **Infrastructure-only starter:** `common-starter` keeps cross-cutting concerns only (security, error handling,
   tracing, resilience, Kafka and datasource plumbing). Every feature is opt-in.
6. **Zero user-visible regression:** the public contracts in `documentation/api-contract.md` do not change, and the
   frontend is not modified.

## Non-Goals

- Renaming `dashboard` or changing public `/api/**` routes.
- Change Data Capture with Debezium (ARCH-001 Phase 10). The outbox table is designed so CDC can replace the polling
  relay later.
- Replacing the trust model based on the gateway's `X-User-*` headers for `/api/**` (tracked separately; see
  `analyse.md` Q6).
- Building `export-service`, `nlq-service` or `structure-service` features. They only **consume** the new client
  once they exist.

---

## Target Architecture

```text
                              ┌──────────────────────┐
     Browser ── HTTPS ──────► │     API Gateway      │   /api/** only; /internal/** denied
                              └──────────┬───────────┘
                                         │ lb://
          ┌──────────────────────────────┼───────────────────────────────┐
          ▼                              ▼                               ▼
 ┌─────────────────┐   Feign (sync)  ┌─────────────────────────────┐  Feign  ┌──────────────────┐
 │ import-service  │ ──────────────► │ dashboard (Protein Catalog) │ ◄────── │ export-service   │
 │ parse .dat/.tsv │ POST /internal/ │  • ProteinEntry aggregate   │ scan    │ (PIPE-001, later)│
 │ owns import_job │ v1/protein-     │  • /api/genes/** (public)   │         └──────────────────┘
 └───────┬─────────┘ ingestions/...  │  • /internal/v1/** (S2S)    │
         │                           │  • Outbox → Kafka relay     │
         │                           └──────┬───────────────┬──────┘
         │                                  │ JDBC (only)   │ Kafka (async)
         ▼                                  ▼               ▼
 ┌─────────────────┐              ┌──────────────────┐   ┌──────────────────────────────────────┐
 │ schema          │              │ schema dashboard │   │ catalog.protein-entry.changed.v1     │
 │ import_batch    │              │ (role dashboard_ │   │   key = accession, compacted         │
 │ (role import_   │              │  svc ONLY)       │   │ catalog.protein-ingestion.completed.v1│
 │  svc)           │              └──────────────────┘   └──────────────────┬───────────────────┘
 └─────────────────┘                                                        │ consume
                                                                            ▼
                                                           ┌───────────────────────────────────┐
                                                           │ analytics-service                  │
                                                           │  • analytics.protein_fact (CQRS    │
                                                           │    read model, own schema)         │
                                                           │  • materialized views on its OWN   │
                                                           │    projection                      │
                                                           └───────────────────────────────────┘
```

---

## Acceptance Criteria

### AC-1 — Module isolation

```
Given the backend is built with ./mvnw verify
When the ArchUnit rules and the Maven Enforcer rules run
Then no class annotated with @Entity, @Repository or a catalog @Service exists in libs/common-starter
  and the class ProteinEntry exists only in the dashboard module
  and import-service, analytics-service and export-service do not depend on spring-boot-starter-data-jpa
      entities from another service
  and the build fails if any of these rules is violated
```

### AC-2 — Database ownership

```
Given the Stage A database provisioning has run
When analytics_svc or import_svc runs SELECT 1 FROM dashboard.protein_entry
Then PostgreSQL returns "permission denied for schema dashboard"
  and only dashboard_svc can read or write the dashboard schema (on both the primary and the replica)
```

### AC-3 — Import through the owner's API

```
Given an admin uploads a UniProt .dat file
When the import-service Spring Batch job processes a chunk
Then the chunk is sent to POST /internal/v1/protein-ingestions/{ingestionId}/entries on dashboard (via Eureka)
  and dashboard upserts the entries idempotently by accession
  and at the end import-service calls POST /internal/v1/protein-ingestions/{ingestionId}/completion
  and the import job reaches COMPLETED with the same processed and skipped counts as the pre-migration baseline
```

### AC-4 — Resilience of the import write path

```
Given dashboard is unavailable during an import
When a chunk write fails with 503 or a connection error
Then the chunk is retried with exponential backoff (max 3 attempts)
  and after that the step fails and the job is marked FAILED with an explicit error message
  and restarting the job resumes from the last committed chunk without creating duplicate proteins
```

### AC-5 — Reliable catalog events

```
Given a protein entry is created or updated in dashboard
When the database transaction commits
Then one ProteinEntryChangedEvent (CREATED or UPDATED) is written to the outbox in the same transaction
  and is published to catalog.protein-entry.changed.v1 with key = accession within 5 s (p95)
  and no event is published if the transaction rolls back
  and the message carries no Java type header (__TypeId__)
```

### AC-6 — Analytics read model

```
Given analytics-service consumes catalog.protein-entry.changed.v1
When it receives duplicated or out-of-order events for the same accession
Then analytics.protein_fact ends in the state of the highest entry version (idempotent, version-guarded upsert)
  and after catalog.protein-ingestion.completed.v1 plus a quiet period, the materialized views are refreshed
  and the dashboard KPIs match dashboard's own counts (parity check)
```

### AC-7 — Filtered analytics parity

```
Given a fixture set of at least 30 GeneSearchRequest combinations (all filter fields covered)
When the filtered analytics total is compared with dashboard POST /internal/v1/protein-entries/search totalElements
Then both counts are equal for every fixture
```

### AC-8 — Internal API is not public

```
Given the gateway configuration after this ticket
When a client calls http://gateway:8080/dashboard/internal/v1/protein-entries/P12345
Then the gateway returns 404
  and a direct call to dashboard without a service token returns 401
  and a direct call with a user access token returns 403
```

### AC-9 — Contract safety

```
Given the dashboard-client module version N
When a consumer receives a payload that contains an unknown field
Then deserialization succeeds (tolerant reader)
  and the consumer contract tests (WireMock stubs) and the provider verification tests pass in CI
```

### AC-10 — No functional regression and independent startup

```
Given the full docker-compose stack
When each service is started on its own
Then it starts without waiting for dashboard to be healthy (import-service no longer depends_on backend)
  and /api/genes/**, /api/analytics/**, /api/admin/import/** keep the exact contracts in api-contract.md
  and the Playwright smoke suite (frontend/e2e) passes
```

---

## Key Design Decisions (summary — full rationale in `analyse.md`)

| ID  | Decision                                                                                                                      | Status        |
|-----|-------------------------------------------------------------------------------------------------------------------------------|---------------|
| D1  | Hybrid contract: a shared `dashboard-client` module for Feign and DTOs. Event consumers keep their own tolerant-reader copies | Proposed      |
| D2  | Import writes go through a Feign bulk-upsert endpoint on `dashboard`, not through Kafka commands                              | Proposed      |
| D3  | Analytics owns a CQRS projection (`analytics.protein_fact`) fed by Kafka, not Feign aggregation calls                         | Proposed      |
| D4  | Transactional outbox with a polling relay (Debezium-ready)                                                                    | Proposed      |
| D5  | One event per entry, compacted topic keyed by accession, plus an ingestion-completed marker event                             | Proposed      |
| D6  | Stage A: logical isolation (schemas + roles + REVOKE) is mandatory. Stage B: dedicated catalog cluster is optional            | Proposed — Q2 |
| D7  | Move catalog tables from `public` to schema `dashboard` (`ALTER TABLE … SET SCHEMA`, metadata-only)                           | Proposed — Q1 |
| D8  | Keep a single `common-starter`, stripped of business code, with optional dependencies and explicit auto-configuration         | Proposed      |
| D9  | `/internal/**` requires a service JWT (`typ=service`, scopes `catalog:read` / `catalog:write`)                                | Proposed      |
| D10 | Keep OpenFeign as requested (maintenance mode noted). Spring HTTP Interface clients are a drop-in alternative                 | Proposed      |

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
**Estimated Effort**: L (about 4–6 weeks for 1–2 engineers; Stage B DB split excluded)

