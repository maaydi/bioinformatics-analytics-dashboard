# ARCH-002 — Implementation Plan (rev. 2)

> **Revision history**
>
> | Rev | Date       | Change                                                                                                                                                                  |
> |-----|------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
> | 1   | 2026-09-29 | First phased plan derived from `overview.md` and `analyse.md`                                                                                                           |
> | 2   | 2026-09-29 | Review against the Q1–Q7 answers and the codebase: schema `gene_data`, merge semantics, header-trust hardening in scope, phase re-ordering, 15 review findings (§1)     |

---

## 1. Review findings (rev. 1 → rev. 2)

Each finding was verified against the code or the Q1–Q7 answers and is resolved in this revision.

| ID  | Finding                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Evidence                                                                      | Resolution in rev. 2                                                                                                                                             |
|-----|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| F1  | Rev. 1 kept schema `dashboard`; the Q1 answer requires **`gene_data`** (the service will be renamed `gene-service`)                                                                                                                                                                                                                                                                                                                                                                    | Q1 answer; existing convention `export_data`                                  | Schema `gene_data` everywhere (§3.1); AC-2 updated in `overview.md`                                                                                              |
| F2  | `entry_version` is **already** the UniProt `DT` entry version (`SMALLINT`). Using it as the event aggregate version (D5/D11) would mix source data with technical versioning                                                                                                                                                                                                                                                                                                           | `domain-model.md` l.56 and l.546; `ProteinEntry.entry_version`                | New technical column `revision BIGINT` drives events and projection ordering; `entry_version` stays UniProt data and becomes the merge staleness guard           |
| F3  | Rev. 1 removed catalog classes from `common-starter` in Phase 2, but import-service (`ProteinAggregateItemWriter`) and analytics-service (`AnalyticsProteinRepository extends ProteinEntryRepository`) still compile against them until they are migrated                                                                                                                                                                                                                              | C4, C5 in `overview.md`                                                       | Strangler order: dashboard gets its own copy in P4; the legacy package is `@Deprecated` and deleted only in **P8**, after P5 and P6                              |
| F4  | Rev. 1 Phase 3 ingestion endpoint wrote outbox rows that were only created in Phase 4                                                                                                                                                                                                                                                                                                                                                                                                  | Rev. 1 §3.2 vs §4.1                                                           | Outbox, merge service and internal API are delivered together in **P4** (provider side), before the import cutover in **P5**                                     |
| F5  | The Q3 answer is **merge** ("new data updates current"); rev. 1 left source precedence and empty-collection behaviour open                                                                                                                                                                                                                                                                                                                                                             | Q3, Q4 answers                                                                | Normative merge rules in §3.2: present-field last-writer-wins, additive collections, never delete, UniProt-version downgrade guard                               |
| F6  | The Q6 answer puts header hardening **in scope**. The code shows concrete weaknesses: (a) `JwtGatewayFilter.shouldNotFilter` uses `path::contains` on POST, so any POST path containing `auth/login` bypasses JWT validation **and** forwards client-supplied `X-User-*` headers; (b) inbound `X-User-*` are never stripped on bypassed routes; (c) every service authenticates from `X-User-*` via `GatewayUserAuthenticationFilter`, so any in-network caller can impersonate a user | `JwtGatewayFilter` l.85-89; `GatewayUserAuthenticationFilter` l.52-65         | New phase **P3** + AC-11 (§3.5)                                                                                                                                  |
| F7  | `SavedFilterClient` (import-service) calls the **public** `/api/saved-filters/{id}` and **forges** `X-User-Id` / `X-User-Role` headers                                                                                                                                                                                                                                                                                                                                                 | `SavedFilterClient` l.15-18                                                   | Replaced by `GET /internal/v1/saved-filters/{id}` with a service token carrying an `act` (actor) claim — P3                                                      |
| F8  | Dashboard Flyway history is `public.flyway_schema_history` (no `default-schema` configured). Switching `default-schema` to `gene_data` would make Flyway see an empty history and re-run V1                                                                                                                                                                                                                                                                                            | `gitea/config-repo/application.yml` Flyway block; no dashboard schema setting | Controlled history relocation runbook + idempotent move migration (§3.1, P7)                                                                                     |
| F9  | Disabling Kafka type headers globally (C11) would break the legacy `ViewRefreshRequestedEvent` consumer, which must keep running in shadow mode (Q7)                                                                                                                                                                                                                                                                                                                                   | `KafkaProducerConfig` l.46 `ADD_TYPE_INFO_HEADERS = true`                     | P1 binds the legacy consumer to an explicit default type **before** headers are disabled                                                                         |
| F10 | Shadow mode (Q7) needs two comparable read paths; rev. 1 only said "measure both"                                                                                                                                                                                                                                                                                                                                                                                                      | Q7 answer                                                                     | Two MV sets (legacy / projection) + read switch `analytics.read-model.source` + automated parity job (P6)                                                        |
| F11 | The compacted topic is empty on day 1, so the projection cannot be built from it                                                                                                                                                                                                                                                                                                                                                                                                       | D3 / D5                                                                       | Dashboard **republish** operation writes snapshot events through the outbox (P4) for backfill (P6)                                                               |
| F12 | AC-5 "published within 5 s (p95)" is not achievable while a 570 k-entry ingestion backlog drains                                                                                                                                                                                                                                                                                                                                                                                       | D4 (≈ 570 k events per ingestion)                                             | AC-5 latency measured in steady state; bulk ingestion measured as backlog drain time (P9)                                                                        |
| F13 | Internal search must not be dispatched to the remote UniProt provider (REFACTOR-001 / REMOTE-001 `X-Data-Provider`), otherwise AC-7 compares against remote data                                                                                                                                                                                                                                                                                                                       | Provider dispatcher; `AppHeaders.DATA_PROVIDER`                               | `/internal/v1/protein-entries/search` always uses the local PostgreSQL catalog                                                                                   |
| F14 | `analytics-service` local `application.yml` sets `spring.flyway.default-schema: auth`; it only works because Config Server overrides it. After role isolation, the fallback would fail on `auth`                                                                                                                                                                                                                                                                                       | `analytics-service/src/main/resources/application.yml` l.20-21                | Fixed in P7 (fallback `analytics`)                                                                                                                               |
| F15 | Public API DTO `GeneSearchRequest` is also the analytics public request body; moving it into the **internal** client couples a public contract to an internal SDK                                                                                                                                                                                                                                                                                                                      | `analyse.md` §1.1                                                             | `dashboard-client` exposes `ProteinSearchCriteria` (internal v1). Analytics keeps its own public request record with an **identical JSON shape** (contract test) |

