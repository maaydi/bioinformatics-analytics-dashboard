# ARCH-002 — Ambiguity Analysis & Architecture Decisions

> **Status:** Q1–Q8 have answers recorded below. D1–D11 are the existing design proposals; D12–D15 capture the
> confirmed trust, freshness, refactor and audit ownership decisions. Remaining operational choices are listed as
> implementation gates rather than unresolved ownership questions.

---

## 1. Current-State Inventory (what couples services to the catalog)

### 1.1 `libs/common-starter` — business code that must leave

| Artifact                                                                                                                                                                              | Package                                            | Used by today                                          | Target owner                                             |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|--------------------------------------------------------|----------------------------------------------------------|
| `ProteinEntry`, `ProteinFeature`, `ProteinPublication`, `ProteinComment`, `Keyword`, `HostOrganism`, `GoTerm`, `CrossReference`                                                       | `common.gene.entity`                               | dashboard, import-service, analytics-service           | **dashboard** `catalog.entity`                           |
| `ProteinEntryRepository`, `ProteinFeatureRepository`, `ProteinPublicationRepository`, `ProteinCommentRepository`, `KeywordRepository`, `GoTermRepository`, `CrossReferenceRepository` | `common.gene.repository`                           | dashboard, import-service, analytics-service (extends) | **dashboard** `catalog.repository`                       |
| `ProteinEntryService`                                                                                                                                                                 | `common.gene.service`                              | dashboard                                              | **dashboard** `catalog.service.ProteinEntryQueryService` |
| `GeneSpecification`                                                                                                                                                                   | `common.gene.specification`                        | dashboard, analytics-service                           | **dashboard** `catalog.specification`                    |
| `GeneSpecification` (UniProt query builder)                                                                                                                                           | `common.providers.uniprotkb.gene.specification`    | dashboard                                              | **dashboard** `providers.uniprotkb.query`                |
| `UniprotKbRestService`, `SuggesterRestService`, `UniProtApiClient`, … + `uniprot.dto.*`, `providers.uniprotkb.dto.*`, `UniprotRestClientConfig`, `RetryConfig`                        | `common.providers.uniprotkb.*`, `common.uniprot.*` | dashboard only (REMOTE-001)                            | **dashboard** `providers.uniprotkb.client`               |
| `GeneSearchRequest`                                                                                                                                                                   | `common.models.gene`                               | dashboard, analytics-service (compare / filtered)      | **dashboard-client** (search contract, owned by catalog) |
| `SavedFilterDto`                                                                                                                                                                      | `common.models.filter`                             | dashboard, import-service (`SavedFilterClient`)        | **dashboard-client**                                     |
| `MalformedUniprotFileException`, `ImportAlreadyRunningException`, `UnsupportedFileTypeException`                                                                                      | `common.exception`                                 | import-service                                         | **import-service**                                       |
| `ExportRowCapExceededException`, `DuplicateFilterNameException`                                                                                                                       | `common.exception`                                 | dashboard                                              | **dashboard**                                            |
| `PasswordUpdateException`                                                                                                                                                             | `common.exception`                                 | auth-service                                           | **auth-service**                                         |

### 1.2 `libs/shared-models` — coupling constants

| Artifact                                                                                    | Problem                                                              | Target                                                                                    |
|---------------------------------------------------------------------------------------------|----------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| `DbSchema` (`GENES_SCHEMA = "public"`, …)                                                   | Publishes every service's physical schema to every other service     | **Delete**. Each service keeps a private constant                                         |
| `KafkaTopics.ANALYTICS_VIEW_REFRESH_REQUESTED`                                              | The topic name belongs to a producer, not to a platform kernel       | Replaced by `CatalogTopics` in `dashboard-client`. The legacy topic is removed in Phase 6 |
| `ViewRefreshRequestedEvent`                                                                 | A shared event class with FQCN type headers means a shared classpath | **Deprecated → deleted** (replaced by `ProteinIngestionCompletedEvent`)                   |
| `UserPrincipal`, `AppHeaders`, `AppClaims`, `TypeClaimValue`, `Constants`, `TypedCacheSpec` | Platform-level (security, cache). Acceptable in a kernel             | **Keep**                                                                                  |

### 1.3 Direct data access outside the owner

