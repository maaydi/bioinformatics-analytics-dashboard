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
- [x] Phase 2 — analytics-service extracted (7 of 8 items; Phase 2.4 Kafka listener deferred)
- [ ] Phase 3 — import-service extracted
- [ ] Phase 4 — gene-service extracted
- [ ] Phase 5 — export-service extracted
- [ ] Phase 6 — structure-service extracted
- [ ] Phase 7 — nlq-service extracted
- [ ] Phase 8 — notification + audit services extracted
- [ ] Phase 9 — Frontend adapted, monolith drained
- [ ] Phase 10 — Database-per-service
- [ ] Integration tests written
- [ ] End-to-end tests written
- [ ] Load tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

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
  - [x] `RoutingDataSourceConfig` — `AbstractRoutingDataSource` with PRIMARY/REPLICA keys
  - [x] `KafkaProducerConfig` — `KafkaTemplate` with JSON serializer for domain events
  - [x] `KafkaConsumerConfig` — `ConcurrentKafkaListenerContainerFactory` with JSON deserializer
  - [x] `TracingConfig` — Micrometer tracing with Brave propagation
  - [x] `WebClientConfig` — `WebClient.Builder` with load-balanced base URLs (Eureka)
  - [x] `CommonSecurityConfig` — common `SecurityFilterChain` pattern for service-level JWT validation
  - [x] `CommonGlobalExceptionHandler` — shared `@RestControllerAdvice` (reused from monolith, adapted)
- [ ] Publish to local Maven repository (`./mvnw install`)
- [ ] All services add dependency: `com.bioinformatics:common-starter:1.0.0`

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
    - [ ] `api-gateway.yml` — routing table, JWT secret, rate limits
    - [ ] `auth-service.yml`, `gene-service.yml`, `analytics-service.yml`, etc.
  - [x] `*-dev.yml`, `*-prod.yml` environment overrides
- [ ] Encrypt sensitive properties using Spring Cloud Config Encrypt (`{cipher}...`)
- [x] Docker service in `docker-compose.infra.yml`

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

- [ ] Kafka 3.7 + Zookeeper (or KRaft mode) containers
- [ ] Topics auto-created on startup:
    - [ ] `protein.events.imported` — 3 partitions, replication factor 1 (local dev)
    - [ ] `export.events.completed` — 3 partitions
    - [ ] `auth.events.authenticated` — 1 partition
    - [ ] `audit.events.log` — 6 partitions (high throughput)
    - [ ] `nlq.events.executed` — 1 partition
    - [ ] `structure.events.viewed` — 1 partition
    - [ ] `notification.events.trigger` — 3 partitions
- [ ] Kafka UI (optional) for local dev monitoring

#### 0.6 Database — PostgreSQL Primary + Replica (`docker-compose.infra.yml`)

- [ ] `postgres-primary` container:
    - [ ] `POSTGRES_DB=uniprot`
    - [ ] `wal_level=replica`, `max_wal_senders=5`, `max_replication_slots=5`
    - [ ] Init script creates replication user
- [ ] `postgres-replica` container:
    - [ ] `pg_basebackup` from primary on first start
    - [ ] `hot_standby=on`
    - [ ] Read-only connections allowed
- [ ] Schema separation (Stage 1):
    ```sql
    CREATE SCHEMA IF NOT EXISTS auth;
    CREATE SCHEMA IF NOT EXISTS gene;
    CREATE SCHEMA IF NOT EXISTS analytics;
    CREATE SCHEMA IF NOT EXISTS import_batch;
    CREATE SCHEMA IF NOT EXISTS export_pipe;
    CREATE SCHEMA IF NOT EXISTS audit_log;
    ```
- [ ] `pgAdmin` container (optional) for local dev

#### 0.7 Docker Compose Infrastructure Stack

- [ ] `docker-compose.infra.yml` services:
    - [ ] `eureka-server` (port 8761)
    - [ ] `config-server` (port 8888)
    - [ ] `api-gateway` (port 8080)
    - [ ] `kafka` (port 9092)
    - [ ] `zookeeper` (port 2181)
    - [ ] `postgres-primary` (port 5432)
    - [ ] `postgres-replica` (port 5433)
    - [ ] `redis` (port 6379) — for Gateway rate limiting + service caching
- [ ] Health checks and dependency ordering (`depends_on` with condition)
- [ ] Shared network `bioinformatics-network`

#### Phase 0 — Tests