---

## 2. Confirmed decisions (Q1–Q7)

| Question | Answer (2026-09-29)                                                                | Applied as                                                                                                                                                                                                                                   |
|----------|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Q1       | Schema `gene_data` — `dashboard` will be renamed `gene-service`                    | Schema `gene_data`; service-neutral names in contracts (`catalog.*` topics, `com.bioinformatics.catalog.client.v1` packages); Feign service ID configurable (`catalog.client.service-id`, default `dashboard`), so the rename is config-only |
| Q2       | Stage B (physical cluster split) stays in ARCH-001 Phase 10                        | Stage A only (schemas + roles + grants)                                                                                                                                                                                                      |
| Q3       | **Merge**: newer data updates existing data (light file first, detailed API later) | Normative merge rules §3.2 — supersedes D11 "replace"                                                                                                                                                                                        |
| Q4       | No delete — only add or merge                                                      | No delete endpoint, no tombstones; event types `CREATED` / `UPDATED` (`DELETED` reserved, unused)                                                                                                                                            |
| Q5       | Freshness depends on data size / hardware → configurable, default 60 s             | `analytics.projection.refresh.*` properties §3.6, default max staleness 60 s                                                                                                                                                                 |
| Q6       | Header-trust hardening **in scope** (other services rely on it; traceability)      | New phase P3 and AC-11; the `overview.md` non-goal is removed                                                                                                                                                                                |
| Q7       | Keep the legacy refresh in parallel until parity is proven                         | Shadow mode with read switch (P6); legacy code removed in P8 only                                                                                                                                                                            |

---

## 3. Normative design specification

These rules are the reference for implementation and tests. Any deviation must be recorded in `analyse.md` first.

### 3.1 Database ownership (Stage A)