| Service           | Access                                                                                                             | Type                     |
|-------------------|--------------------------------------------------------------------------------------------------------------------|--------------------------|
| import-service    | `ProteinAggregateItemWriter` → `proteinEntryRepository.saveAll(...)` + child repositories                          | **WRITE** (JPA)          |
| import-service    | `ImportUniprotSkipListener`, `ImportProgressChunkListener` typed on `ProteinEntry`                                 | Compile-time             |
| import-service    | `PostImportCacheEvictionListener` clears all Redis caches (including dashboard's)                                  | Cache ownership          |
| analytics-service | `AnalyticsProteinRepository extends ProteinEntryRepository`                                                        | READ (JPA)               |
| analytics-service | `AnalyticsViewProteinRepositoryImpl` → Criteria API on `ProteinEntry`                                              | READ (JPA)               |
| analytics-service | `V1__analytics_schema.sql` materialized views → `public.protein_entry`, `public.keyword`, `public.protein_keyword` | READ (SQL, cross-schema) |

### 1.4 Infrastructure

| Item                                                                                                                                        | Problem                                                                    |
|---------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| `.env` → `COMMON_DATASOURCE_PRIMARY_*`, `SPRING_DATASOURCE_*` shared by all containers (`env_file: .env`)                                   | Same superuser credentials everywhere                                      |
| `import-service.depends_on.backend`                                                                                                         | Startup coupling caused by JPA `ddl-auto=validate` on dashboard's tables   |
| `CommonAutoConfiguration` `@ComponentScan("com.bioinformatics.common")`                                                                     | A library that component-scans itself leaks every bean into every consumer |
| `common-starter` pom: `data-jpa`, `postgresql`, `kafka`, `webflux`, `gateway-server-webmvc`, `data-redis`, `bucket4j` are all compile scope | Every service inherits the whole stack                                     |
| `KafkaProducerConfig` → `ADD_TYPE_INFO_HEADERS = true`; consumer `TRUSTED_PACKAGES = com.bioinformatics.*`                                  | Class-name coupling between producer and consumers                         |
| Gateway `discovery.locator.enabled: true`                                                                                                   | Auto-exposes `/{service-id}/**` for every registered service               |
| `GatewayUserAuthenticationFilter` trusts `X-User-Id` / `X-User-Role` headers                                                                | Any in-network caller can impersonate a user (see Q6)                      |

---

## 2. Decisions

### D1 — How DTOs are shared: shared client module vs duplication

| Option                                                       | Pros                                                                                | Cons                                                                          |
|--------------------------------------------------------------|-------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| A. Shared `dashboard-client` module (DTOs + Feign interface) | Compile-time safety, single source of truth, easy to use in a monorepo              | Consumers take a binary dependency on the provider's release train            |
| B. Each consumer duplicates DTOs                             | Full autonomy, a consumer maps only the fields it needs                             | Drift risk, duplicated effort, needs contract tests to catch breaking changes |
| **C. Hybrid (recommended)**                                  | Sync API: provider-owned SDK (A). Async events: consumers own their read models (B) | Two conventions to document                                                   |

**Decision (proposed): C.**

- `dashboard-client` contains **only**: Java records (DTOs), Feign interfaces, topic-name constants and event payload
  records (as a reference schema). Its dependencies are `jakarta.validation-api`, `jackson-annotations` and
  `spring-cloud-openfeign-core` (`provided`/`optional`). There is **no** Spring Boot auto-configuration, **no**
  JPA and **no** component scan.
- The provider controller **does not implement** the Feign interface. The Spring Cloud OpenFeign documentation
  explicitly advises against sharing one interface between server and client. Server/client alignment is enforced by
  contract tests (Phase 3/4).
- Tolerant reader rule: every DTO in `dashboard-client` is annotated `@JsonIgnoreProperties(ignoreUnknown = true)`.
  Changes are **additive only** within `v1`. A breaking change means `/internal/v2` plus a new topic version.
- Kafka consumers may deserialize into **their own local record** that holds only the fields they use. The
  `dashboard-client` event records are the published reference schema, and `documentation/event-catalog.md` is the
  human-readable schema.

### D2 — Import write path

| Option                                                            | Pros                                                                                                                  | Cons                                                                                                                  |
|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| **A. Feign bulk upsert per chunk (recommended)**                  | Keeps Spring Batch chunk semantics (retry, skip, restart), natural backpressure, job status reflects real persistence | HTTP overhead; payload size (sequences). Needs gzip and chunk tuning                                                  |
| B. Kafka command topic (`catalog.protein-entry.upsert-requested`) | Fully async, buffers dashboard outages                                                                                | The import job can no longer know when data is persisted; error reporting is harder; a second write path to reconcile |
| C. Move the whole import pipeline into dashboard                  | No network hop                                                                                                        | Brings back the monolith; defeats ARCH-001 Phase 3                                                                    |

**Decision (proposed): A.**

- `import-service` owns the **file parsing**, the **job lifecycle** (`import_batch.import_job`, Spring Batch
  metadata) and progress reporting.
- `dashboard` owns **persistence and invariants** of the aggregate.
- An anti-corruption layer is added in `import-service`: the line processors build a local parse model
  (`ParsedProtein`), and the writer maps it to `ProteinEntryUpsertDto` (MapStruct).

### D3 — How analytics gets catalog data

| Option                                                                                 | Pros                                                                                                                                      | Cons                                                                                                          |
|----------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| **A. CQRS projection `analytics.protein_fact` fed by Kafka (recommended)**             | True autonomy (analytics works when dashboard is down); join-free wide table is faster for aggregations; MVs built on data analytics owns | Filter semantics are duplicated (`ProteinFactSpecification`); needs a backfill; eventual consistency          |
| B. Feign aggregation API on dashboard (`POST /internal/v1/protein-entries/statistics`) | No duplication; `GeneSpecification` stays with the owner                                                                                  | Analytics becomes a thin proxy with a runtime dependency on dashboard; heavy aggregations load the catalog DB |
| C. Keep cross-schema SQL reads with read-only grants                                   | Zero effort                                                                                                                               | **Rejected**: it is the problem this ticket solves                                                            |

**Decision (proposed): A**, with B as the documented fallback if the parity tests (AC-7) show that the filter
semantics cannot be reproduced reliably.

- The projection is **denormalized**: scalar columns plus `TEXT[]` columns (`keywords`, `go_term_ids`, `go_aspects`,
  `feature_types`, `cross_ref_sources`, `lineage`) with GIN indexes, and a generated `search_vector`.
- `GeneSearchRequest` (from `dashboard-client`) stays the **request contract**. Each side translates it into its own
  predicates.

### D4 — Event publication reliability

| Option                                                        | Delivery                     | Notes                                                                                                      |
|---------------------------------------------------------------|------------------------------|------------------------------------------------------------------------------------------------------------|
| `KafkaTemplate.send()` inside `@Transactional`                | Can publish phantom events   | Rejected                                                                                                   |
| `@TransactionalEventListener(AFTER_COMMIT)` → `KafkaTemplate` | At most once (lost on crash) | Rejected for projection-feeding events                                                                     |
| **Transactional outbox + polling relay (recommended)**        | At least once                | One table, JDBC batch insert (fits bulk ingestion), `FOR UPDATE SKIP LOCKED` relay; Debezium-ready later   |
| Spring Modulith event externalization                         | At least once                | Good off-the-shelf option. One registry row per event plus async dispatch; less control over bulk batching |

**Decision (proposed): hand-rolled, minimal outbox.** A single ingestion can produce about 570k events (PERF-001
dataset), so we need explicit control over batch sizes and cleanup.

### D5 — Topic and event granularity

- `catalog.protein-entry.changed.v1`: **one event per entry**, `key = accession`, `cleanup.policy=compact`,
  6 partitions. Per-key ordering is guaranteed. Compaction turns the topic into a replayable snapshot for new
  projections.
- `catalog.protein-ingestion.completed.v1`: one marker per ingestion, `key = ingestionId`, `cleanup.policy=delete`,
  3 partitions. It is a **hint** to refresh materialized views. It is **not** a correctness barrier, because
  ordering across topics is not guaranteed; the consumer uses a quiet-period debounce.
- Envelope fields: `eventId` (UUID), `eventType`, `schemaVersion`, `occurredAt`, `aggregateVersion`
  (`entry_version` or a monotonic `updated_at`), `correlationId`, `payload`.
- Type headers are **disabled** (`spring.json.add.type.headers=false`). Consumers bind to a default local type.
- Legacy topics `analytics.view-refresh.requested` and `protein.events.imported` are deprecated in Phase 5 and
  removed in Phase 6.

### D6 — Level of database isolation

| Stage                                   | What                                                                                                                    | Cost                                         |
|-----------------------------------------|-------------------------------------------------------------------------------------------------------------------------|----------------------------------------------|
| **A — Logical (mandatory in ARCH-002)** | Same PostgreSQL cluster; one schema and one login role per service; `REVOKE ALL … FROM PUBLIC`; per-service credentials | Low; reversible                              |
| B — Physical (optional, see Q2)         | Dedicated `catalog-db` (+ replica) for dashboard; `platform-db` for auth, analytics and import                          | Medium: more containers, backups, monitoring |

### D7 — Catalog schema `gene_data`; audit schema separately owned

- `ALTER TABLE public.<t> SET SCHEMA gene_data` is metadata-only (instant, no data rewrite). Indexes, constraints and
  owned sequences move with the table. Functions (the FTS function from V3) and standalone sequences must be moved
  explicitly.
- Reasons: least-privilege grants on a dedicated schema, no accidental unqualified access, symmetry with `auth`,
  `analytics` and `import_batch`.
- The gene-service catalog move includes `saved_filter` but excludes `audit_log` and its sequence. Audit records live
  in the separate `audit_log` schema owned only by audit-service; the database/cluster placement and existing-history
  handoff must be approved and rehearsed before cutover.
- This step runs **after** analytics stops reading the catalog tables (Phase 5), otherwise the MVs would break.

### D8 — Shape of `common-starter`

| Option                                                                                                                                                                                                      | Decision                                                                       |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| **Single starter with business code removed, `<optional>true</optional>` heavy dependencies, `@ConditionalOnClass` / `@ConditionalOnProperty`, explicit `AutoConfiguration.imports` (no `@ComponentScan`)** | **Recommended now**                                                            |
| Split into `common-web-starter`, `common-data-starter`, `common-kafka-starter`, `common-cache-starter`                                                                                                      | Deferred. Revisit if a service still pulls in unwanted transitive dependencies |

Cleanup items: remove `spring-cloud-starter-gateway-server-webmvc` (a gateway concern) and the unused
`spring-boot-starter-oauth2-client`; choose **one** web stack per service (servlet vs reactive).

The audit instrumentation is a deliberate cross-cutting exception to “business code removed”: common-starter may
provide the `@Auditable` annotation, AOP aspect, event construction and Kafka publisher, but no audit entity,
repository, datasource, schema migration, search API or retention implementation.

### D9 — Service-to-service authentication on `/internal/**`

- `/internal/**` is **never** routed by the gateway: the discovery locator is disabled and an explicit deny route
  returns 404.
- Callers attach a short-lived **service JWT** (`typ=service` → `TypeClaimValue.SERVICE_TOKEN`) through a Feign
  `RequestInterceptor` from `common-starter`. The token carries `sub=<service-id>` and scopes
  `catalog:read` / `catalog:write`.
- `dashboard` security: `/internal/v1/protein-entries/**` requires `SCOPE_catalog:read`;
  `/internal/v1/protein-ingestions/**` requires `SCOPE_catalog:write`. A user token gets 403.
- Token issuance reuses the ARCH-001 AC-7 mechanism (auth-service). Until auth-service issues tokens through
  client-credentials, `common-starter` can mint service tokens with the shared HMAC key (`app.jwt.secret`). That is
  an interim measure, documented as risk R8.

### D10 — OpenFeign vs Spring HTTP Interface clients

- OpenFeign is **feature-complete / in maintenance mode**. Spring Framework 7 / Boot 4 provide declarative HTTP
  Interface clients (`@HttpExchange`, `@ImportHttpServices`) that work with the load-balanced `RestClient.Builder`.
- **Decision:** use OpenFeign as requested; `import-service` already uses it (`SavedFilterClient`). Keep interfaces
  free of Feign-only features (use only `@GetMapping`/`@PostMapping`/`@PathVariable`/`@RequestBody`/`@RequestParam`),
  so a later move to `@HttpExchange` is mechanical.

### D11 — Re-import (upsert) semantics

- Confirmed in Q3/Q4: merge present scalar values, preserve absent values, add/merge child collections without deleting,
  and treat a lower UniProt `entry_version` as stale. Technical `revision` drives catalog event ordering.
- Reapplying the same chunk is **idempotent**; see the normative merge matrix in ARCH-002 plan §3.2.

### D12 — Header trust model

- Confirmed in Q6: gateway strips inbound identity/internal headers, exact public route matching is used, services
  authenticate from validated JWT claims, and service-to-service calls use scoped service tokens. See ARCH-002 plan §3.5.

### D13 — Configurable analytics freshness

- Confirmed in Q5: projection refresh uses configurable quiet period and maximum staleness, defaulting to 60 seconds.

### D14 — gene-service module refactor

- Confirmed in Q1: move `backend/dashboard` to `backend/services/gene-service`, with artifact/runtime identity
  `gene-service` and root package `com.bioinformatics.geneservice`; keep public `/api/**` and internal `/internal/v1/**`
  contracts stable.

### D15 — Audit event producer and audit-service ownership

- Common-starter supplies producer-side `@Auditable` AOP instrumentation and publishes events asynchronously to the
  existing Kafka topic `audit.events.log`. It has no audit database connection or persistence responsibilities.
- The new audit-service consumes that topic and exclusively owns the `audit_log` schema, migrations, persistence,
  authorized search APIs and retention policies. Producer services require Kafka access only, not audit DB credentials.
- Audit events preserve actor, source-service and correlation ID as distinct fields. Delivery/retry, duplicate handling,
  API authorization and event payload allow-listing are specified and tested as part of the service implementation.
- Historical records and dashboard Flyway V13–V17 require an explicit transfer/baseline or archive/purge decision and
  rehearsed cutover; they are not moved into `gene_data` or blindly replayed.

---

## 3. Confirmed Questions and Implementation Gates

| ID | Priority | Question                                                                                                                                        | Proposed default                                                                                       | Response                                                                                                                                                                          |
|----|----------|-------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Q1 | 🟡       | Schema name for dashboard-owned tables: `dashboard` or `catalog`?                                                                               | `dashboard` (matches the service ID and the existing `auth` / `analytics` / `import_batch` convention) | gene_data as dashboard will be renamed to gene-service in the future.                                                                                                             |
| Q2 | 🔴       | Is Stage B (a physically separate catalog PostgreSQL cluster) in scope for ARCH-002, or deferred to ARCH-001 Phase 10?                          | Deferred; only Stage A is mandatory                                                                    | keep it for arch-001 phase 10.                                                                                                                                                    |
| Q3 | 🔴       | Re-import of an existing accession: **replace**, **merge**, or **reject**?                                                                      | Replace (D11)                                                                                          | merge in gene consumer would be good so if data exists from file which is light weight and them imported from api which is detailed it will be updated by new data so merge works |
| Q4 | 🟡       | Is there any use case for **deleting** proteins (single or bulk "purge before import")?                                                         | No for now; `DELETED` event type is reserved, and a tombstone is emitted if it is introduced           | No delete for existing data only add or merge ( update current with new data )                                                                                                    |
| Q5 | 🔴       | Acceptable analytics staleness after an import completes (SLA)?                                                                                 | ≤ 60 s after `ingestion.completed` (quiet period 15 s + refresh time)                                  | ir depends on data size and hardware resource so make it configurable and default to 60s.                                                                                         |
| Q6 | 🟡       | Is hardening the gateway header trust model (`X-User-*`) in scope?                                                                              | Out of scope; `/internal/**` gets service JWT (D9), `/api/**` unchanged                                | yes as other services are using this header to get user info so it should be hardened. and for traçability                                                                        |
| Q7 | 🟡       | During the transition, should the legacy `analytics.view-refresh.requested` flow keep running in parallel (shadow mode) until parity is proven? | Yes, until the Phase 5 exit criteria are met                                                           | the refresh view is done at the end of import so it should be kept in parallel until parity is proven.                                                                            |
| Q8 | ✅       | Where should audit instrumentation, transport and persistence live?                                                                        | Common producer instrumentation publishes events; audit-service persistence ownership to be confirmed | Common-starter emits audit events; existing Kafka topic `audit.events.log` transports them; new audit-service owns schema, persistence, search and retention. Historical-data/Flyway handoff is a P−1 gate. |

---

## 4. Assumptions

1. The monorepo keeps a single `${revision}` for all modules, so `dashboard-client` is versioned with the build.
   Consumers still treat it as a **published contract** (additive changes only).
2. Kafka runs single-broker in local dev (`replication-factor 1`). Production sizing (RF=3, `min.insync.replicas=2`)
   belongs to the deployment and is out of scope here.
3. The `postgres-replica` is physical streaming replication, so roles, grants and schemas replicate automatically.
4. The PERF-001 import throughput is the baseline. The Feign write path must stay within **+30 %** total import time (to
   be measured in Phase 4).
5. Kafka is already provisioned with the `audit.events.log` topic. Audit producers and audit-service use that existing
   transport; no parallel synchronous persistence path is introduced.