- [ ] `DiscoveryServerIntegrationTest` — Eureka registers itself
- [ ] `ConfigServerIntegrationTest` — serves `application.yml` correctly
- [ ] `GatewayRoutingTest` — routes `/api/v1/genes/**` to mock downstream
- [ ] `GatewayJwtFilterTest` — validates token, rejects invalid, forwards claims
- [ ] `GatewayRateLimitTest` — exceeds limit → 429
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

- [ ] `ProteinImportedEventListener`:
    - [ ] `@KafkaListener(topics = "protein.events.imported")`
    - [ ] On event: `REFRESH MATERIALIZED VIEW CONCURRENTLY` for all 6 views
    - [ ] Idempotent: checks `lastRefresh` timestamp to avoid duplicate refreshes
- **Status:** Blocked on Phase 3 (Import Service) event definition. Phase 2.4 is first task in Phase 3.

#### 2.5 Monolith Adaptation

- [x] Monolith `AnalyticsController` deprecated — returns `307` or `410`
- [x] Monolith analytics repositories removed

#### Phase 2 — Tests

- [x] `AnalyticsServiceTest` — unit
- [x] `AnalyticsControllerIntegrationTest` — Testcontainers with read replica
- [ ] `ProteinImportedEventListenerTest` — `@EmbeddedKafka` (blocked on Phase 3 event definition)
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

#### 3.4 API Implementation

- [x] `ImportController`:
  - [x] `POST /api/v1/admin/import/uniprot` — trigger job
  - [x] `GET /api/v1/admin/import/status` — list jobs
  - [x] `GET /api/v1/admin/import/status/{jobId}` — job progress
  - [x] `POST /api/v1/admin/import/{jobId}/cancel` — cancel running job
- [x] `ImportService` — job launch, concurrency guard, file handling
- [x] `ImportJobMapper` — MapStruct

#### 3.5 Event Publishing

- [ ] `ProteinImportedEvent` record:
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
- [ ] `KafkaTemplate<String, ProteinImportedEvent>` in `ImportEventPublisher`
- [ ] Published in `JobExecutionListener.afterJob()` only if `BatchStatus.COMPLETED`

#### 3.6 Monolith Adaptation

- [x] Monolith batch configuration disabled (`@ConditionalOnProperty`)
- [x] Monolith `ImportController` deprecated

#### Phase 3 — Tests

- [x] `ImportServiceTest` — unit
- [x] `ImportControllerIntegrationTest` — Testcontainers + `@EmbeddedKafka`
- [ ] `ProteinImportedEventPublishingTest` — verifies Kafka message on job completion

---

### PHASE 4 — Extract Gene Service (`services/gene-service/`)

**Goal:** Extract the core protein catalog domain (search, filter, detail, export).

**Rationale:** This is the highest-traffic service. It must support read replica routing, maintain the existing API
contract, and delegate CSV export to the export-service in Phase 5.

#### 4.1 Service Setup

- [ ] `GeneServiceApplication` — port `8084`
- [ ] Dependencies: `common-starter`, `spring-data-jpa`, `spring-kafka`, `hibernate-types`
- [ ] `bootstrap.yml`

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

#### 4.3 API Implementation

- [ ] `GeneController`:
    - [ ] `GET /api/v1/genes` — paginated list
    - [ ] `POST /api/v1/genes/search` — filtered search
    - [ ] `GET /api/v1/genes/{accession}` — detail by accession (not numeric id)
    - [ ] `GET /api/v1/genes/autocomplete` — autocomplete (moved from monolith)
- [ ] `GeneService` — search, detail, specification composition
- [ ] `GeneSpecification` — all filter predicates (copied from monolith, schema-prefixed)
- [ ] `ProteinMapper` — MapStruct (summary + detail)
- [ ] DTOs: `ProteinSummaryDto`, `ProteinDetailDto`, `GeneSearchRequest`, `PagedResponse`

#### 4.4 Service-to-Service Client

- [ ] `AuthServiceClient` — `@FeignClient("auth-service")`:
    - [ ] `validateToken(String token): UserDto`
    - [ ] Protected by Resilience4j circuit breaker (fallback: local cached key)
- [ ] `AnalyticsServiceClient` — `@FeignClient("analytics-service")` (future use)

#### 4.5 Monolith Adaptation

- [ ] Monolith `GeneController` deprecated
- [ ] Monolith `GeneService`, `GeneSpecification` removed
- [ ] Monolith `ProteinEntryRepository` and entities removed

#### Phase 4 — Tests

