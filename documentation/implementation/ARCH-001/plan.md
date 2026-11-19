# ARCH-001 — Implementation Plan

## Architectural Overview

The migration follows the **Strangler Fig pattern** with 10 incremental phases. Each phase extracts one bounded context
into an independent Spring Boot service, deploys it alongside the monolith, reroutes traffic via the API Gateway, and
validates before proceeding. The monolith is gradually "drained" until it becomes a hollow reverse proxy, then
decommissioned.

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                         PHASE ROADMAP                                        │
├───────┬─────────────────────────────┬────────────────────────────────────────┤
│ Phase │ Scope                       │ Services Added                         │
├───────┼─────────────────────────────┼────────────────────────────────────────┤
│   0   │ Infrastructure Bootstrap    │ Gateway, Eureka, Config, Kafka, PG     │
│   1   │ Extract Auth Service        │ auth-service                           │
│   2   │ Extract Analytics Service   │ analytics-service (read replica)       │
│   3   │ Extract Import Service      │ import-service                         │
│   4   │ Extract Gene Service        │ gene-service (core domain)             │
│   5   │ Extract Export Service      │ export-service (PIPE-001)              │
│   6   │ Extract Structure Service   │ structure-service (STRUCT-001)         │
│   7   │ Extract NLQ Service         │ nlq-service (NLQ-001)                  │
│   8   │ Cross-Cutting Services      │ notification-service, audit-service    │
│   9   │ Frontend + Monolith Drain   │ Angular multi-url, monolith 410        │
│  10   │ Database-per-Service        │ CDC, schema migration, physical split  │
└───────┴─────────────────────────────┴────────────────────────────────────────┘
```

**Cross-ticket ownership (reconciled 2026-11-10):** several ARCH-001 phases were
delivered or taken over by other tickets. This plan keeps the original checklist
and annotates each item with its current owner.

| Phase | Delivered / owned by | Current state |
|-------|----------------------|---------------|
| 4 | ARCH-002 P1 (item 1.7) | Monolith `dashboard` moved/renamed to `backend/services/gene-service`; catalog ownership, `gene_data` schema, contracts and S2S remain ARCH-002 P2–P4/P7 |
| 5 | PIPE-001 | `export-service` (schema `export_data`, `/api/v1/exports`) with Angular wizard/list; lifecycle audit and completion event still open |
| 5 (readers) | REFACTOR-001 / REMOTE-001 | Postgres and UniProtKB provider dispatch/readers |
| 8 (audit) | ARCH-001 + ARCH-002 P1 | audit-service, header-free Kafka, correlation propagation, opt-in auto-configuration |
| 9 (deployment) | OPS-002 | Kubernetes manifests; 18 Deployments verified on Minikube (2026-10-26) |
| 9 (drainage) | ARCH-002 P8 | Superseded: no 410 monolith; legacy catalog removal + ArchUnit in ARCH-002 P8 |
| 10 Stage A | ARCH-002 P4/P7 | Outbox and `gene_data` schema/roles/grants |
| 10 Stage B | ARCH-001 (this ticket) | Physical split/Debezium explicitly deferred here by ARCH-002 Q2 |
| 6 / 7 | STRUCT-001 / NLQ-001 | Not started |

---

## Tasks

1. Analyze requirements and update plan
2. Resolve ambiguities (migration strategy, DB split, communication, auth, transactions)
3. Implement Phase 0 — Infrastructure bootstrap (Gateway, Eureka, Config, Kafka, DB replicas)
4. Implement Phase 1 — Extract auth-service
5. Implement Phase 2 — Extract analytics-service with read replica
6. Implement Phase 3 — Extract import-service
7. Implement Phase 4 — Extract gene-service (core domain)
8. Implement Phase 5 — Extract export-service (PIPE-001)
9. Implement Phase 6 — Extract structure-service (STRUCT-001)
10. Implement Phase 7 — Extract nlq-service (NLQ-001)
11. Implement Phase 8 — Extract notification-service and audit-service
12. Implement Phase 9 — Frontend adaptation and monolith drainage
13. Implement Phase 10 — Database-per-service migration (CDC, Debezium)
14. Write integration tests for all service boundaries
15. Write end-to-end tests (Gateway → Service → DB)
16. Write load tests for read replica routing
17. Update documentation and journal

## Status

- [x] Requirements analyzed
- [x] Ambiguities resolved (see analyse.md)
- [x] Phase 0 — Infrastructure bootstrap
- [x] Phase 1 — auth-service extracted
- [x] Phase 2 — analytics-service extracted (2.4 implemented as `ViewRefreshRequestedEvent` on `protein.events.imported`; to be replaced by ARCH-002 P6)
- [x] Phase 3 — import-service extracted
- [ ] Phase 4 — gene-service extracted (service exists via ARCH-002 rename of the monolith; catalog still in common-starter legacy catalog on `public` — ARCH-002 P2–P4/P7/P8)
- [x] Phase 5 — export-service extracted (delivered by PIPE-001; completion event, lifecycle audit and replica routing open)
- [ ] Phase 6 — structure-service extracted (STRUCT-001 not started)
- [ ] Phase 7 — nlq-service extracted (NLQ-001 not started)
- [ ] Phase 8 — notification + audit services extracted (audit producer/consumer foundation implemented; qualification and notification work remain)
- [ ] Phase 9 — Frontend adapted, monolith drained (single gateway base URL done; drainage superseded by ARCH-002 P8; K8s via OPS-002)
- [ ] Phase 10 — Database-per-service (Stage A in ARCH-002 P4/P7; Stage B open here)
- [ ] Integration tests written
- [ ] End-to-end tests written
- [ ] Load tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

**Current update — 2026-10-30:** Audit publishing has moved from dashboard into
`common-starter`; dashboard no longer persists audit records. Audit-service has
its own schema migration, consumer, persistence/search implementation, and three
Config Server files. Targeted producer and embedded-Kafka consumer checks passed
during implementation. This does not close Phase 8: retention, batch ingestion,
consumer failure handling/metrics, API qualification, clean-store rollout verification
and exclusive database credentials remain outstanding. See the dated journal entry.

**Rate-limiting update — 2026-10-30:** `@RateLimited`, its Bucket4j aspect and the
independent `app.rate-limiter` binding now live in `common-starter`. Dashboard,
auth, analytics, import, export and audit controllers use explicit cost-sensitive
policies; polling, autocomplete and chart fan-out have separate budgets from job
starts and credential checks. Shared defaults and service policy lists live in
`backend-config`; see its README for the complete policy/value matrix. These are
per-policy/per-IP, per-instance limits, not distributed quotas or an additional
global ceiling. Config Server publication, rollout and trusted-proxy validation
are still required; Gateway Redis rate limits are unchanged.

**Reconciliation update — 2026-11-10:** This plan was reconciled with the current
code and with PIPE-001, ARCH-002, OPS-002 and REMOTE-001 (see the ownership table
above and the dated journal entry). This was a documentation-only change; no
new tests were run for it. Main corrections:

- The `dashboard` module no longer exists. ARCH-002 P1 moved it to
  `backend/services/gene-service` (`com.bioinformatics.geneservice`), and
  dashboard-specific notes below are historical.
- Phase 2.4 was implemented with `ViewRefreshRequestedEvent`, not
  `ProteinImportedEvent`, which does not exist.
- Export-service uses the `export_data` schema and the primary datasource only.
  It has no replica routing, and nothing publishes `ExportCompletedEvent`.
- Gene-service APIs are at `/api/genes`, `/api/autocomplete` and
  `/api/saved-filters`, not `/api/v1/genes`.
- The Compose stack is the root `docker-compose.yml`; there is no
  `docker-compose.infra.yml`.
- Replica routing (`common.datasource.primary-url` / `replica-url`) is wired
  only for auth, analytics and audit.

**Fresh database decision — 2026-11-16 (all services):** Start from empty databases
and service-owned V1 migrations. Do not transfer/archive/purge legacy rows or retain,
relocate or repair old Flyway histories. Gene-service's V2–V21 chain is removed;
analytics/export V2 changes are folded into V1. Auth/import already have a single V1;
audit V1 includes the pooled ID sequence required by its entity.
The current shared PostgreSQL layout is retained: catalog in `public`, other services
in their own schemas; analytics starts after gene-service. Physical separation and
the target `gene_data` API/role isolation remain later phases. Existing databases
cannot use the rewritten scripts; provision fresh targets explicitly. No live database
or Docker volume is reset by this change. Older transfer/upgrade notes below are
historical and superseded by this decision.

---

## Detailed Checklist

---

### PHASE 0 — Infrastructure Bootstrap

**Goal:** Establish the foundational platform services that all microservices depend on.

#### 0.1 Shared Common Library (`libs/common-starter/`)

- [x] Create `common-starter` Spring Boot starter module:
  - [x] `CommonAutoConfiguration` — `@AutoConfiguration` with `@ConditionalOnProperty`
  - [x] `JwtDecoderConfig` — validates JWT using shared secret (from Config Server)
  - [x] `Resilience4jConfig` — default circuit breaker, retry, rate limiter beans
  - [x] Shared `@RateLimited`, `RateLimitAspect` and `RateLimitConfig` — servlet-only
    Bucket4j enforcement, independent `app.rate-limiter` properties, explicit
    configuration errors and the existing standard HTTP 429 response
  - [x] Apply shared policies to dashboard/auth/analytics/import/export/audit APIs;
    centralize enabled/global defaults and configure service-owned endpoint lists
    with environment-backed capacity/refill/period overrides in `backend-config`
  - [ ] Publish updated Config Server files and deploy/restart affected services;
    qualify proxy-derived client addresses and multi-replica rate-limit behavior
  - [x] `RoutingDataSourceConfig` — `AbstractRoutingDataSource` with PRIMARY/REPLICA keys
  - [x] `KafkaProducerConfig` — `KafkaTemplate` with JSON serializer for domain events
  - [x] `KafkaConsumerConfig` — `ConcurrentKafkaListenerContainerFactory` with JSON deserializer
  - [x] `TracingConfig` — Micrometer tracing with Brave propagation
  - [x] `WebClientConfig` — `WebClient.Builder` with load-balanced base URLs (Eureka)
  - [x] `CommonSecurityConfig` — common `SecurityFilterChain` pattern for service-level JWT validation
  - [x] `CommonGlobalExceptionHandler` — shared `@RestControllerAdvice` (reused from monolith, adapted)
- [x] Publish to local Maven repository (`./mvnw install`)
- [x] All services add dependency: `com.bioinformatics:common-starter:1.0.0`

#### 0.2 Service Discovery — Eureka Server (`infrastructure/discovery-server/`)

- [x] `DiscoveryServerApplication` — `@EnableEurekaServer`
- [x] `application.yml`:
    ```yaml
    server:
      port: 8761
    eureka:
      client:
        register-with-eureka: false
        fetch-registry: false
    ```
- [x] Health endpoint: `/actuator/health` returns UP
- [x] Docker service in `docker-compose.infra.yml`

#### 0.3 Config Server (`infrastructure/config-server/`)

- [x] `ConfigServerApplication` — `@EnableConfigServer`
- [x] `application.yml`:
    ```yaml
    server:
      port: 8888
    spring:
      cloud:
        config:
          server:
            git:
              uri: ${CONFIG_REPO_URI:https://github.com/bioinformatics/config-repo}
              clone-on-start: true
              default-label: main
    ```
- [x] Initialize `config-repo` Git repository with:
  - [x] `application.yml` — shared logging level, management endpoints, Kafka bootstrap
    - [x] `api-gateway.yml` — routing table, JWT secret, rate limits
    - [x] `auth-service.yml`, `gene-service.yml`, `analytics-service.yml`, etc.
  - [x] `*-dev.yml`, `*-prod.yml` environment overrides
- [x] Encrypt sensitive properties using Spring Cloud Config Encrypt (`{cipher}...`)
- [x] Docker service in `docker-compose.infra.yml`
  > **2026-11-10:** The config repo is the Gitea repository `bioinformatics/config-repo`
  > (seeded from `gitea/config-repo` / `backend-config/`). All infrastructure runs from
  > the root `docker-compose.yml`, alongside `gitea-db` and `gitea-server`.

#### 0.4 API Gateway (`infrastructure/api-gateway/`)

- [x] `GatewayApplication` — `@EnableDiscoveryClient`
- [x] `application.yml` — routing predicates per service:
    ```yaml
    spring:
      cloud:
        gateway:
          routes:
            - id: auth-service
              uri: lb://auth-service
              predicates:
                - Path=/api/v1/auth/**
            - id: gene-service
              uri: lb://gene-service
              predicates:
                - Path=/api/v1/genes/**
            - id: analytics-service
              uri: lb://analytics-service
              predicates:
                - Path=/api/v1/analytics/**
            - id: import-service
              uri: lb://import-service
              predicates:
                - Path=/api/v1/admin/import/**
            - id: export-service
              uri: lb://export-service
              predicates:
                - Path=/api/v1/exports/**
            - id: structure-service
              uri: lb://structure-service
              predicates:
                - Path=/api/v1/structures/**
            - id: nlq-service
              uri: lb://nlq-service
              predicates:
                - Path=/api/v1/nlq/**
            - id: notification-service
              uri: lb://notification-service
              predicates:
                - Path=/api/v1/notifications/**
            - id: audit-service
              uri: lb://audit-service
              predicates:
                - Path=/api/v1/audit/**
    ```
  > **Actual routes (2026-11-10, `backend-config/api-gateway.yml`):**
  >
  > - Legacy paths are rewritten to `/api/v1/...`:
  >   - `auth-service`: `/api/auth/**`
  >   - `analytics-service`: `/api/analytics/**`
  >   - `import-service`: `/api/admin/import/**`
  > - `audit-service` uses `/api/v1/admin/audit/**`, not `/api/v1/audit/**`.
  > - `export-service` uses `/api/v1/exports/**`.
  > - `gene-service` uses `/api/genes/**`, `/api/autocomplete/**` and
  >   `/api/saved-filters/**`, with no `/api/v1` prefix.
  > - Each route has a `forward:/fallback/<svc>` circuit-breaker fallback.
  > - No structure, nlq or notification routes are configured; those services
  >   do not exist (STRUCT-001 and NLQ-001 have not started).
- [x] `JwtGatewayFilter` — `GlobalFilter` that:
  - [x] Extracts `Authorization: Bearer <token>` header
  - [x] Validates JWT signature and expiry
  - [x] Adds `X-User-Id`, `X-User-Role`, `X-Data-Provider` headers to downstream requests
  - [x] Returns 401 if token missing/invalid (no downstream call)
- [x] `RateLimitGatewayFilter` — Redis-backed rate limiter (Bucket4j or Spring Cloud Gateway Redis RateLimiter):
  - [x] Default: 100 req/min per user per route
  - [x] `/api/v1/nlq/**`: 10 req/min (protects LLM cost)
  - [x] `/api/v1/exports/**`: 20 req/min
- [x] `CircuitBreakerGatewayFilter` — Resilience4j for each route:
  - [x] Fallback: 503 with `Retry-After` header
  - [x] Excludes `/api/v1/auth/**` from CB (auth must fail fast with 401/403)
- [x] `CorsGatewayConfig` — central CORS configuration (replaces monolith CORS)
- [x] Docker service in `docker-compose.infra.yml` exposing port 8080

#### 0.5 Message Broker — Kafka (`docker-compose.infra.yml`)

- [x] Kafka 3.7 + Zookeeper (or KRaft mode) containers
- [x] Topics auto-created on startup:
    - [x] `protein.events.imported` — 3 partitions, replication factor 1 (local dev)
    - [x] `export.events.completed` — 3 partitions
    - [x] `auth.events.authenticated` — 1 partition
    - [x] `audit.events.log` — 6 partitions (high throughput)
    - [x] `nlq.events.executed` — 1 partition
    - [x] `structure.events.viewed` — 1 partition
    - [x] `notification.events.trigger` — 3 partitions
- [x] Kafka UI (optional) for local dev monitoring
  > **2026-11-10:**
  >
  > - Topics are created by `kafka-init-topics` in the root `docker-compose.yml`
  >   and by the OPS-002 Kubernetes init job.
  > - Each topic's current producer:
  >   - `protein.events.imported`: import-service (`ViewRefreshRequestedEvent`).
  >   - `audit.events.log`: common-starter audit publisher.
  >   - `export.events.completed`, `nlq.events.executed`,
  >     `structure.events.viewed` and `notification.events.trigger`: none.
  > - Kafka payloads are header-free JSON with no `__TypeId__` (ARCH-002 P1).
  > - Topics are tracked in `documentation/event-catalog.md`.

#### 0.6 Database — PostgreSQL Primary + Replica (`docker-compose.infra.yml`)

- [x] `postgres-primary` container:
    - [x] `POSTGRES_DB=uniprot`
    - [x] `wal_level=replica`, `max_wal_senders=5`, `max_replication_slots=5`
    - [x] Init script creates replication user
- [x] `postgres-replica` container:
    - [x] `pg_basebackup` from primary on first start
    - [x] `hot_standby=on`
    - [x] Read-only connections allowed
- [x] Schema separation (Stage 1):
    ```sql
    CREATE SCHEMA IF NOT EXISTS auth;
    CREATE SCHEMA IF NOT EXISTS gene;
    CREATE SCHEMA IF NOT EXISTS analytics;
    CREATE SCHEMA IF NOT EXISTS import_batch;
    CREATE SCHEMA IF NOT EXISTS export_pipe;
    CREATE SCHEMA IF NOT EXISTS audit_log;
    ```
  > **Schemas actually in use (2026-11-10):**
  >
  > - `auth`, `analytics`, `import_batch` and `audit_log`.
  > - `export_data` (PIPE-001) instead of `export_pipe`.
  > - `public` holds the gene catalog, managed by gene-service Flyway V1–V21.
  > - No `gene` schema exists. The target schema is `gene_data`
  >   (ARCH-002 P4/P7).
- [x] `pgAdmin` container (optional) for local dev

#### 0.7 Docker Compose Infrastructure Stack

- [x] `docker-compose.infra.yml` services:
    - [x] `eureka-server` (port 8761)
    - [x] `config-server` (port 8888)
    - [x] `api-gateway` (port 8080)
    - [x] `kafka` (port 9092)
    - [x] `zookeeper` (port 2181)
    - [x] `postgres-primary` (port 5432)
    - [x] `postgres-replica` (port 5433)
    - [x] `redis` (port 6379) — for Gateway rate limiting + service caching
- [x] Health checks and dependency ordering (`depends_on` with condition)
- [x] Shared network `bioinformatics-network`
  > **2026-11-10:** There is no `docker-compose.infra.yml`. The root
  > `docker-compose.yml` runs:
  >
  > - Infrastructure: postgres, postgres-replica, redis, zipkin, zookeeper,
  >   kafka, kafka-init-topics, kafka-ui, discovery-server, gitea-db,
  >   gitea-server, config-server and api-gateway.
  > - Services: auth, audit, analytics, gene, import and export.
  > - The frontend.
  >
  > Kubernetes manifests are owned by OPS-002.

#### Phase 0 — Tests

- [ ] `DiscoveryServerIntegrationTest` — Eureka registers itself
- [ ] `ConfigServerIntegrationTest` — serves `application.yml` correctly
- [ ] `GatewayRoutingTest` — routes `/api/v1/genes/**` to mock downstream
- [ ] `GatewayJwtFilterTest` — validates token, rejects invalid, forwards claims
- [ ] `GatewayRateLimitTest` — exceeds limit → 429
- [x] `RateLimitConfigTest` / `RateLimitAspectTest` — shared servlet limiter binding,
  disabled/non-servlet activation, invalid configuration, exact capacity, refill,
  concurrency, policy/IP isolation, shared method budgets and standard HTTP 429
- [ ] `GatewayCircuitBreakerTest` — downstream down → 503 fallback
- [ ] `KafkaTopicCreationTest` — all topics exist with correct partitions
- [ ] `PostgresReplicationTest` — write to primary → readable on replica within 5s

---

### PHASE 1 — Extract Auth Service (`services/auth-service/`)

**Goal:** Isolate authentication and authorization into an independent service.

**Rationale:** Auth has the fewest downstream dependencies and the highest security value. Extracting it first validates
the infrastructure (Gateway, Eureka, Config) with minimal risk.

#### 1.1 Service Setup

- [x] `AuthServiceApplication` — `@SpringBootApplication`, `@EnableDiscoveryClient`
- [x] `bootstrap.yml` — Config Server location, Eureka registration, profile
- [x] Dependencies: `common-starter`, `spring-cloud-starter-netflix-eureka-client`, `spring-boot-starter-data-jpa`,
  `spring-boot-starter-security`, `jjwt`, `bcrypt`
- [x] Port: `8081` Could be `0` as the application use Eureka for registration

#### 1.2 Database & Schema

- [x] Flyway migration `V1__auth_schema.sql`:
    ```sql
    CREATE SCHEMA IF NOT EXISTS auth;
    CREATE TABLE auth.app_user (
        id BIGSERIAL PRIMARY KEY,
        username VARCHAR(50) NOT NULL UNIQUE,
        password VARCHAR(100) NOT NULL,
        role VARCHAR(20) NOT NULL,
        failed_attempts INT DEFAULT 0,
        locked_until TIMESTAMPTZ,
        created_at TIMESTAMPTZ DEFAULT NOW(),
        updated_at TIMESTAMPTZ DEFAULT NOW()
    );
    CREATE TABLE auth.refresh_token (
        id BIGSERIAL PRIMARY KEY,
        user_id BIGINT NOT NULL REFERENCES auth.app_user(id) ON DELETE CASCADE,
        token_hash VARCHAR(100) NOT NULL,
        expires_at TIMESTAMPTZ NOT NULL,
        revoked BOOLEAN DEFAULT FALSE,
        created_at TIMESTAMPTZ DEFAULT NOW()
    );
    ```
- [x] `AppUser` entity — `@Table(schema = "auth", name = "app_user")`
- [x] `RefreshToken` entity — `@Table(schema = "auth", name = "refresh_token")`
- [x] `AppUserRepository`, `RefreshTokenRepository`
- [x] Routing DataSource: auth-service uses PRIMARY only (writes)

#### 1.3 API Implementation

- [x] `AuthController`:
  - [x] `POST /api/v1/auth/login` → `TokenResponse`
  - [x] `POST /api/v1/auth/refresh` → `TokenResponse`
  - [x] `PUT /api/v1/auth/password` → `200` (change password)
  - [x] `POST /api/v1/auth/logout` → `204` (revoke refresh token)
  - [x] `POST /api/v1/auth/service-token` → internal JWT for service-to-service calls (ADMIN only)
- [x] `AuthService` — bcrypt verification, JWT signing, refresh token lifecycle
- [x] `JwtService` — access token (1h) + refresh token (24h) + service token (5m)
- [x] `UserDetailsService` implementation
- [x] `CommonSecurityConfig` — stateless session, JWT filter, role-based access

#### 1.4 Monolith Adaptation

- [x] Monolith `AuthController` deprecated:
  - [x] All endpoints return `307 Temporary Redirect` to Gateway `/api/v1/auth/**`
  - [x] Or `410 Gone` with `Location` header (configurable)
- [x] Monolith `CommonSecurityConfig` updated to validate JWT via `auth-service` (REST call) instead of local secret
  - [x] Fallback: if auth-service unreachable, use cached public key

#### 1.5 Gateway Integration

- [x] Gateway route `/api/v1/auth/**` → `lb://auth-service`
- [x] Gateway JWT filter skips token validation for `/api/v1/auth/login` and `/api/v1/auth/refresh`

#### Phase 1 — Tests

- [x] `AuthServiceTest` — unit (mock repo):
  - [x] `login_validCredentials_returnsTokens`
  - [x] `login_invalidCredentials_throws401`
  - [x] `refresh_validToken_returnsNewAccessToken`
  - [x] `changePassword_wrongCurrentPassword_throws401`
  - [x] `serviceToken_adminRequest_returnsShortLivedJwt`
- [x] `AuthControllerIntegrationTest` — WebMvc unit (login/refresh/logout/password/service-token + validation)
- [x] `AuthControllerIntegrationTest` — Testcontainers:
  - [x] Full login/refresh/password flow
  - [x] Service-token issuance
- [x] `GatewayAuthRoutingTest` — routes login through Gateway correctly

---

### PHASE 2 — Extract Analytics Service (`services/analytics-service/`)

**Goal:** Move all analytics endpoints and materialized views to a dedicated read-optimized service.

**Rationale:** Analytics is 100% read-only, making it the safest domain to extract. It validates the read replica
routing and serves as a performance benchmark.

#### 2.1 Service Setup

- [x] `AnalyticsServiceApplication` — port `8082`
- [x] Dependencies: `common-starter`, `spring-data-jpa`, no Spring Batch, no security (relies on Gateway)
- [x] `bootstrap.yml` — Config Server, Eureka

#### 2.2 Database & Schema

- [x] Flyway `V1__analytics_schema.sql`:
    ```sql
    CREATE SCHEMA IF NOT EXISTS analytics;
    -- Materialized views moved from public schema
    CREATE MATERIALIZED VIEW analytics.mv_dashboard_kpis AS ...;
    CREATE MATERIALIZED VIEW analytics.mv_length_histogram AS ...;
    CREATE MATERIALIZED VIEW analytics.mv_organism_counts AS ...;
    CREATE MATERIALIZED VIEW analytics.mv_reviewed_ratio AS ...;
    CREATE MATERIALIZED VIEW analytics.mv_evidence_distribution AS ...;
    CREATE MATERIALIZED VIEW analytics.mv_keyword_frequency AS ...;
    ```
- [x] `AnalyticsRepository` — native `@Query` reading from `analytics.*` views
- [x] **Routing DataSource:** analytics-service uses REPLICA exclusively (all endpoints read-only)
  - [x] `@Transactional(readOnly = true)` at class level on service
  - [x] Connection pool sized for read-heavy load (HikariCP max 50)

#### 2.3 API Implementation

- [x] `AnalyticsController` — same endpoints as monolith, prefixed `/api/v1/analytics/`:
  - [x] `GET /api/v1/analytics/dashboard-kpis`
  - [x] `GET /api/v1/analytics/length-histogram`
  - [x] `GET /api/v1/analytics/by-organism`
  - [x] `GET /api/v1/analytics/reviewed-ratio`
  - [x] `GET /api/v1/analytics/evidence-levels`
  - [x] `GET /api/v1/analytics/keyword-frequency`
  - [x] `POST /api/v1/analytics/compare` (moved from monolith)
- [x] `AnalyticsService` — delegates to repositories, validates `limit` params
- [x] DTOs: `DashboardKpisDto`, `LengthBucketDto`, etc. (copied from monolith)

#### 2.4 Event Consumer

- [x] `ProteinImportedEventListener`:
  - [x] `@KafkaListener(topics = "protein.events.imported")`
  - [x] On event: `REFRESH MATERIALIZED VIEW CONCURRENTLY` for all 6 views
  - [x] Idempotent: checks `lastRefresh` timestamp to avoid duplicate refreshes
- **Status (2026-11-10):** Implemented. This is no longer blocked.
  - The listener is analytics `ViewRefreshConsumer`, in group
    `ANALYTICS_VIEW_REFRESHERS`. It consumes
    `ViewRefreshRequestedEvent(jobId, source, jobCompletedAt, correlationId)` from
    `shared-models`, with manual ack and `spring.json.use.type.headers=false`.
  - Idempotency is per `jobId` via `RefreshIdempotencyService`, not a
    `lastRefresh` timestamp.
  - Failures are surfaced through `ViewRefreshAlertService`.
  - ARCH-002 D3/P6 will replace this path:
    ingestion-completed event → gene-service statistics API →
    analytics-owned refresh tables.
  - The legacy refresh stays in parallel until parity (ARCH-002 Q7).

#### 2.5 Monolith Adaptation

- [x] Monolith `AnalyticsController` deprecated — returns `307` or `410`
- [x] Monolith analytics repositories removed

#### Phase 2 — Tests

- [x] `AnalyticsServiceTest` — unit
- [x] `AnalyticsControllerIntegrationTest` — Testcontainers with read replica
- [x] `ProteinImportedEventListenerTest` — `@EmbeddedKafka`. Delivered as
  `ViewRefreshConsumerTest` (unit) plus `ViewRefreshTransportTest`
  (`@EmbeddedKafka`, header-free payload), together with
  `RefreshIdempotencyServiceTest` and `MaterializedViewRefreshServiceTest`.
- [x] `ReadReplicaRoutingTest` — verifies zero primary connections during analytics queries

---

### PHASE 3 — Extract Import Service (`services/import-service/`)

**Goal:** Isolate the Spring Batch UniProt import pipeline.

**Rationale:** Import is batch-oriented, long-running, and self-contained. It owns the `import_job` table and publishes
domain events on completion.

#### 3.1 Service Setup

- [x] `ImportServiceApplication` — port `8083`
- [x] Dependencies: `common-starter`, `spring-boot-starter-batch`, `spring-batch-core`, `spring-kafka`
- [x] `bootstrap.yml`

#### 3.2 Database & Schema

- [x] Flyway `V1__import_schema.sql`:
    ```sql
    CREATE SCHEMA IF NOT EXISTS import_batch;
    CREATE TABLE import_batch.import_job (
        id UUID PRIMARY KEY,
        status VARCHAR(20) NOT NULL,
        file_name VARCHAR(200),
        entry_count BIGINT,
        records_processed BIGINT,
        total_estimated BIGINT,
        progress_percent INT,
        duration_ms BIGINT,
        error_message TEXT,
        created_at TIMESTAMPTZ DEFAULT NOW(),
        completed_at TIMESTAMPTZ
    );
    ```
- [x] `ImportJob` entity
- [x] `ImportJobRepository`
- [x] **Routing DataSource:** import-service uses PRIMARY only (writes + batch inserts)

#### 3.3 Batch Job Migration

- [x] Migrate `UniprotDatItemReader`, `UniprotTsvItemReader`, `ProteinEntryItemProcessor`, `ProteinAggregateWriter` from
  monolith
- [x] `ImportJobConfig` — Spring Batch job configuration
- [x] Chunk size: 250 (from PERF-001 tuning)
- [x] `JobExecutionListener` — `afterJob()` publishes `ProteinImportedEvent` to Kafka if COMPLETED
  > **2026-11-10:** This is implemented by `ImportJobRefreshViewsListener.afterJob()`.
  > It calls `MaterializedViewRefreshService.refreshAllDashboardViews(jobId, source)`,
  > which publishes `ViewRefreshRequestedEvent`, keyed by jobId.

#### 3.4 API Implementation

- [x] `ImportController`:
  - [x] `POST /api/v1/admin/import/uniprot` — trigger job
  - [x] `GET /api/v1/admin/import/status` — list jobs
  - [x] `GET /api/v1/admin/import/status/{jobId}` — job progress
  - [x] `POST /api/v1/admin/import/{jobId}/cancel` — cancel running job
- [x] `ImportService` — job launch, concurrency guard, file handling
- [x] `ImportJobMapper` — MapStruct

#### 3.5 Event Publishing

- [x] `ProteinImportedEvent` record:
    ```java
    public record ProteinImportedEvent(
        UUID jobId,
        String fileName,
        long entryCount,
        long recordsProcessed,
        Instant completedAt,
        String triggeredByUserId
    ) {}
    ```
- [x] `KafkaTemplate<String, ProteinImportedEvent>` in `ImportEventPublisher`
- [x] Published in `JobExecutionListener.afterJob()` only if `BatchStatus.COMPLETED`
  > **2026-11-10 — actual contract:**
  >
  > - The `ProteinImportedEvent` / `ImportEventPublisher` design above was not
  >   implemented under these names.
  > - The shipped record is `ViewRefreshRequestedEvent(String jobId, String source,
  >   Instant jobCompletedAt, String correlationId)` in
  >   `libs/shared-models/.../kafka/events`. It is sent to
  >   `KafkaTopics.PROTEIN_EVENTS_IMPORTED`.
  > - The payload does not carry entry counts or the triggering user.
  > - The versioned `catalog.protein-ingestion.completed.v1` successor is
  >   defined in `documentation/event-catalog.md` (ARCH-002).

#### 3.6 Monolith Adaptation

- [x] Monolith batch configuration disabled (`@ConditionalOnProperty`)
- [x] Monolith `ImportController` deprecated

#### Phase 3 — Tests

- [x] `ImportServiceTest` — unit
- [x] `ImportControllerIntegrationTest` — Testcontainers + `@EmbeddedKafka`
  (2026-11-10: the test exists, but it does not declare `@EmbeddedKafka`;
  Kafka transport is covered on the analytics side)
- [x] `ProteinImportedEventPublishingTest` — verifies Kafka message on job completion.
  Delivered as import `MaterializedViewRefreshServiceTest` and
  `ImportJobRefreshViewsListenerTest`; also see `ImportControllerAuditTest`.

---

### PHASE 4 — Extract Gene Service (`services/gene-service/`)

**Goal:** Extract the core protein catalog domain (search, filter, detail, export).

**Rationale:** This is the highest-traffic service. It must support read replica routing, maintain the existing API
contract, and delegate CSV export to the export-service in Phase 5.

> **Status (2026-11-10): partially delivered by ARCH-002, not by a fresh extraction.**
>
> - ARCH-002 P1 item 1.7 (commits `0af2921` and `cab1140`) moved the monolith
>   `backend/dashboard` to `backend/services/gene-service` and renamed the
>   package to `com.bioinformatics.geneservice`. The monolith therefore *became*
>   gene-service.
> - Catalog entities, repositories and services still live in the common-starter
>   "legacy catalog" (`app.legacy-catalog.enabled=true` in gene, import, export
>   and analytics), on the `public` schema.
> - Remaining extraction work is owned by ARCH-002:
>   - P2: `gene-model-lib` contracts and Feign APIs.
>   - P3: S2S authentication.
>   - P4: catalog ownership and outbox.
>   - P7: `gene_data` schema, roles and grants.
>   - P8: legacy catalog removal and ArchUnit.
> - Items below are annotated against that state.

#### 4.1 Service Setup

- [x] `GeneServiceApplication` — port `8084` (prod `8084`, dev random port `0`; ARCH-002 rename)
- [x] Dependencies: `common-starter`, `spring-data-jpa`, `spring-kafka`, `hibernate-types` (inherited from the monolith POM)
- [x] `bootstrap.yml` (no bootstrap file: `application.yaml` uses `spring.config.import` → Config Server; app name `gene-service`)

#### 4.2 Database & Schema

- [ ] Flyway migrations for `gene` schema:
    ```sql
    CREATE SCHEMA IF NOT EXISTS gene;
    -- All protein_entry, keyword, protein_keyword, go_term, protein_go_term,
    -- cross_reference, protein_feature, host_organism, protein_comment, protein_publication
    -- tables moved to gene schema with ON DELETE CASCADE FKs
    ```
- [ ] All entities updated with `@Table(schema = "gene", name = "...")`
- [ ] `ProteinEntryRepository`, `KeywordRepository`, etc. — schema-qualified queries
- [ ] **Routing DataSource:**
    - [ ] `GET`, `POST /search` → REPLICA
    - [ ] Any write operation (future) → PRIMARY
  > **2026-11-16 (supersedes the previous migration inventory):**
  >
  > - Gene-service Flyway lives in `src/main/resources/migration`: a single fresh V1
  >   on `public`, with current catalog tables, saved filters, FTS/indexes and pooled sequences.
  >   The former V2–V21 chain is removed; no other services' obsolete objects are created.
  > - The target schema is now `gene_data` (ARCH-002 P7), not `gene`.
  > - Gene-service uses plain `spring.datasource` (`SPRING_DATASOURCE_URL`) on
  >   the primary. Replica routing has not been wired for it.

#### 4.3 API Implementation

- [x] `GeneController` (served at `/api/genes`, not `/api/v1/genes` — existing contract kept):
    - [x] `GET /api/v1/genes` — paginated list (`GET /api/genes`)
    - [x] `POST /api/v1/genes/search` — filtered search (`POST /api/genes/search`; plus `POST /api/genes/count` used by export-service)
    - [x] `GET /api/v1/genes/{accession}` — detail by accession (`GET /api/genes/{accession}`)
    - [x] `GET /api/v1/genes/autocomplete` — autocomplete (`AutoCompleteController` at `/api/autocomplete`)
    - Also owns `SavedFilterController` at `/api/saved-filters`
- [x] `GeneService` — search, detail, specification composition (provider dispatch: `PostgresGeneService` + UniProtKB provider from REFACTOR-001/REMOTE-001)
- [ ] `GeneSpecification` — all filter predicates (copied from monolith, schema-prefixed) — still in common-starter legacy catalog; ownership move is ARCH-002 P4/P8
- [x] `ProteinMapper` — MapStruct (summary + detail) — delivered as `GeneMapper` in common-starter legacy catalog
- [x] DTOs: `ProteinSummaryDto`, `ProteinDetailDto`, `GeneSearchRequest`, `PagedResponse` (currently in common-starter; published contracts move to `gene-model-lib` in ARCH-002 P2)

#### 4.4 Service-to-Service Client

- [ ] `AuthServiceClient` — `@FeignClient("auth-service")`:
    - [ ] `validateToken(String token): UserDto`
    - [ ] Protected by Resilience4j circuit breaker (fallback: local cached key)
- [ ] `AnalyticsServiceClient` — `@FeignClient("analytics-service")` (future use)
  > **2026-11-10:**
  >
  > - Not implemented as designed. Services validate JWTs locally with the
  >   shared secret, so no `AuthServiceClient` exists.
  > - Inbound Feign clients *to* gene-service already exist:
  >   - import-service `SavedFilterClient`.
  >   - export-service `GeneServiceClient`, which calls `POST /api/genes/count`.
  > - Both forward forged `X-User-*` headers. Replacing these with S2S
  >   authentication is ARCH-002 P3.

#### 4.5 Monolith Adaptation

- [x] Monolith `GeneController` deprecated (superseded: monolith renamed to gene-service, ARCH-002 P1)
- [ ] Monolith `GeneService`, `GeneSpecification` removed (legacy catalog removal — ARCH-002 P8)
- [ ] Monolith `ProteinEntryRepository` and entities removed (legacy catalog removal — ARCH-002 P8)

#### Phase 4 — Tests

- [x] `GeneServiceTest` — unit (mock repo) — delivered as `PostgresGeneServiceTest`
- [x] `GeneControllerIntegrationTest` — Testcontainers with read replica routing (test exists; replica routing not applicable until wired)
- [ ] `GeneSpecificationTest` — all filter combinations (lives with legacy catalog in common-starter)
- [ ] `AuthServiceClientTest` — Feign client with Resilience4j fallback (no client; superseded by ARCH-002 P3 S2S tests)
- Additional delivered tests: `ControllerAuditPolicyTest`, `SavedFilterControllerTest`,
  `SavedFilterServiceTest`, `SavedFilterServiceCacheTest`, Postgres/UniProtKB `*SuggestionTest` suites

---

### PHASE 5 — Extract Export Service (`services/export-service/`)

**Goal:** Implement PIPE-001 as an independent microservice.

**Rationale:** Export is batch-oriented, file-I/O heavy, and has different scaling needs than the gene catalog. It
consumes gene data via Feign or Kafka and produces files asynchronously.

> **Status (2026-11-10): delivered by PIPE-001.** See `documentation/implementation/PIPE-001/`.
>
> - The Angular frontend includes `ExportPipelineWizardComponent`, an
>   `/exports` list/store, and `ExportPipelineService` calling
>   `${apiBaseUrl}/v1/exports`.
> - PIPE-001 recorded 23 focused backend tests and 148 frontend export tests
>   passing.
> - Still open in PIPE-001 / here:
>   - `ExportCompletedEvent` (5.6).
>   - `ExportPipelineAuditListener` for completed/failed lifecycle audit.
>   - Global completion notification.
>   - Replica routing.
>   - Accessibility and coverage qualification.
> - The PostgreSQL reader still reads the catalog through the legacy
>   `ProteinEntryService` on the shared database. Moving to the gene-service
>   API is ARCH-002 P2/P4.

#### 5.1 Service Setup

- [x] `ExportServiceApplication` — port `8085`
- [x] Dependencies: `common-starter`, `spring-boot-starter-batch`, `apache-poi`, `apache-commons-csv`, `spring-kafka`
- [x] `bootstrap.yml`

#### 5.2 Database & Schema

- [x] Flyway `V1__export_schema.sql`:
    ```sql
    CREATE SCHEMA IF NOT EXISTS export_pipe;
    CREATE TABLE export_pipe.export_pipeline (
        id BIGSERIAL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        name VARCHAR(200) NOT NULL,
        filter_json JSONB NOT NULL,
        format VARCHAR(10) NOT NULL,
        field_schema JSONB NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
        estimated_rows BIGINT,
        actual_rows BIGINT,
        file_path VARCHAR(500),
        file_size_bytes BIGINT,
        error_message TEXT,
        job_execution_id BIGINT,
        created_at TIMESTAMPTZ DEFAULT NOW(),
        started_at TIMESTAMPTZ,
        completed_at TIMESTAMPTZ,
        deleted_at TIMESTAMPTZ,
        duration_ms BIGINT
    );
    ```
- [x] `ExportPipeline` entity
- [x] `ExportPipelineRepository`
- [ ] **Routing DataSource:** (2026-11-10 correction — previously marked done; export-service uses only primary `spring.datasource`, no `RoutingDataSourceConfig`)
    - [ ] Reads (pipeline list, status) → REPLICA
    - [x] Writes (create, update status) → PRIMARY
  > **Actual schema (2026-11-10):** schema `export_data` (not `export_pipe`), migrations
  > `V1__export_pipeline.sql` (`export_pipeline` + `export_job_execution`) and
  > `V2__update_export_format_constraint.sql`.

#### 5.3 Batch Job Configuration

- [x] `ExportJobConfig` — Spring Batch job:
    - [x] Step 1: `validateAndEstimateStep` (Tasklet)
    - [x] Step 2: `exportChunkStep` (chunk = 500)
    - [x] Step 3: `assembleAndFinalizeStep` (Tasklet)
- [x] `ExportItemReader` — `JpaPagingItemReader<ProteinSummaryDto>` or Feign client to `gene-service`
    - [x] For local provider: direct DB read from gene schema (REPLICA)
    - [x] For UniProt provider: `UniProtApiExportItemReader` (cursor-based REST pagination)
- [x] `ExportItemProcessor` — `ProteinSummaryDto` → `Map<String, Object>`
- [x] `ExportItemWriter` — delegates to format-specific writer per chunk
- [x] `ExportFileStorageService` — `${APP_DIR}/exports/{userId}/{pipelineId}/`
  > **Actual implementation (2026-11-10):**
  >
  > - `ExportJobConfig` uses `ExportSourceDecider`, which routes to
  >   `UniProtExportJobStepConfig`.
  > - Tasklets: `ValidateAndEstimateTasklet` and `AssembleAndFinalizeTasklet`.
  > - Readers come from common:
  >   - `UniProtPostgresItemReader` (via legacy `ProteinEntryService`).
  >   - `UniProtApiItemReader` (REMOTE-001).
  > - Processors: `UniProtPostgresEntryProcessor`, `UniProtApiEntryProcessor`
  >   and `ProteinDetailProcessor`.
  > - Lifecycle: `ExportJobLifecycleListener`, `ExportProgressChunkListener`
  >   and `ExportCleanupJob`.
  > - The local-provider read goes to the primary, not the replica.

#### 5.4 Format Writers

- [x] `CsvExportWriter` — Apache Commons CSV, RFC 4180, UTF-8 BOM
- [x] `TsvExportWriter` — tab-delimited
- [x] `JsonExportWriter` — Jackson `SequenceWriter`
- [x] `ExcelExportWriter` — Apache POI SXSSF, streaming, auto-size
- [x] `ExportWriterFactory` — `getWriter(ExportFormat)` (plus format registry, `ExportItemWriter` and segment assemblers)

#### 5.5 API Implementation

- [x] `ExportPipelineController`:
    - [x] `POST /api/v1/exports/pipelines` — create & queue
    - [x] `GET /api/v1/exports/pipelines` — list (paginated)
    - [x] `GET /api/v1/exports/pipelines/{id}` — detail
    - [x] `GET /api/v1/exports/pipelines/{id}/status` — polling endpoint
    - [x] `GET /api/v1/exports/pipelines/{id}/download` — download URL
    - [x] `GET /api/v1/exports/pipelines/{id}/download-file` — stream file
    - [x] `POST /api/v1/exports/pipelines/{id}/retry` — re-run
    - [x] `DELETE /api/v1/exports/pipelines/{id}` — soft delete
    - [x] `GET /api/v1/exports/fields` — available field schema
  > **2026-11-10:**
  >
  > - Six `@Auditable` actions publish to `audit.events.log`:
  >   - `EXPORT_CREATE`
  >   - `DETAIL_VIEW`
  >   - `EXPORT_DOWNLOAD_REQUEST`
  >   - `EXPORT_DOWNLOAD`
  >   - `EXPORT_RETRY`
  >   - `EXPORT_DELETE`
  > - The gateway route is `/api/v1/exports/**`, with no rewrite.

#### 5.6 Event Publishing

- [ ] `ExportCompletedEvent` — published on COMPLETED or FAILED (2026-11-10: still open — topic
  `export.events.completed` is created by compose/K8s init but has no producer; no record class exists):
    ```java
    public record ExportCompletedEvent(
        Long pipelineId,
        Long userId,
        ExportStatus status,
        String filePath,
        Long fileSizeBytes,
        Instant completedAt
    ) {}
    ```

#### 5.7 Monolith Adaptation

- [x] Monolith `CsvExportService` and `GeneController.exportCsv()` deprecated
- [ ] Redirect to `/api/v1/exports/pipelines` (2026-11-10 correction: gene-service `POST /api/genes/export-csv`
  is a stub throwing `RuntimeException("Not implemented")`, not a redirect; the UI uses the PIPE-001 wizard and
  `GenesService.exportCsv` remains only in its spec — remove both or return 410/308)

#### Phase 5 — Tests

- [x] `ExportPipelineServiceTest` — unit
- [ ] `ExportJobIntegrationTest` — Testcontainers + `@EmbeddedKafka` (2026-11-10 correction: no such test exists;
  job flow is covered by unit-level `ExportEngineTest`, `ExportJobStepTrackerTest`, `ExportSourceDeciderTest`,
  `ExportItemProcessorTest`, `ExportPipelineLifeCycleServiceTest`)
- [x] `ExportFileStorageServiceTest` — temp directory assertions
- [x] `ExcelExportWriterTest` — valid XLSX generation
- [x] `ExportControllerIntegrationTest` — full pipeline CRUD + download (delivered as
  `ExportPipelineControllerIntegrationTest` and `ExportPipelineControllerAuditTest`)
- Additional delivered tests: `CsvExportWriterTest`, `ExportItemWriterTest`, `ExportWritersTest`,
  `ExportWriterFactoryTest`, `SegmentAssemblersTest`, `ExportFormatConverterTest`

---

### PHASE 6 — Extract Structure Service (`services/structure-service/`)

**Goal:** Implement STRUCT-001 as an independent microservice.

> **Status (2026-11-10):** Not started. The STRUCT-001 journal contains only its 2026-08-12 ticket entry, and
> no `structure-service` module, gateway route or frontend module exists.

**Rationale:** 3D protein visualization is orthogonal to the core catalog. It has external API dependencies (AlphaFold,
PDBe) and its own caching needs.

#### 6.1 Service Setup

- [ ] `StructureServiceApplication` — port `8086`
- [ ] Dependencies: `common-starter`, `spring-webflux` (WebClient for external APIs), `spring-data-redis`
- [ ] `bootstrap.yml`

#### 6.2 Database & Schema

- [ ] No local relational DB (uses Redis cache + external APIs).
- [ ] Optional: `structure` schema for cached SIFTS mappings if needed in Stage 2.

#### 6.3 External API Clients

- [ ] `AlphaFoldApiClient` — `WebClient` to `https://alphafold.ebi.ac.uk/api`
    - [ ] `@Cacheable(value = "structures", key = "'alphafold:' + #accession")` (Redis)
    - [ ] Retry with backoff (Resilience4j)
- [ ] `PdbDataApiClient` — `WebClient` to `https://data.rcsb.org/rest/v1`
    - [ ] `@Cacheable(value = "structures", key = "'pdb:' + #accession")`
- [ ] `SiftsApiClient` — `WebClient` to `https://www.ebi.ac.uk/pdbe/api`

#### 6.4 API Implementation

- [ ] `StructureController`:
    - [ ] `GET /api/v1/structures/{accession}` — availability
    - [ ] `GET /api/v1/structures/{accession}/features?source=ALPHAFOLD|PDB` — feature mappings
    - [ ] `GET /api/v1/structures/{accession}/cif-proxy` — proxy CIF file (CORS workaround)
- [ ] `StructureService` — orchestrates client calls, builds feature mappings
- [ ] `StructureFeatureMappingBuilder` — maps `protein_feature` to 3D coordinates
- [ ] DTOs: `StructureAvailabilityDto`, `AlphaFoldEntryDto`, `PdbEntryDto`, `StructureFeatureMappingDto`

#### 6.5 Cache Configuration

- [ ] Redis cache bucket `"structures"`:
    - [ ] AlphaFold metadata: TTL = 24 hours
    - [ ] PDB mappings: TTL = 7 days
    - [ ] CIF file blobs: TTL = 7 days (stored as binary in Redis or local disk)

#### 6.6 Monolith Adaptation

- [ ] New service — no monolith code to deprecate.

#### Phase 6 — Tests

- [ ] `StructureServiceTest` — unit (mock clients)
- [ ] `AlphaFoldApiClientTest` — `MockWebServer`
- [ ] `PdbDataApiClientTest` — `MockWebServer`
- [ ] `StructureControllerIntegrationTest` — `@MockBean` for external clients
- [ ] `RedisCacheTest` — `@Testcontainers` Redis

---

### PHASE 7 — Extract NLQ Service (`services/nlq-service/`)

**Goal:** Implement NLQ-001 as an independent microservice.

> **Status (2026-11-10):** Not started. The NLQ-001 journal contains only its 2026-08-12 ticket entry, and
> no `nlq-service` module or gateway route exists.

**Rationale:** LLM integration is infrastructure-heavy (external API costs, rate limiting, prompt management) and
benefits from independent scaling.

#### 7.1 Service Setup

- [ ] `NlqServiceApplication` — port `8087`
- [ ] Dependencies: `common-starter`, `spring-ai-core`, `spring-ai-google-ai-gemini`, `json-schema-validator`,
  `spring-kafka`
- [ ] `bootstrap.yml`

#### 7.2 Database & Schema

- [ ] No relational DB (stateless; conversation history stored in frontend).
- [ ] Optional: `nlq` schema for usage telemetry / audit.

#### 7.3 LLM Provider Abstraction

- [ ] `LlmProvider` interface:
    ```java
    public interface LlmProvider {
        String chat(List<LlmMessage> messages);
        String getProviderName();
    }
    ```
- [ ] `GeminiLlmProvider` — Spring AI `GoogleAiGeminiChatModel` or raw `RestClient`
- [ ] `OpenAiLlmProvider` — Spring AI `OpenAiChatModel` (stub for v1)
- [ ] `OllamaLlmProvider` — Spring AI `OllamaChatModel` (stub for v1)
- [ ] `LlmProviderResolver` — `@ConditionalOnProperty(name = "app.nlq.provider")`

#### 7.4 API Implementation

- [ ] `NlqController`:
    - [ ] `POST /api/v1/nlq/translate` → `NlqTranslateResponse`
    - [ ] `POST /api/v1/nlq/chat` → `NlqChatResponse`
    - [ ] `POST /api/v1/nlq/summarize` → `NlqSummarizeResponse`
- [ ] `NlqService` — orchestrates sanitize → guard → prompt → LLM → parse → validate
- [ ] `NlqPromptBuilder` — system prompts with JSON schema + few-shot examples
- [ ] `NlqResponseParser` — strips markdown, Jackson parsing, fuzzy repair
- [ ] `NlqInputSanitizer` — HTML strip, truncate, normalize
- [ ] `PromptInjectionGuard` — regex blocklist, uppercase ratio check

#### 7.5 Service-to-Service Client

- [ ] `GeneServiceClient` — `@FeignClient("gene-service")`:
    - [ ] `search(GeneSearchRequest): PagedResponse<ProteinSummaryDto>`
    - [ ] `getByAccession(String): ProteinDetailDto`
    - [ ] Circuit breaker fallback: 503 on gene-service down

#### 7.6 Event Publishing

- [ ] `NlqQueryExecutedEvent` — audit telemetry (async):
    ```java
    public record NlqQueryExecutedEvent(
        String userId,
        String provider,
        int inputTokens,
        int outputTokens,
        long latencyMs,
        String status
    ) {}
    ```

#### 7.7 Rate Limiting

- [ ] Bucket4j on `/api/v1/nlq/**`:
    - [ ] 10 req/min per user (protects Gemini free tier)
    - [ ] Separate bucket: 60 req/min per IP (Gateway-level)

#### Phase 7 — Tests

- [ ] `NlqServiceTest` — unit (mock LlmProvider + mock GeneServiceClient)
- [ ] `NlqPromptBuilderTest` — prompt content assertions
- [ ] `NlqResponseParserTest` — JSON extraction, markdown stripping, error handling
- [ ] `PromptInjectionGuardTest` — jailbreak detection
- [ ] `NlqControllerIntegrationTest` — `@MockBean` for LLM provider

---

### PHASE 8 — Extract Cross-Cutting Services

#### 8.1 Notification Service (`services/notification-service/`) — Port 8088

> **Status (2026-11-10):** Not started. No module exists, and no producers exist yet for `export.events.completed`
> or `notification.events.trigger`. PIPE-001's global completion notification is also open.

- [ ] `NotificationServiceApplication`
- [ ] `@KafkaListener` for:
    - [ ] `export.events.completed` → email notification with download link
    - [ ] `protein.events.imported` → admin dashboard notification
    - [ ] `auth.events.authenticated` → suspicious login alert (future)
- [ ] Email sender: Spring Mail (SMTP) or AWS SES integration
- [ ] In-app notification store (WebSocket or SSE for real-time)
- [ ] API: `GET /api/v1/notifications` (user's notification list)

#### 8.2 Audit Service (`services/audit-service/`) — Port 8089

- [x] `AuditServiceApplication`
- [x] Consume the existing `audit.events.log` topic; producer services publish asynchronously through the
  producer-only audit aspect/event publisher in `common-starter` and never call audit persistence synchronously.
  - Shared producer: `com.bioinformatics.common.audit`, using `shared-models` audit enums and `AuditLogEvent`.
    Dashboard annotations now publish through this starter; local audit persistence has been removed.
    Enable with `app.audit.enabled=true`, `spring.application.name` and `common.kafka.bootstrap-servers`; tune the bounded executor
    with `app.audit-pool.*`. Delivery is best-effort with logged queue/send failures, not a durable outbox.
    All-service clean-install decision (2026-11-16): no legacy records or histories are transferred or retained.
    Audit-service starts from its independent V1 migration; old dashboard audit scripts are removed.
    ARCH-002 P1 registers the producer through explicit auto-configuration without JPA/JDBC;
    all six producer services opt in. Consumers bind their own event types and ignore Java type headers.
    Existing-database cutover is out of scope; use an empty database, not an old Flyway history.
- [x] Single-event Kafka consumer with manual acknowledgements, Redis event-ID deduplication (24-hour TTL),
  and a database uniqueness constraint on `event_id`.
- [ ] Qualify duplicate/replay behavior across Redis expiry, restart, concurrent delivery and persistence failure;
  configure bounded consumer retries, dead-letter/poison-event handling, consumer-lag and persistence-failure metrics.
- [x] Audit-owned entity/repository, Flyway schema migration and paginated/filterable search implementation.
- [x] Config Server files: `audit-service.yml`, `audit-service-dev.yml`, `audit-service-prod.yml`.
  Base configuration uses `audit_log`, `classpath:migration`, the `audit-logger` consumer group and manual
  acknowledgements; dev uses a random port and prod uses port `8089`.
- [ ] Provision and verify exclusive audit-schema credentials and denial of catalog `gene_data` access.
  The local datasource still accepts shared environment-backed credentials; isolation is not yet demonstrated.
- [x] History scope for all services is clean-install only: no historical rows or Flyway histories are retained.
  Initialize `audit_log` using audit-service V1 with its pooled ID sequence; the old gene-service audit scripts
  are removed. Existing databases are unsupported by the new baselines.
- [x] Schema (`audit_log`; clean-install baseline is the audit-service V1 migration; no legacy-row handoff):
    ```sql
    CREATE SCHEMA IF NOT EXISTS audit_log;
    CREATE TABLE audit_log.audit_event (
        id BIGSERIAL PRIMARY KEY,
        event_id UUID NOT NULL UNIQUE,
        actor_identity VARCHAR(255),
        actor_type VARCHAR(30),
        source_service VARCHAR(100) NOT NULL,
        correlation_id VARCHAR(100),
        action VARCHAR(50) NOT NULL,
        target_type VARCHAR(50),
        target_id VARCHAR(100),
        status VARCHAR(20),
        ip_address INET,
        metadata JSONB,
        created_at TIMESTAMPTZ DEFAULT NOW()
    );
    ```
- [x] Audit-service owns the search controller, implemented as `POST /api/v1/admin/audit` with a validated search body
  and class-level `hasRole('ADMIN')` authorization.
- [x] Use the existing `POST /api/v1/admin/audit` search contract (validated body, paginated response)
  rather than introducing a second GET contract. The gateway routes the same path to `lb://audit-service`.
  (Gateway predicate is `/api/v1/admin/audit/**`; the `/api/v1/audit/**` route in 0.4 was never used.)
- [ ] Qualify authorization and pagination/filtering through the deployed gateway before rollout.
- [ ] Retention: configurable policy and scheduled cleanup/archive, with metrics and an operator runbook.
- [ ] High-throughput: batch Kafka consumer (100 messages per batch insert), preserving event ID, actor, source service
  and correlation ID.

#### Phase 8 — Tests

- [x] `AuditAspectTest` — actual AOP success/failure, skip/failure-only behavior, argument/result SpEL,
  explicit/default targets, and preservation of business outcomes on publisher failure.
- [x] `AuditEventPublisherTest` — caller-context snapshot before async execution, event fields/metadata,
  actor classification, and logged queue/immediate/asynchronous send failures.
- [x] `AuditContextFilterTest` and `AuditPublisherConfigTest` — context cleanup on success/failure,
  explicit audit opt-in, servlet-only filter and validated `app.audit-pool.*` bindings.
- [x] `AuditPublisherConsumerTest` — embedded Kafka using the real shared producer/consumer configuration and
  `AuditConsumer`; persistence and Redis idempotency services are mocked. Confirms transport compatibility,
  event fields/metadata and post-save deduplication marking, not real database persistence or Redis behavior.
- [ ] `NotificationServiceTest` — `@EmbeddedKafka`, verify email sending (no notification-service)
- [ ] `AuditServiceTest` — `@EmbeddedKafka`, verify batch insert (2026-11-10: `AuditServiceTest`, `AuditConsumerTest`
  and Testcontainers-based `AuditConsumerIntegrationTest` / `AuditIdempotencyServiceIntegrationTest` cover single-event
  persistence and deduplication; batch insert itself is not implemented)
- [ ] `AuditControllerIntegrationTest` — authz checks (2026-11-10: delivered in part as `AuditLogControllerIntegrationTest`,
  `AuditLogControllerAuditTest` and `SecurityConfigTest`; gateway-level qualification remains open)
- Additional delivered tests: `AuditLogRepositoryIntegrationTest`, `AuditSearchRequestTest`, `AuditLogMapperDecoratorTest`,
  `AuditLogTest`. ARCH-002 P1 also added correlation-ID propagation (servlet/Feign/Kafka, MDC `correlationId`)
  and header-free Kafka payloads; the full reactor run on 2026-11-06 passed (883 tests, 153 suites, 0 failures).

- `FreshDatabaseMigrationTest` (gene-service module) installs all six services' V1 migrations on a disposable
  PostgreSQL database in dependency order, checks independent histories/restarts, catalog FTS/sequences,
  analytics projection, custom export format and audit ID generation, and refuses an unmanaged nonempty schema.
  Gene and audit integration test contexts now use Flyway plus Hibernate validation rather than generated DDL.

---

### PHASE 9 — Frontend Adaptation & Monolith Drainage

> **Status (2026-11-10):**
>
> - **Frontend:** The frontend uses a single gateway base, `apiBaseUrl: '/api'`.
>   In dev, `proxy.conf.json` proxies to `localhost:8080`; in the container,
>   nginx proxies to `api-gateway:8080`. The gateway rewrites legacy
>   `/api/...` paths to `/api/v1/...`, so the per-service URL map in 9.1 is not
>   needed.
> - **Error handling:** `ErrorInterceptor` shows a toast on 429. 503 /
>   `Retry-After` handling and a `GatewayHealthService` do not exist.
> - **Monolith drainage:** Superseded. The monolith was renamed to gene-service
>   (ARCH-002 P1) instead of being drained to a 410 skeleton. Legacy catalog
>   removal and ArchUnit enforcement are ARCH-002 P8.
> - **Production deployment:** Owned by OPS-002. 18 Deployments were verified
>   on Minikube on 2026-10-26. Deployment/teardown scripts are still pending,
>   and OPS-002 docs still mention `dashboard`.

#### 9.1 Frontend Multi-Service Configuration

- [ ] `environment.ts` updated:
    ```typescript
    export const environment = {
      production: false,
      gatewayUrl: 'http://localhost:8080',
      // Service-specific URLs (fallback if Gateway unavailable)
      services: {
        auth: '/api/v1/auth',
        gene: '/api/v1/genes',
        analytics: '/api/v1/analytics',
        import: '/api/v1/admin/import',
        export: '/api/v1/exports',
        structure: '/api/v1/structures',
        nlq: '/api/v1/nlq',
        notification: '/api/v1/notifications',
        audit: '/api/v1/audit'
      }
    };
    ```
- [ ] `ApiService` base class — all feature services extend this:
    - [ ] Handles 503 with `Retry-After` (exponential backoff)
    - [x] Handles 429 (rate limit) with user toast (implemented in `ErrorInterceptor`, not a base class)
    - [ ] Circuit breaker state exposed via service signals
- [ ] `AuthInterceptor` — unchanged (still attaches Bearer token)
- [ ] `GatewayHealthService` — polls `/actuator/health` on Gateway; shows offline banner

#### 9.2 Feature Module Updates

- [ ] `GenesModule` — `GeneService` base URL → Gateway `/api/v1/genes`
- [ ] `AnalyticsModule` — `AnalyticsService` base URL → Gateway `/api/v1/analytics`
- [ ] `DashboardModule` — aggregates KPIs from analytics endpoint
- [ ] `ImportAdminModule` — `ImportAdminService` base URL → Gateway `/api/v1/admin/import`
- [ ] `ExportModule` — new `ExportPipelineService` → Gateway `/api/v1/exports`
  (delivered by PIPE-001: `ExportPipelineService` uses `${apiBaseUrl}/v1/exports`, wizard + `/exports` route)
- [ ] `StructureModule` — new `StructureService` → Gateway `/api/v1/structures`
- [ ] `NlqModule` — new `NlqService` → Gateway `/api/v1/nlq`

#### 9.3 Monolith Drainage

- [ ] Monolith stripped to skeleton:
    - [ ] All `@RestController` classes removed (or return 410 Gone)
    - [ ] All `@Service` business logic removed
    - [ ] All `@Repository` interfaces removed
    - [ ] All entity classes removed
    - [ ] Remaining: `MonolithFallbackController` — returns 410 for all `/api/**` paths
    - [ ] `application.yml` updated: `server.port=8090` (non-conflicting)
- [ ] `docker-compose.yml` for monolith removed from production stack
- [ ] DNS / load balancer routes all traffic to Gateway port 8080

#### Phase 9 — Tests

- [ ] `GatewayEndToEndTest` — full request chain: Angular → Gateway → Service → DB
- [ ] `MonolithDrainageTest` — every old monolith endpoint returns 410
- [ ] `FrontendServiceIntegrationTest` — Cypress/Playwright smoke tests per feature

---

### PHASE 10 — Database-per-Service Migration (Stage 2)

**Goal:** Physically split the shared PostgreSQL into per-service databases using CDC.

> **Status (2026-11-10):** ARCH-002 has split the prerequisites into two stages.
>
> - **Stage A** is owned by ARCH-002:
>   - P4: catalog outbox with relay `catalog.outbox` and
>     `catalog.protein-entry.changed.v1`.
>   - P7: `gene_data` schema, service roles and grants.
>   - P6: analytics-owned refresh tables.
> - **Stage B** (physical databases, Debezium) remains here, as ARCH-002 Q2
>   explicitly deferred it to this phase.
> - Read `gene` as `gene_data` and `export_pipe` as `export_data` below.
> - `ProteinEntryChangedEvent` corresponds to
>   `catalog.protein-entry.changed.v1` in `documentation/event-catalog.md`.
> - `ProteinImportedEvent` corresponds to `ViewRefreshRequestedEvent` today and
>   to `catalog.protein-ingestion.completed.v1` after ARCH-002 P6.

#### 10.1 CDC Infrastructure

- [ ] Debezium connector (Kafka Connect) deployed:
    - [ ] Monitors `gene.protein_entry` WAL
    - [ ] Publishes `ProteinEntryChangedEvent` to Kafka
- [ ] `analytics-service` consumes CDC events instead of relying on `REFRESH MATERIALIZED VIEW`
    - [ ] Builds real-time aggregations (optional, for v1.1)

#### 10.2 Schema Migration

- [ ] Each service gets its own PostgreSQL instance (or schema-isolated logical DB):
    - [ ] `auth-db` — `auth` schema only
    - [ ] `gene-db` — `gene` schema only
    - [ ] `analytics-db` — `analytics` schema + materialized views
    - [ ] `import-db` — `import_batch` schema
    - [ ] `export-db` — `export_pipe` schema
    - [ ] `audit-db` — `audit_log` schema
- [ ] Foreign key references replaced by:
    - [ ] Service calls (synchronous) for immediate consistency needs
    - [ ] Async events (Kafka) for eventual consistency

#### 10.3 Fresh Database Provisioning

- [ ] Provision empty per-service targets only after the dependent API/isolation work is complete.
- [ ] Initialize each target from its service-owned migration baseline; do not copy old records or Flyway history.
- [ ] Load any required catalog fixture through the supported import workflow and rebuild analytics from source APIs.
- [ ] Verify constraints, service startup and API behavior. No historical transfer, dual-write, old-DB fallback or
  zero-downtime data-preservation requirement applies to this fresh-install scope.

#### 10.4 Saga Implementation

- [ ] `ImportSagaOrchestrator` — manages cross-service import workflow:
    - [ ] Step 1: `import-service` completes batch
    - [ ] Step 2: Publish `ProteinImportedEvent`
    - [ ] Step 3: `analytics-service` confirms view refresh
    - [ ] Step 4: `notification-service` confirms admin alert
    - [ ] Compensation: if analytics refresh fails, event retried; no rollback of import

#### Phase 10 — Tests

- [ ] `CdcIntegrationTest` — Debezium captures WAL change → Kafka message
- [ ] `DatabasePerServiceTest` — service connects only to its own DB
- [ ] `SagaIntegrationTest` — full import saga with compensation

---

## Testing Strategy

### Unit Tests (per service)

| Service              | Target Coverage | Key Classes to Test                             |
|----------------------|-----------------|-------------------------------------------------|
| auth-service         | ≥ 85%           | AuthService, JwtService, UserDetailsService     |
| gene-service         | ≥ 85%           | GeneService, GeneSpecification, ProteinMapper   |
| analytics-service    | ≥ 80%           | AnalyticsService, all repositories              |
| import-service       | ≥ 80%           | ImportService, JobConfig, ItemProcessor         |
| export-service       | ≥ 80%           | ExportPipelineService, all writers              |
| structure-service    | ≥ 80%           | StructureService, all API clients               |
| nlq-service          | ≥ 80%           | NlqService, NlqPromptBuilder, NlqResponseParser |
| notification-service | ≥ 75%           | NotificationListener, EmailSender               |
| audit-service        | ≥ 75%           | AuditBatchListener, AuditRepository             |
| api-gateway          | ≥ 75%           | JwtGatewayFilter, RateLimitGatewayFilter        |

### Integration Tests

- [ ] `ServiceDiscoveryIntegrationTest` — all services register with Eureka
- [ ] `ConfigServerIntegrationTest` — properties refresh across services
- [ ] `GatewayRoutingIntegrationTest` — all routes resolve correctly
- [ ] `GatewaySecurityIntegrationTest` — JWT validation, role enforcement
- [ ] `ReadReplicaIntegrationTest` — `@Transactional(readOnly)` routes to replica
- [ ] `KafkaEventIntegrationTest` — publish → consume round-trip for all event types
- [ ] `CircuitBreakerIntegrationTest` — downstream failure → fallback → recovery
- [ ] `ServiceToServiceAuthIntegrationTest` — internal JWT propagation

### End-to-End Tests

- [ ] `FullImportPipelineE2ETest`:
    - [ ] Upload UniProt file → import-service → Kafka event → analytics refresh → notification email
- [ ] `GeneExplorerE2ETest`:
    - [ ] Search → filter → export pipeline → download → verify file
- [ ] `NlqChatE2ETest`:
    - [ ] Natural language query → NLQ service → gene search → results displayed

### Load Tests

- [ ] `GeneSearchLoadTest` (k6 or JMeter):
    - [ ] 100 concurrent users, 1000 req/min → p95 < 1s via read replica
- [ ] `GatewayResilienceLoadTest`:
    - [ ] Kill gene-service mid-test → verify 503 fallback + recovery
- [ ] `KafkaThroughputTest`:
    - [ ] 10K events/sec → no message loss, consumer lag < 5s

---

## Risk Register

| ID  | Risk                                                        | Probability | Mitigation                                                                                           |
|-----|-------------------------------------------------------------|-------------|------------------------------------------------------------------------------------------------------|
| R1  | Service extraction breaks existing integration tests        | High        | Maintain monolith in parallel until Phase 9; run dual test suites                                    |
| R2  | Read replica lag causes stale analytics after import        | Medium      | Event-driven refresh (Kafka) instead of polling; document acceptable lag (≤ 30s)                     |
| R3  | Network latency between services degrades response time     | Medium      | Collocate services in same AZ; use async where possible; cache aggressively                          |
| R4  | Kafka outage breaks event-driven workflows                  | Medium      | Services must operate degraded (e.g., analytics refresh manual trigger); monitor consumer lag        |
| R5  | Database schema drift between shared and per-service stages | Medium      | Flyway migrations versioned per service; schema validation in CI                                     |
| R6  | Circuit breaker flapping under partial load                 | Low         | Tune thresholds per service; use exponential backoff; alert on repeated opens                        |
| R7  | Config Server outage prevents service startup               | Low         | Config Client retry with backoff; local `bootstrap.yml` fallback; cache config on disk               |
| R8  | Service-to-service auth token leakage                       | Low         | Short-lived tokens (5m); no logging of tokens; mTLS in Stage 2                                       |
| R9  | Frontend bundle size increases with multi-service config    | Low         | Lazy-loaded modules; environment config externalized (not bundled); tree-shake unused clients        |
| R10 | Monolith drainage incomplete — zombie code remains          | Medium      | Static analysis (ArchUnit) to detect monolith imports; code review checklist; 410 verification tests |

---

## Commands

```powershell
# Start the full stack (infra + services + frontend) from the repository root
docker compose up -d

# Verify Eureka
curl http://localhost:8761/eureka/apps

# Verify Config Server
curl http://localhost:8888/gene-service/default

# Build and test the whole backend reactor
mvn -q -fae -f backend\pom.xml test

# Test a single service (with required upstream modules)
mvn -q -f backend\pom.xml -pl services/gene-service -am test

# Load test (k6) — not yet written
k6 run load-tests/gene-search.js
```

Kubernetes deployment commands are documented in OPS-002.

---

## Documentation Artifacts

| Artifact                         | Location                                                   |
|----------------------------------|------------------------------------------------------------|
| Service decomposition diagram    | `documentation/implementation/ARCH-001/overview.md`        |
| Ambiguity analysis & decisions   | `documentation/implementation/ARCH-001/analyse.md`         |
| Implementation plan (this file)  | `documentation/implementation/ARCH-001/plan.md`            |
| Chronological journal            | `documentation/implementation/ARCH-001/journal.md`         |
| API Gateway routing reference    | `backend-config/api-gateway.yml` (served by Config Server) |
| Event catalog                    | `documentation/event-catalog.md` (created by ARCH-002 P0)  |
| Database migration strategy      | `documentation/db-migration-strategy.md` (to be created)   |
| Service-to-service auth spec     | `documentation/service-auth-spec.md` (to be created)       |
| Runbook: local development setup | `docs/runbooks/local-dev-setup.md` (to be created)         |
| Runbook: adding a new service    | `docs/runbooks/add-new-service.md` (to be created)         |