| Object                         | Target                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Catalog schema                 | `gene_data`: `protein_entry` and child tables, `keyword`, `go_term`, `host_organism`, join tables, standalone sequences (V11), FTS function (V3), `saved_filter`, `audit_log`, `catalog_outbox`, `protein_ingestion`, Flyway history                                                                                                                                                                                                                                            |
| Group role                     | `gene_data_rw` (NOLOGIN): `USAGE` on `gene_data`, DML on tables, `USAGE`/`SELECT` on sequences, `EXECUTE` on functions                                                                                                                                                                                                                                                                                                                                                          |
| Migration role                 | `gene_data_owner` (LOGIN, used only by Flyway via `spring.flyway.user`): owns every `gene_data` object                                                                                                                                                                                                                                                                                                                                                                          |
| Runtime login                  | `dashboard_svc` (member of `gene_data_rw`). The future `gene-service` gets a new login in the same group, so the rename needs no re-grant                                                                                                                                                                                                                                                                                                                                       |
| Other services                 | `analytics_svc` → `analytics`; `import_svc` → `import_batch`; `auth_svc` → `auth`; `export_svc` → `export_data`. No privilege on `gene_data`                                                                                                                                                                                                                                                                                                                                    |
| `public` schema                | `REVOKE ALL ON SCHEMA public FROM PUBLIC`; `REVOKE ALL ON DATABASE … FROM PUBLIC`; explicit per-role `CONNECT`                                                                                                                                                                                                                                                                                                                                                                  |
| Default privileges             | `ALTER DEFAULT PRIVILEGES FOR ROLE gene_data_owner IN SCHEMA gene_data GRANT … TO gene_data_rw`, so future tables are covered                                                                                                                                                                                                                                                                                                                                                   |
| Replica                        | Physical replication copies roles and grants; `dashboard_svc` is read-only there by nature                                                                                                                                                                                                                                                                                                                                                                                      |
| Hibernate                      | `hibernate.default_schema: gene_data` (same pattern as the other services)                                                                                                                                                                                                                                                                                                                                                                                                      |
| Flyway history relocation (F8) | Runbook step executed once, **before** the release that sets `spring.flyway.default-schema: gene_data`: `CREATE SCHEMA IF NOT EXISTS gene_data; ALTER TABLE public.flyway_schema_history SET SCHEMA gene_data;` (guarded by an existence check)                                                                                                                                                                                                                                 |
| Move migration                 | `V22__move_catalog_to_gene_data.sql` is **idempotent**: each `ALTER … SET SCHEMA` is wrapped in a `DO` block guarded by `to_regclass('public.<t>')` / `to_regprocedure(…)`, so fresh installs (where Flyway's `search_path` makes V1–V21 create objects directly in `gene_data`) and upgrades both succeed. Functions to move explicitly: `fts_match` (V3) and trigger function `trg_protein_entry_search_vector` (V1); standalone sequences from V11 and `audit_log_seq` (V13) |
| Fresh-install leftover         | V21 drops `public.materialized_view_refresh_log` with an explicit schema, so on a fresh install the V12 table would survive in `gene_data`. V22 also runs `DROP TABLE IF EXISTS gene_data.materialized_view_refresh_log`                                                                                                                                                                                                                                                        |

### 3.2 Merge semantics (Q3, Q4) — supersedes D11

| Element                         | Rule                                                                                                                                                                                                                                                                               |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Identity                        | `accession` (natural key); a new accession ⇒ `CREATED`                                                                                                                                                                                                                             |
| Scalar field present (non-null) | Overwrites the stored value (last writer wins — "new data updates current")                                                                                                                                                                                                        |
| Scalar field absent / `null`    | Stored value is kept (a light file never erases detailed data)                                                                                                                                                                                                                     |
| Staleness guard                 | If the incoming UniProt `entry_version` is present **and lower** than the stored one, the entry is a no-op counted as `stale`. Equal version ⇒ merge (enrichment of the same UniProt release)                                                                                      |
| Child collections               | **Additive**: each incoming child is matched by natural key; match ⇒ scalar merge rules; no match ⇒ insert. Absent or empty collection ⇒ no change. Nothing is ever deleted                                                                                                        |
| Natural keys                    | keyword: `keyword_id` (else normalized name) · GO term: `go_id` · cross reference: (`database`, `ref_id`) · host organism: `taxon_id` · feature: (`type`, `begin`, `end`, `description`) · comment: (`type`, text hash) · publication: PubMed ID, else DOI, else citation hash     |
| Material change detection       | Merge compares before/after. Change ⇒ `revision = revision + 1`, `updated_at`, `last_ingestion_id`, one outbox event. No change ⇒ no write, no event                                                                                                                               |
| Idempotency                     | By construction: re-applying the same payload is a no-op. No request-level dedup table is needed. Chunk retries and job restarts are safe (AC-4)                                                                                                                                   |
| Concurrency                     | One transaction per chunk; existing rows loaded with `SELECT … FOR UPDATE` **ordered by accession** (deadlock-free); children fetched in batch (no N+1); new entries use the PERF-001 sequence / batch-insert path                                                                 |
| Delete                          | Not supported (Q4). No endpoint, no tombstone                                                                                                                                                                                                                                      |
| Traceability                    | `protein_entry`: `revision`, `created_at`, `updated_at`, `last_ingestion_id`. `protein_ingestion`: id, source service, source type (`FILE_DAT`, `FILE_TSV`, `UNIPROT_API`), initiating user, status, counters (`created`, `updated`, `unchanged`, `stale`, `rejected`), timestamps |

### 3.3 Internal API v1 (`dashboard`, never routed by the gateway)

| Method & path                                                   | Scope            | Purpose                                                                                                             |
|-----------------------------------------------------------------|------------------|---------------------------------------------------------------------------------------------------------------------|
| `POST /internal/v1/protein-ingestions/{ingestionId}/entries`    | `catalog:write`  | Merge one chunk; the first call registers the ingestion. Returns chunk counters and rejected items                  |
| `POST /internal/v1/protein-ingestions/{ingestionId}/completion` | `catalog:write`  | Idempotent; marks the ingestion completed, writes the completion outbox event, evicts dashboard caches after commit |
| `GET /internal/v1/protein-entries/{accession}`                  | `catalog:read`   | Single entry read (AC-8 probe; export-service later)                                                                |
| `POST /internal/v1/protein-entries/search`                      | `catalog:read`   | Paged search on the **local** catalog only (F13); used by the AC-7 parity check                                     |
| `GET /internal/v1/saved-filters/{id}`                           | `catalog:read`   | Replaces the forged-header call (F7); ownership checked against the token `act.sub`                                 |
| `POST /internal/v1/protein-entries/republish`                   | `catalog:replay` | Operator-only backfill: pages the catalog and writes snapshot events through the outbox (F11)                       |

Rules: request/response records live in `dashboard-client`; controllers do not implement the Feign interfaces;
errors use the platform error envelope; a request-body limit is configured for `/internal/**`; if Feign request
compression is enabled, dashboard must register a request-decompression filter (the servlet container does not
decompress request bodies).

### 3.4 Events

| Topic                                    | Key           | Policy            | Partitions | Payload                                                                                            |
|------------------------------------------|---------------|-------------------|-----------:|----------------------------------------------------------------------------------------------------|
| `catalog.protein-entry.changed.v1`       | `accession`   | `compact`         |          6 | Post-merge **state snapshot** of analytics-relevant fields (no raw sequence; length and mass only) |
| `catalog.protein-ingestion.completed.v1` | `ingestionId` | `delete` (7 days) |          3 | Ingestion id, source type, counters, completion time                                               |

Envelope: `eventId` (UUID), `eventType` (`CREATED` | `UPDATED`), `schemaVersion`, `occurredAt`,
`aggregateVersion` (= `revision`, F2), `correlationId`, `actor`, `payload`. JSON without type headers.
Consumers apply a version-guarded upsert (`incoming.revision > stored.revision`, or row absent). Because events
are snapshots, duplicates and out-of-order delivery are harmless.

### 3.5 Trust model (Q6, D9) — new AC-11

| Layer                | Rule                                                                                                                                                                                                                                                      |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Gateway ingress      | Always strip inbound `X-User-*` (and any other internal header) **before** routing, on every route including public ones                                                                                                                                  |
| Public endpoints     | Exact `method + path` matching (no `contains`), declared in configuration                                                                                                                                                                                 |
| Routing              | `discovery.locator.enabled: false`; only explicit `/api/**` routes; explicit deny for `/internal/**` and `/{service-id}/**` ⇒ 404                                                                                                                         |
| Identity to services | The gateway relays the validated `Authorization` bearer token. Services authenticate with the resource server (`CommonSecurityConfig` / `JwtDecoderConfig`) and build `UserPrincipal` from claims. `X-User-*` become informational only, then are removed |
| Transition switch    | `app.security.header-auth.enabled` (`true` during the P3 rollout, `false` at P3 exit); removed in P8                                                                                                                                                      |
| Service-to-service   | Service JWT: `typ=service`, `sub=<service-id>`, `aud=<target-service>`, `scope`, short TTL (`app.jwt.service-token-expiry-seconds`, today 300 s). Optional `act.sub` = originating user when the call is made on behalf of a user (RFC 8693 actor claim)  |
| Issuance             | Preferred: auth-service client credentials (it already issues `typ=service` tokens). Interim: HMAC minting in `common-starter` (risk R8)                                                                                                                  |
| Traceability         | `X-Correlation-Id` generated at the gateway if absent and propagated (HTTP, Feign, Kafka headers, outbox rows); MDC keys `correlationId`, `userId`, `actor`, `serviceId`; `audit_log` gets `correlation_id` and `actor_service`                           |

### 3.6 Configuration properties

```yaml
# analytics-service
analytics:
  read-model:
    source: legacy                  # legacy | projection (P6 switch)
  legacy-refresh:
    enabled: true                   # Q7 shadow mode; false after parity sign-off
  projection:
    refresh:
      quiet-period: 15s             # wait until no new catalog event arrives
      max-staleness: 60s            # Q5 default; refresh starts at the latest at marker + max-staleness
      timeout: 10m                  # guard for a single refresh

# dashboard
catalog:
  outbox:
    relay:
      enabled: true
      batch-size: 500
      poll-interval: 200ms
      max-attempts: 10
    retention: 7d                   # cleanup of published rows
  internal-api:
    max-chunk-size: 1000

# import-service
import:
  writer:
    mode: legacy-jpa                # legacy-jpa | catalog-api (P5 switch)
  catalog-client:
    service-id: dashboard           # becomes gene-service after the rename (Q1)
    retry:
      max-attempts: 3
      initial-backoff: 1s
      multiplier: 2
```

Every group is bound to a `@ConfigurationProperties` record with `@Validated` constraints (for example
`quiet-period < max-staleness`, positive durations, `max-attempts ≥ 1`).

---

## 4. Roadmap

```text
P0  Baseline & documentation alignment
P1  Platform groundwork (common-starter opt-in, Kafka without type headers, correlation id)
P2  dashboard-client v1 contract module
P3  Trust-model hardening & service-to-service security         (Q6, D9)
P4  Catalog ownership in dashboard: merge, outbox, internal API  (D2, D4, D5, Q3)
P5  Import cutover to the internal API                           (D2)
P6  Analytics projection, shadow mode & parity                   (D3, Q5, Q7)
P7  Database isolation: gene_data schema, roles, grants          (D6, D7, Q1, Q2)
P8  Legacy removal & architecture enforcement                    (D8)
P9  Qualification, rollout & handover
```

| Phase | Depends on  | Primary ACs       | Estimate (eng-days) | Rollback                                                                              |
|-------|-------------|-------------------|--------------------:|---------------------------------------------------------------------------------------|
| P0    | —           | —                 |                   2 | n/a                                                                                   |
| P1    | P0          | AC-5 (headers)    |                   3 | Redeploy the previous starter version                                                 |
| P2    | P1          | AC-9              |                   3 | Unused module, no runtime effect                                                      |
| P3    | P2          | AC-8, AC-11       |                   6 | `app.security.header-auth.enabled=true`; restore previous gateway routes              |
| P4    | P2, P3      | AC-5, AC-12       |                   8 | `catalog.outbox.relay.enabled=false`; endpoints unused by callers                     |
| P5    | P4          | AC-3, AC-4, AC-10 |                   5 | `import.writer.mode=legacy-jpa`                                                       |
| P6    | P4, P5      | AC-6, AC-7        |                   8 | `analytics.read-model.source=legacy`                                                  |
| P7    | P5, P6 exit | AC-2, AC-10       |                   5 | Re-grant script (kept until P9 exit); the schema move is metadata-only and reversible |
| P8    | P7          | AC-1              |                   3 | Git revert (no data impact)                                                           |
| P9    | P8          | AC-1 … AC-12      |                   4 | Per-phase switches above                                                              |
|       |             | **Total**         |            **≈ 47** | ≈ 7–9 weeks for 1–2 engineers                                                         |

> The estimate grows from the original 4–6 weeks because Q6 (header hardening) and Q3 (merge instead of
> replace) were added to the scope.

## Status

- [ ] P0 — Baseline & documentation alignment
- [ ] P1 — Platform groundwork
- [ ] P2 — `dashboard-client` v1
- [ ] P3 — Trust-model hardening & S2S security
- [ ] P4 — Catalog ownership in dashboard
- [ ] P5 — Import cutover
- [ ] P6 — Analytics projection & parity
- [ ] P7 — Database isolation
- [ ] P8 — Legacy removal & enforcement
- [ ] P9 — Qualification & rollout
- [ ] Coverage ≥ 80 % (JaCoCo) recorded in `journal.md`

---

## 5. Detailed checklist

### P0 — Baseline & documentation alignment

**Goal:** freeze the public surface, measure the baseline, and align every spec document with the Q1–Q7 answers.

- [ ] 0.1 `analyse.md`: record resolutions (Q1–Q7); amend D5 (`revision`), D7 (`gene_data`), D9 (+ trust model), D11
  (merge); add D12 (trust model) and D13 (configurable freshness).
- [ ] 0.2 `overview.md`: AC-2 on `gene_data`; remove the `X-User-*` non-goal; add AC-11 (trust model) and AC-12 (merge);
  update the decision table and the estimate.
- [ ] 0.3 `domain-model.md`: target `gene_data` schema, `revision`, `last_ingestion_id`, `protein_ingestion`,
  `catalog_outbox`, `analytics.protein_fact`, roles and grants (each section updated **before** the migration that
  implements it).
- [ ] 0.4 Create `documentation/event-catalog.md` (topics, envelope, payloads, ordering, duplicates, no-delete rule).
- [ ] 0.5 Baseline: PERF-001 import fixture → processed / skipped counts, duration, chunk size, row counts per table.
- [ ] 0.6 Snapshot public responses for `/api/genes/**`, `/api/analytics/**`, `/api/admin/import/**`,
  `/api/saved-filters/**` (golden files).
- [ ] 0.7 Build the AC-7 fixture set: ≥ 30 search combinations covering every filter field, full-text, empty result,
  pagination and sorting.
- [ ] 0.8 Record current KPI totals and MV refresh durations.
- [ ] 0.9 Merge fixtures: a light `.tsv` entry and a detailed `.dat` / API entry for the same accessions, plus a lower-
  `entry_version` file (staleness guard).

**Exit:** all documents consistent; baseline artifacts stored under `documentation/implementation/ARCH-002/baseline/`.

### P1 — Platform groundwork

**Goal:** remove accidental coupling from shared infrastructure without changing behaviour.

- [ ] 1.1 `common-starter`: replace `@ComponentScan("com.bioinformatics.common")` with explicit
  `AutoConfiguration.imports` entries, each guarded by `@ConditionalOnClass` / `@ConditionalOnProperty`.
- [ ] 1.2 Legacy catalog beans (`ProteinEntryService`, UniProt clients) are registered **only** where still needed,
  through an explicit temporary opt-in (`app.legacy-catalog.enabled`), not by scan.
- [ ] 1.3 Mark heavy dependencies `<optional>true</optional>`; remove `spring-cloud-starter-gateway-server-webmvc` and
  the unused `spring-boot-starter-oauth2-client`; each service declares its own web / data / Kafka starters.
- [ ] 1.4 Kafka (F9): bind the legacy `ViewRefreshRequestedEvent` listener to an explicit default value type, then set
  `ADD_TYPE_INFO_HEADERS=false`; remove `TRUSTED_PACKAGES=com.bioinformatics.*`.
- [ ] 1.5 Correlation id: servlet filter + Feign interceptor + Kafka producer/consumer interceptors propagating
  `X-Correlation-Id`; MDC keys documented.
- [ ] 1.6 Tests: auto-configuration context tests (each feature on/off), Kafka round-trip without `__TypeId__`,
  correlation propagation test.

**Exit:** all services start with unchanged public behaviour; golden files still match.

### P2 — `dashboard-client` v1

**Goal:** a thin, versioned, persistence-free contract.

- [ ] 2.1 Module `backend/libs/dashboard-client`, packages `com.bioinformatics.catalog.client.v1` (sync) and
  `com.bioinformatics.catalog.client.v1.event` (reference event schema).
- [ ] 2.2 Dependencies: `jakarta.validation-api`, `jackson-annotations`, `spring-cloud-openfeign-core` (`provided`).
  Enforcer rule: no Spring Boot starter, JPA, Hibernate or Spring Data.
- [ ] 2.3 Records: `ProteinEntryUpsert` (+ child records), `IngestionChunkResult`, `IngestionCompletion`,
  `ProteinEntrySummary`, `ProteinSearchCriteria` (F15), `PageResponse<T>`, `SavedFilter`, `CatalogEvent<T>`,
  `ProteinEntryChangedPayload`, `ProteinIngestionCompletedPayload`.
- [ ] 2.4 All records annotated `@JsonIgnoreProperties(ignoreUnknown = true)`; nullable fields documented as "absent =
  keep" (§3.2).
- [ ] 2.5 Feign interfaces `ProteinIngestionClient`, `ProteinEntryClient`, `SavedFilterClient` with
  `@FeignClient(name = "${catalog.client.service-id:dashboard}", contextId = …)`; only `@GetMapping` / `@PostMapping` /
  `@PathVariable` / `@RequestBody` / `@RequestParam` (D10).
- [ ] 2.6 `CatalogTopics` constants.
- [ ] 2.7 Tests: tolerant-reader test per record (unknown field), JSON snapshot tests guarding additive-only evolution.

**Exit:** the module builds standalone; dependency tree verified; unit part of AC-9 green.

### P3 — Trust-model hardening & service-to-service security

**Goal:** no service trusts caller-supplied identity headers; `/internal/**` is private and scoped.

- [ ] 3.1 Gateway: global pre-filter stripping inbound `X-User-*` and internal headers on **every** route (F6-b).
- [ ] 3.2 Gateway: replace `path::contains` with exact method + path matching for public endpoints (F6-a); regression
  test for `POST /api/genes/x/auth/login`.
- [ ] 3.3 Gateway: `discovery.locator.enabled: false`; explicit deny route for `/internal/**` and `/{service-id}/**` →
  404; relay `Authorization`.
- [ ] 3.4 Services: authenticate users from the relayed JWT (resource server); map claims to `UserPrincipal`;
  `GatewayUserAuthenticationFilter` active only while `app.security.header-auth.enabled=true`.
- [ ] 3.5 Service tokens: extend auth-service to issue client-credentials tokens (`sub`, `aud`, `scope`, optional
  `act`); interim HMAC minting in `common-starter` behind a property (R8). A Feign `RequestInterceptor` attaches the
  token and caches it until shortly before expiry.
- [ ] 3.6 Dashboard `/internal/**` security chain: `typ=service` required; scopes per §3.3; user token ⇒ 403; no token ⇒
  401.
- [ ] 3.7 Migrate import-service `SavedFilterClient` (F7) to `GET /internal/v1/saved-filters/{id}` with `act.sub` =
  initiating admin; dashboard checks ownership / visibility against `act.sub`.
- [ ] 3.8 Traceability: `audit_log.correlation_id`, `audit_log.actor_service` (Flyway + `domain-model.md`); structured
  logs carry `correlationId`, `userId`, `actor`, `serviceId`.
- [ ] 3.9 Set `app.security.header-auth.enabled=false` in all services once every caller is migrated.
- [ ] 3.10 Tests: spoofed `X-User-Id` through the gateway is ignored; direct call with forged headers and no JWT ⇒ 401;
  AC-8 matrix (404 / 401 / 403 / 200); expired and wrong-audience service tokens rejected; frontend Playwright smoke
  unchanged.

**Exit:** AC-8 and AC-11 green; frontend unchanged.

### P4 — Catalog ownership in dashboard

**Goal:** dashboard owns the aggregate, its invariants and its event publication.

- [ ] 4.1 Copy catalog entities, repositories, specifications and services into dashboard feature packages
  (`…dashboard.catalog.{entity,repository,specification,service,web}`); restrict `@EntityScan` /
  `@EnableJpaRepositories` to them. Mark `common.gene.*` `@Deprecated(forRemoval = true)` (removed in P8, F3).
- [ ] 4.2 Move the UniProt remote-provider code (`common.providers.uniprotkb.*`, `common.uniprot.*`) to
  `dashboard.providers.uniprotkb`.
- [ ] 4.3 Flyway (still in `public`, qualified names): `revision BIGINT NOT NULL DEFAULT 0`, `created_at`, `updated_at`,
  `last_ingestion_id` on `protein_entry`; `protein_ingestion`; `catalog_outbox` (id, aggregate_type, aggregate_id,
  aggregate_version, event_type, topic, message_key, payload `JSONB`, correlation_id, occurred_at, published_at,
  attempts, last_error) with a partial index on unpublished rows.
- [ ] 4.4 `ProteinMergeService` implementing §3.2, backed by a pure `ProteinMergePolicy` (no I/O) that is fully
  unit-testable.
- [ ] 4.5 `ProteinIngestionService`: chunk transaction = lock ordered by accession → merge → batch write → outbox rows
  (JDBC batch) → counters.
- [ ] 4.6 Thin internal controllers per §3.3; Bean Validation per `validation-rules.md`; max chunk size; errors in the
  platform envelope.
- [ ] 4.7 Outbox relay: `FOR UPDATE SKIP LOCKED` batches ordered by `id`; publish; set `published_at` after broker
  acknowledgement; bounded retries with backoff; poison rows flagged; cleanup job driven by `catalog.outbox.retention`.
  `SKIP LOCKED` makes multiple instances safe without an extra distributed lock.
- [ ] 4.8 Topic provisioning (`NewTopic` beans or init script) per §3.4.
- [ ] 4.9 Completion endpoint: idempotent; evicts **dashboard's own** cache names (CACHE-001) after commit (C10).
- [ ] 4.10 Republish operation (F11): paged snapshot events through the outbox, throttled, resumable by accession
  cursor.
- [ ] 4.11 Metrics: outbox backlog size, oldest unpublished age, publish rate / failures, merge counters.
- [ ] 4.12 Tests: merge policy matrix (enrichment, absent fields, empty collections, staleness guard, conflicting
  values, child natural keys); idempotent re-apply; rollback ⇒ no outbox row; commit ⇒ one event, key = accession, no
  `__TypeId__`; concurrent chunks on overlapping accessions (no deadlock); relay crash / restart; provider contract
  tests for every internal endpoint; search always local (F13).

**Exit:** AC-5 and AC-12 green on dashboard in isolation; public golden files unchanged.

### P5 — Import cutover

**Goal:** import-service parses and orchestrates; dashboard persists.

- [ ] 5.1 Anti-corruption layer: processors build `ParsedProtein`; MapStruct `ParsedProteinMapper` →
  `ProteinEntryUpsert` (absent values stay `null`).
- [ ] 5.2 `CatalogApiItemWriter` using `ProteinIngestionClient`; `ingestionId` = import job id; `sourceType` from the
  file type.
- [ ] 5.3 **One** retry layer only: Spring Batch fault-tolerant step (`retryLimit` from
  `import.catalog-client.retry.max-attempts`, exponential backoff) on 503 / connection errors; Feign
  `Retryer.NEVER_RETRY`; no retry on 4xx.
- [ ] 5.4 Retry exhaustion ⇒ step FAILED with an explicit message (`Catalog service unavailable after N attempts`), job
  FAILED; restart resumes from the last committed chunk (idempotent merge, §3.2).
- [ ] 5.5 Rewrite `ImportUniprotSkipListener` / `ImportProgressChunkListener` on `ParsedProtein`; progress includes
  `created / updated / unchanged / stale`.
- [ ] 5.6 End of job: `POST …/completion`; remove `PostImportCacheEvictionListener` (C10).
- [ ] 5.7 Feign timeouts sized for the largest chunk; compression only when the §3.3 decompression filter is in place
  and measured.
- [ ] 5.8 `docker-compose.yml`: remove `import-service.depends_on.backend` (C9); import-service no longer validates
  catalog tables with JPA.
- [ ] 5.9 Switch `import.writer.mode=catalog-api`.
- [ ] 5.10 Tests: WireMock consumer tests (200, 400, 401, 403, 503, timeout, unknown fields); outage test (3 attempts →
  FAILED) and restart (no duplicate, counts = baseline); light-then-detailed import enriches entries (AC-12); throughput
  vs PERF-001 (≤ +30 %).

**Exit:** AC-3, AC-4 and AC-12 green end-to-end; startup part of AC-10 green for import-service.

### P6 — Analytics projection, shadow mode & parity

**Goal:** analytics reads only data it owns, proven equal to the legacy path.

- [ ] 6.1 Flyway (`analytics` schema): `protein_fact` (scalars + `TEXT[]` columns + generated `search_vector` using the
  **same** text-search configuration and source fields as the dashboard FTS function), GIN indexes, `revision`,
  `last_event_id`, `updated_at`.
- [ ] 6.2 Projection MVs (`*_proj`) on `protein_fact`, next to the legacy MVs (F10).
- [ ] 6.3 Consumer on `catalog.protein-entry.changed.v1`: local tolerant record; single-statement upsert
  `ON CONFLICT (accession) DO UPDATE … WHERE protein_fact.revision < EXCLUDED.revision`; dead-letter topic for poison
  messages.
- [ ] 6.4 Refresh coordinator on `catalog.protein-ingestion.completed.v1`: quiet-period debounce with a `max-staleness`
  ceiling (§3.6); single-flight (no concurrent refresh); `REFRESH MATERIALIZED VIEW CONCURRENTLY`; evicts analytics
  caches after refresh.
- [ ] 6.5 The legacy refresh keeps running (`analytics.legacy-refresh.enabled=true`, Q7).
- [ ] 6.6 `ProteinFactSpecification` translating the analytics public request into projection predicates; the public
  request record keeps the exact JSON shape (F15).
- [ ] 6.7 Read switch `analytics.read-model.source` for KPI, compare and filtered endpoints.
- [ ] 6.8 Backfill (only after the P5 writer switch, see 9.5): trigger the dashboard republish (4.10) into an empty
  projection; reconciliation report (row counts, per-accession revision).
- [ ] 6.9 Parity job: legacy vs projection for KPIs, MVs and the ≥ 30 fixtures (AC-7 compares with
  `/internal/v1/protein-entries/search` `totalElements`).
- [ ] 6.10 Tests: duplicate and out-of-order events; completion marker before the last entry event; burst of markers ⇒
  one refresh; configured values change the timing without code change; freshness measured on a small and on the
  PERF-001 dataset.
- [ ] 6.11 Soak in shadow mode, then set `read-model.source=projection` and `legacy-refresh.enabled=false`. Decision
  gate: if parity fails and cannot be fixed, fall back to D3 option B (documented in `analyse.md`).

**Exit:** AC-6 and AC-7 green; parity report signed off; legacy refresh disabled but its code still present.

### P7 — Database isolation (Stage A)

**Goal:** only `dashboard_svc` (through `gene_data_rw`) can touch catalog data.

- [ ] 7.1 Provisioning script (idempotent, versioned under `devops/scripts/db/`): roles of §3.1, per-service passwords
  from secrets, `REVOKE … FROM PUBLIC`, default privileges.
- [ ] 7.2 Per-service datasource credentials in `.env` / Config Server; remove the shared `COMMON_DATASOURCE_*`
  variables (C8).
- [ ] 7.3 Fix the analytics local fallback `spring.flyway.default-schema: analytics` (F14).
- [ ] 7.4 Flyway history relocation runbook (F8), rehearsed on a restored production-like dump.
- [ ] 7.5 `V22__move_catalog_to_gene_data.sql` (idempotent, §3.1): tables, owned sequences, standalone V11 sequences and
  `audit_log_seq`, functions `fts_match` and `trg_protein_entry_search_vector`, `saved_filter`, `audit_log`, outbox,
  ingestion table; drop the fresh-install leftover `materialized_view_refresh_log`; `ALTER … OWNER TO gene_data_owner`.
- [ ] 7.5b Migration test matrix: (a) fresh install V1→V22 with `default-schema: gene_data`; (b) upgrade from a V21
  database in `public` after the history relocation runbook; both produce an identical `pg_dump --schema-only` of
  `gene_data`.
- [ ] 7.6 Dashboard configuration: `hibernate.default_schema: gene_data`, `spring.flyway.default-schema: gene_data`,
  `spring.flyway.user: gene_data_owner`; native queries schema-qualified.
- [ ] 7.7 Revoke the transitional analytics read grants on catalog tables (legacy MVs are no longer refreshed after P6).
- [ ] 7.8 Tests (Testcontainers, plus the replica where available): `analytics_svc` / `import_svc` / `auth_svc` /
  `export_svc` ⇒ `permission denied for schema gene_data` on `SELECT` / `INSERT` / `UPDATE` / `DELETE`; `dashboard_svc`
  has full DML; replica read checks; fresh-install and upgrade paths reach the same schema.
- [ ] 7.9 Each service starts on its own without dashboard (AC-10).

**Exit:** AC-2 and AC-10 green on primary and replica.

### P8 — Legacy removal & architecture enforcement

- [ ] 8.1 Delete `common.gene.*`, `common.providers.uniprotkb.*`, `common.uniprot.*`, `common.models.gene`,
  `common.models.filter` and the catalog exceptions from `common-starter`; move import / auth exceptions to their
  owners.
- [ ] 8.2 Delete `DbSchema`, `ViewRefreshRequestedEvent`, `KafkaTopics.ANALYTICS_VIEW_REFRESH_REQUESTED`, the legacy
  producers / consumers, the legacy MVs (analytics migration), `app.legacy-catalog.enabled`,
  `app.security.header-auth.enabled` and `GatewayUserAuthenticationFilter`.
- [ ] 8.3 Remove JPA / data dependencies on catalog code from import-service and analytics-service.
- [ ] 8.4 ArchUnit (shared rule set run in every module): no `@Entity` / Spring Data repository outside the owning
  service; `ProteinEntry` only in dashboard; no `com.bioinformatics.common.gene`; no dependency from one service on
  another service's packages; `dashboard-client` declares no Spring bean.
- [ ] 8.5 Maven Enforcer: banned dependencies per module; allow-list for `dashboard-client`.
- [ ] 8.6 Build check rejecting SQL that references `gene_data.` outside dashboard migrations.
- [ ] 8.7 Negative fixtures prove that each rule fails the build.

**Exit:** AC-1 green; `./mvnw verify` fails on each deliberate violation.

### P9 — Qualification, rollout & handover

- [ ] 9.1 Full `./mvnw verify`; JaCoCo ≥ 80 % on new / changed code (merge policy, ingestion, relay, writer, consumer,
  refresh coordinator, gateway filters); results recorded in `journal.md`; anything below 80 % is a blocker.
- [ ] 9.2 Contract tests (provider + WireMock consumers) in CI (AC-9).
- [ ] 9.3 Golden-file comparison of the public APIs; Playwright smoke suite (`frontend/e2e`) unchanged (AC-10).
- [ ] 9.4 Performance: import duration ≤ baseline + 30 %; relay p95 ≤ 5 s in steady state; backlog drain time and
  projection freshness recorded for the PERF-001 dataset (F12).
- [ ] 9.5 Rollout order: P1 → P2 → P3 (header auth off after soak) → P4 (relay on) → P5 writer switch → P6 backfill &
  shadow → parity sign-off → P7 → P8. The writer switch must precede the backfill: while
  `import.writer.mode=legacy-jpa`, imports bypass dashboard and emit no outbox event, so the projection would miss them.
  The republish runs **after** the switch; concurrent imports are safe thanks to the revision guard.
- [ ] 9.6 Alerts: outbox oldest-unpublished age, relay failures, consumer lag, projection staleness > `max-staleness`,
  import retry exhaustion, 401/403 rate on `/internal/**`.
- [ ] 9.7 Runbooks: republish / backfill, outbox poison row, Flyway history relocation, role / credential rotation,
  service-token secret rotation.
- [ ] 9.8 Final `journal.md` entry with evidence per AC and deferred items (Stage B → ARCH-001 Phase 10; Debezium;
  service rename).

**Exit:** all ACs green; operational sign-off recorded.

---

## 6. Acceptance-criteria traceability

| AC    | Subject                                       | Phases     | Evidence                                                                |
|-------|-----------------------------------------------|------------|-------------------------------------------------------------------------|
| AC-1  | Module isolation                              | P1, P4, P8 | ArchUnit + Enforcer reports, negative fixtures                          |
| AC-2  | Database ownership (`gene_data`)              | P7         | Permission-denied tests on primary and replica                          |
| AC-3  | Import through the owner's API                | P4, P5     | E2E import, counts = baseline                                           |
| AC-4  | Import write-path resilience                  | P5         | Outage + restart test report                                            |
| AC-5  | Reliable catalog events                       | P1, P4, P9 | Rollback / commit tests, no `__TypeId__`, steady-state p95              |
| AC-6  | Analytics read model (configurable freshness) | P6         | Duplicate / out-of-order tests, refresh timing, KPI parity              |
| AC-7  | Filtered analytics parity                     | P0, P6     | ≥ 30 fixture report                                                     |
| AC-8  | Internal API not public                       | P3         | 404 / 401 / 403 matrix                                                  |
| AC-9  | Contract safety                               | P2, P4, P5 | Tolerant-reader, provider and WireMock consumer tests                   |
| AC-10 | No regression, independent startup            | P5, P7, P9 | Golden files, standalone startup, Playwright                            |
| AC-11 | Trust model hardened (new, Q6)                | P3         | Spoofing tests, S2S actor propagation, correlation id in logs and audit |
| AC-12 | Merge semantics (new, Q3/Q4)                  | P4, P5     | Enrichment, no-erase, staleness guard, idempotent re-apply, no delete   |

---

## 7. Risk register

| ID  | Risk                                                                                         | Mitigation                                                                                                       |
|-----|----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| R1  | Last-writer-wins lets a lighter but newer re-import overwrite values present in both sources | Only present fields overwrite; UniProt `entry_version` downgrade guard; counters expose `updated` vs `unchanged` |
| R2  | Merge requires reading existing aggregates ⇒ import slowdown                                 | Batch fetch by accession set, new-entry fast path, chunk tuning; gate ≤ +30 %                                    |
| R3  | Deadlocks between concurrent ingestions on overlapping accessions                            | Rows locked in accession order; retry on serialization failure                                                   |
| R4  | Outbox growth (≈ 570 k rows per full import)                                                 | Retention cleanup, partial index, backlog alerts                                                                 |
| R5  | Completion marker overtakes entry events                                                     | Quiet-period debounce with max-staleness ceiling; the marker is only a hint                                      |
| R6  | FTS semantics differ between dashboard and projection ⇒ AC-7 failures                        | Same text-search configuration and source fields; full-text fixtures; fallback D3-B                              |
| R7  | Flyway history mishandled during the schema move ⇒ V1 re-run                                 | Runbook relocation before the config switch; idempotent V22; rehearsal on a dump                                 |
| R8  | Interim HMAC service tokens share the user-token key                                         | Short TTL, `aud` + `typ` checks, scopes; move to auth-service client credentials                                 |
| R9  | Disabling header auth breaks an undiscovered caller                                          | Switch per service, 401 metrics during soak, search for `X-User-*` producers                                     |
| R10 | Kafka type-header removal breaks the legacy consumer                                         | Default-type binding first (P1), round-trip test                                                                 |
| R11 | Revoking grants breaks a hidden reader                                                       | Denial tests in CI; re-grant script kept until P9 exit                                                           |
| R12 | Stacked retry layers multiply attempts                                                       | Single retry layer (Spring Batch); Feign retryer disabled                                                        |

---

## 8. Definition of done

- [ ] P0–P9 checked in this plan; `journal.md` has a dated entry per phase.
- [ ] `analyse.md`, `overview.md`, `domain-model.md` and `event-catalog.md` match the implementation.
- [ ] AC-1 … AC-12 have automated, repeatable evidence.
- [ ] `api-contract.md` unchanged; frontend unchanged; Playwright smoke green.
- [ ] Dashboard is the only catalog persistence owner; `gene_data` is private to `gene_data_rw`.
- [ ] No service authenticates users from `X-User-*` headers.
- [ ] JaCoCo ≥ 80 % on new / changed backend code.
- [ ] Stage B remains deferred to ARCH-001 Phase 10.