- [ ] `GeneServiceTest` — unit (mock repo)
- [ ] `GeneControllerIntegrationTest` — Testcontainers with read replica routing
- [ ] `GeneSpecificationTest` — all filter combinations
- [ ] `AuthServiceClientTest` — Feign client with Resilience4j fallback

---

### PHASE 5 — Extract Export Service (`services/export-service/`)

**Goal:** Implement PIPE-001 as an independent microservice.

**Rationale:** Export is batch-oriented, file-I/O heavy, and has different scaling needs than the gene catalog. It
consumes gene data via Feign or Kafka and produces files asynchronously.

#### 5.1 Service Setup

- [ ] `ExportServiceApplication` — port `8085`
- [ ] Dependencies: `common-starter`, `spring-boot-starter-batch`, `apache-poi`, `apache-commons-csv`, `spring-kafka`
- [ ] `bootstrap.yml`

#### 5.2 Database & Schema

- [ ] Flyway `V1__export_schema.sql`:
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
- [ ] `ExportPipeline` entity
- [ ] `ExportPipelineRepository`
- [ ] **Routing DataSource:**
    - [ ] Reads (pipeline list, status) → REPLICA
    - [ ] Writes (create, update status) → PRIMARY

#### 5.3 Batch Job Configuration

- [ ] `ExportJobConfig` — Spring Batch job:
    - [ ] Step 1: `validateAndEstimateStep` (Tasklet)
    - [ ] Step 2: `exportChunkStep` (chunk = 500)
    - [ ] Step 3: `assembleAndFinalizeStep` (Tasklet)
- [ ] `ExportItemReader` — `JpaPagingItemReader<ProteinSummaryDto>` or Feign client to `gene-service`
    - [ ] For local provider: direct DB read from gene schema (REPLICA)
    - [ ] For UniProt provider: `UniProtApiExportItemReader` (cursor-based REST pagination)
- [ ] `ExportItemProcessor` — `ProteinSummaryDto` → `Map<String, Object>`
- [ ] `ExportItemWriter` — delegates to format-specific writer per chunk
- [ ] `ExportFileStorageService` — `${APP_DIR}/exports/{userId}/{pipelineId}/`

#### 5.4 Format Writers

- [ ] `CsvExportWriter` — Apache Commons CSV, RFC 4180, UTF-8 BOM
- [ ] `TsvExportWriter` — tab-delimited
- [ ] `JsonExportWriter` — Jackson `SequenceWriter`
- [ ] `ExcelExportWriter` — Apache POI SXSSF, streaming, auto-size
- [ ] `ExportWriterFactory` — `getWriter(ExportFormat)`

#### 5.5 API Implementation

- [ ] `ExportPipelineController`:
    - [ ] `POST /api/v1/exports/pipelines` — create & queue
    - [ ] `GET /api/v1/exports/pipelines` — list (paginated)
    - [ ] `GET /api/v1/exports/pipelines/{id}` — detail
    - [ ] `GET /api/v1/exports/pipelines/{id}/status` — polling endpoint
    - [ ] `GET /api/v1/exports/pipelines/{id}/download` — download URL
    - [ ] `GET /api/v1/exports/pipelines/{id}/download-file` — stream file
    - [ ] `POST /api/v1/exports/pipelines/{id}/retry` — re-run
    - [ ] `DELETE /api/v1/exports/pipelines/{id}` — soft delete
    - [ ] `GET /api/v1/exports/fields` — available field schema

#### 5.6 Event Publishing

- [ ] `ExportCompletedEvent` — published on COMPLETED or FAILED:
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

- [ ] Monolith `CsvExportService` and `GeneController.exportCsv()` deprecated
- [ ] Redirect to `/api/v1/exports/pipelines`

#### Phase 5 — Tests

- [ ] `ExportPipelineServiceTest` — unit
- [ ] `ExportJobIntegrationTest` — Testcontainers + `@EmbeddedKafka`
- [ ] `ExportFileStorageServiceTest` — temp directory assertions
- [ ] `ExcelExportWriterTest` — valid XLSX generation
- [ ] `ExportControllerIntegrationTest` — full pipeline CRUD + download

---

### PHASE 6 — Extract Structure Service (`services/structure-service/`)

**Goal:** Implement STRUCT-001 as an independent microservice.

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

- [ ] `NotificationServiceApplication`
- [ ] `@KafkaListener` for:
    - [ ] `export.events.completed` → email notification with download link
    - [ ] `protein.events.imported` → admin dashboard notification
    - [ ] `auth.events.authenticated` → suspicious login alert (future)
- [ ] Email sender: Spring Mail (SMTP) or AWS SES integration
- [ ] In-app notification store (WebSocket or SSE for real-time)
- [ ] API: `GET /api/v1/notifications` (user's notification list)

#### 8.2 Audit Service (`services/audit-service/`) — Port 8089

- [ ] `AuditServiceApplication`
- [ ] `@KafkaListener` for `audit.events.log`:
    - [ ] Batches inserts into `audit_log` table (primary DB, write-optimized)
- [ ] Schema:
    ```sql
    CREATE SCHEMA IF NOT EXISTS audit_log;
    CREATE TABLE audit_log.audit_event (
        id BIGSERIAL PRIMARY KEY,
        actor_user_id BIGINT,
        action VARCHAR(50) NOT NULL,
        target_type VARCHAR(50),
        target_id VARCHAR(100),
        status VARCHAR(20),
        ip_address INET,
        metadata JSONB,
        created_at TIMESTAMPTZ DEFAULT NOW()
    );
    ```
- [ ] API: `GET /api/v1/audit` (ADMIN only, paginated, filterable)
- [ ] High-throughput: batch Kafka consumer (100 messages per batch insert)

#### Phase 8 — Tests

- [ ] `NotificationServiceTest` — `@EmbeddedKafka`, verify email sending
- [ ] `AuditServiceTest` — `@EmbeddedKafka`, verify batch insert
- [ ] `AuditControllerIntegrationTest` — authz checks

---

### PHASE 9 — Frontend Adaptation & Monolith Drainage

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
    - [ ] Handles 429 (rate limit) with user toast
    - [ ] Circuit breaker state exposed via service signals
- [ ] `AuthInterceptor` — unchanged (still attaches Bearer token)
- [ ] `GatewayHealthService` — polls `/actuator/health` on Gateway; shows offline banner

#### 9.2 Feature Module Updates

- [ ] `GenesModule` — `GeneService` base URL → Gateway `/api/v1/genes`
- [ ] `AnalyticsModule` — `AnalyticsService` base URL → Gateway `/api/v1/analytics`
- [ ] `DashboardModule` — aggregates KPIs from analytics endpoint
- [ ] `ImportAdminModule` — `ImportAdminService` base URL → Gateway `/api/v1/admin/import`
- [ ] `ExportModule` — new `ExportPipelineService` → Gateway `/api/v1/exports`
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

#### 10.3 Data Migration

- [ ] `gene-service` data migration:
    - [ ] Export `gene.*` tables from shared DB
    - [ ] Import to `gene-db`
    - [ ] Verify row counts and constraints
- [ ] Zero-downtime migration:
    - [ ] Dual-write period: monolith writes to both old and new DB
    - [ ] Read from new DB, fallback to old DB on mismatch
    - [ ] Cutover when consistency verified

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

```bash
# Start full infrastructure stack
cd infrastructure
docker-compose -f docker-compose.infra.yml up -d

# Verify Eureka
curl http://localhost:8761/eureka/apps

# Verify Config Server
curl http://localhost:8888/gene-service/default

# Build all services
./mvnw clean install -pl libs/common-starter
./mvnw clean package -pl services/auth-service,services/gene-service,...

# Run service tests
./mvnw -pl services/gene-service test

# Run integration tests (requires infra stack running)
./mvnw -pl services/gene-service verify -P integration-test

# Load test (k6)
k6 run load-tests/gene-search.js
```

---

## Documentation Artifacts

| Artifact                         | Location                                                   |
|----------------------------------|------------------------------------------------------------|
| Service decomposition diagram    | `documentation/implementation/ARCH-001/overview.md`        |
| Ambiguity analysis & decisions   | `documentation/implementation/ARCH-001/analyse.md`         |
| Implementation plan (this file)  | `documentation/implementation/ARCH-001/plan.md`            |
| Chronological journal            | `documentation/implementation/ARCH-001/journal.md`         |
| API Gateway routing reference    | `infrastructure/api-gateway/src/main/resources/routes.yml` |
| Event catalog                    | `documentation/event-catalog.md` (to be created)           |
| Database migration strategy      | `documentation/db-migration-strategy.md` (to be created)   |
| Service-to-service auth spec     | `documentation/service-auth-spec.md` (to be created)       |
| Runbook: local development setup | `docs/runbooks/local-dev-setup.md` (to be created)         |
| Runbook: adding a new service    | `docs/runbooks/add-new-service.md` (to be created)         |
