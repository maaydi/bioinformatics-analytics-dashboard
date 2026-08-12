# ARCH-001 — Ambiguities & Analysis

## Status: Resolved — implementation can proceed

---

## Resolved Decisions

### 1. Migration Strategy: Strangler Fig vs Big Bang

**Decision:** Strangler Fig pattern with incremental service extraction over 6 phases.

**Rationale:**

| Approach          | Risk    | Downtime | Rollback    | Team Capacity             |
|-------------------|---------|----------|-------------|---------------------------|
| **Big Bang**      | Extreme | Hours    | Impossible  | Requires full team freeze |
| **Strangler Fig** | Low     | Zero     | Per-service | Parallel development      |

- The monolith remains operational throughout the migration.
- Each extracted service runs alongside the monolith until cutover.
- A reverse proxy (later: API Gateway) routes traffic incrementally.
- Features are migrated one bounded context at a time.

**Cutover mechanism:**

```
Phase N: Service deployed → shadow traffic (validation) → partial traffic (canary) → full cutover → monolith code deleted
```

---

### 2. Database Strategy: Shared DB vs Database-per-Service

**Decision:** Two-stage approach.

**Stage 1 (Phases 0–5): Shared PostgreSQL with read replicas**

- All services connect to the same logical database.
- Schema separation via PostgreSQL schemas (`gene`, `analytics`, `auth`, `import`, `audit`).
- Read replicas handle analytics, search, and export queries.
- Routing DataSource switches between primary (writes) and replicas (reads) based on `@Transactional(readOnly=true)`.

**Stage 2 (Phase 6+): Database-per-Service**

- Each service owns its schema; foreign keys become service calls or async events.
- Event sourcing for cross-service consistency (e.g., import completion → analytics refresh).
- CDC (Debezium) captures changes and publishes to Kafka for downstream services.

**Rationale:**

- Immediate read-replica benefit without schema migration risk.
- Schema-per-service provides logical isolation during Stage 1.
- Physical DB split is deferred until service boundaries are proven stable.

---

### 3. Inter-Service Communication: REST vs Messaging

**Decision:** Hybrid — synchronous REST for queries, asynchronous messaging for events.

**Synchronous (REST via OpenFeign + Resilience4j):**

- Service-to-service queries (e.g., `gene-service` → `auth-service` for user validation).
- Must be idempotent and bounded by circuit breakers.

**Asynchronous (Spring Cloud Stream + Kafka/RabbitMQ):**

- Domain events: `ProteinImportedEvent`, `ExportCompletedEvent`, `AuditEvent`.
- Saga orchestration for long-running transactions (import → materialized view refresh).
- Notification triggers (email on export completion).

**Event Catalog:**

| Event                    | Publisher         | Consumers                           | Topic                       |
|--------------------------|-------------------|-------------------------------------|-----------------------------|
| `ProteinImportedEvent`   | import-service    | analytics-service, audit-service    | `protein.events.imported`   |
| `ExportCompletedEvent`   | export-service    | notification-service, audit-service | `export.events.completed`   |
| `UserAuthenticatedEvent` | auth-service      | audit-service                       | `auth.events.authenticated` |
| `AuditLogEvent`          | all services      | audit-service                       | `audit.events.log`          |
| `NlqQueryExecutedEvent`  | nlq-service       | audit-service                       | `nlq.events.executed`       |
| `StructureViewedEvent`   | structure-service | audit-service                       | `structure.events.viewed`   |

---

### 4. Authentication & Authorization Architecture

**Decision:** JWT validation at Gateway + service-level role verification.

**Flow:**

```
Client → API Gateway (validates JWT, extracts claims)
       → Route to service
       → Service validates role (defense in depth)
       → Service-to-service calls carry internal JWT (short-lived, service-scoped)
```

**Gateway responsibilities:**

- Token validation (signature, expiry).
- Claim extraction (`X-User-Id`, `X-User-Role`, `X-Data-Provider` headers forwarded).
- Rate limiting (per user + per service).

**Service responsibilities:**

- `@PreAuthorize` on endpoints.
- Service-to-service auth via internal JWT issued by `auth-service` (`/api/auth/service-token`).

**No OAuth2/OIDC for MVP:** The existing JWT scheme is preserved; migration to OAuth2 is a future ticket (ARCH-010).

---

### 5. Transaction Boundaries & Saga Pattern

**Decision:** Local transactions within services; sagas for cross-service workflows.

**Pattern:** Orchestration-based Saga for import workflow.

```
Import Saga (orchestrated by import-service):
  1. import-service: Parse & persist proteins (local TX)
  2. import-service: Publish ProteinImportedEvent
  3. analytics-service: Consume event → refresh materialized views (local TX)
  4. notification-service: Consume event → send admin notification (local TX)
  5. audit-service: Consume event → log import completion (local TX)
```

**Compensation:**

- If analytics refresh fails: event is retried (dead-letter queue after 3 attempts).
- Import job status remains `COMPLETED` even if downstream refresh is delayed.

**No 2PC/XA:** Avoided due to complexity and performance penalty; eventual consistency is acceptable.

---

### 6. Service Granularity: 10 Services

**Decision:** 10 services as proposed, with clear bounded contexts.

**Rationale per service:**

| Service                | Bounded Context               | Independent Deploy | Own DB (Stage 2)    |
|------------------------|-------------------------------|--------------------|---------------------|
| `api-gateway`          | Ingress / Edge                | N/A                | N/A                 |
| `discovery-server`     | Service Registry              | N/A                | N/A                 |
| `config-server`        | Configuration                 | N/A                | N/A                 |
| `auth-service`         | Identity & Access             | ✅                 | ✅                  |
| `gene-service`         | Protein Catalog               | ✅                 | ✅                  |
| `analytics-service`    | Aggregations & KPIs           | ✅                 | ✅ (read-optimized) |
| `import-service`       | Batch Ingestion               | ✅                 | ✅                  |
| `export-service`       | Batch Export (PIPE-001)       | ✅                 | ✅                  |
| `structure-service`    | 3D Visualization (STRUCT-001) | ✅                 | ✅                  |
| `nlq-service`          | AI Queries (NLQ-001)          | ✅                 | ✅ (lightweight)    |
| `notification-service` | Messaging & Alerts            | ✅                 | ✅                  |
| `audit-service`        | Audit Logging                 | ✅                 | ✅                  |

**Monolith fate:** Becomes a "skeleton" routing to services until fully drained. Final deletion in Phase 9.

---

### 7. Frontend Strategy: BFF vs Direct Gateway

**Decision:** Direct API Gateway (no BFF) for MVP.

**Rationale:**

- BFF (Backend-for-Frontend) adds operational complexity.
- The existing Angular app can be configured with multiple `HttpClient` base URLs per feature module.
- API Gateway handles aggregation where needed (e.g., `/api/dashboard` aggregates KPIs from analytics + gene counts).
- Future: If mobile app is added, introduce BFF (ARCH-011).

**Frontend adaptation:**

- Environment configs switch from single `apiUrl` to service-specific URLs.
- Auth interceptor remains unchanged (still attaches Bearer token).
- Feature modules lazy-load their service clients.

---

### 8. Read Replica Routing Strategy

**Decision:** Abstract Routing DataSource with `@Transactional(readOnly)` hint.

**Implementation:**

```java
@Bean
public DataSource routingDataSource(
    @Qualifier("primaryDataSource") DataSource primary,
    @Qualifier("replicaDataSource") DataSource replica) {

    AbstractRoutingDataSource router = new AbstractRoutingDataSource() {
        @Override
        protected Object determineCurrentLookupKey() {
            return TransactionSynchronizationManager.isCurrentTransactionReadOnly() 
                ? "REPLICA" : "PRIMARY";
        }
    };
    router.setTargetDataSources(Map.of("PRIMARY", primary, "REPLICA", replica));
    router.setDefaultTargetDataSource(primary);
    return router;
}
```

**Services using replicas:**

- `gene-service`: All `GET` / `POST /search` operations.
- `analytics-service`: All endpoints (exclusively read-only).
- `export-service`: Reader step in Spring Batch.
- `structure-service`: Read-only structure lookups.

**Services using primary only:**

- `import-service`: Writes during batch ingestion.
- `auth-service`: User registration, password changes.
- `export-service`: Pipeline status updates (writes).

---

### 9. Circuit Breaker Configuration

**Decision:** Resilience4j with per-service defaults.

**Default policy (customizable per service in Config Server):**

```yaml
resilience4j.circuitbreaker:
  configs:
    default:
      slidingWindowSize: 10
      failureRateThreshold: 50
      waitDurationInOpenState: 30s
      permittedNumberOfCallsInHalfOpenState: 3
      slowCallRateThreshold: 80
      slowCallDurationThreshold: 2s
```

**Protected calls:**

- Gateway → any service (fallback: 503 with Retry-After).
- `gene-service` → `auth-service` (fallback: reject request).
- `nlq-service` → Gemini API (fallback: return 503).
- `structure-service` → AlphaFold API (fallback: PDBe iframe).

---

### 10. Configuration Management Strategy

**Decision:** Spring Cloud Config Server with Git backend + local override profile.

**Repository structure:**

```
config-repo/
├── application.yml          # Shared across all services
├── api-gateway.yml
├── gene-service.yml
├── gene-service-dev.yml
├── gene-service-prod.yml
├── analytics-service.yml
├── import-service.yml
├── export-service.yml
├── auth-service.yml
├── structure-service.yml
├── nlq-service.yml
├── notification-service.yml
├── audit-service.yml
```

**Secrets:** Spring Cloud Config Server + AWS Secrets Manager / HashiCorp Vault (Stage 2). For Stage 1, encrypted
properties in Git (`{cipher}...`).

**Refresh:** `@RefreshScope` beans + `/actuator/refresh` endpoint (manual) or Spring Cloud Bus (auto on Git webhook).

---

## Open Questions (non-blocking)

| Question                                                         | Owner   | Priority | Resolution Path                                                              |
|------------------------------------------------------------------|---------|----------|------------------------------------------------------------------------------|
| Should we use Kubernetes or Docker Compose for local dev?        | DevOps  | Medium   | Docker Compose for local; K8s manifests as future ticket (ARCH-012)          |
| Do we need distributed tracing (Zipkin/Micrometer)?              | Arch    | Medium   | Add Micrometer tracing in Phase 0; Zipkin deferred to ARCH-013               |
| Should the monolith remain as a "fallback" service indefinitely? | Product | Low      | No — full drainage by Phase 9; document deprecation timeline                 |
| How to handle Flyway migrations when DB is shared (Stage 1)?     | Backend | High     | Each service owns its schema; Flyway migrations in `db/migration/<service>/` |
| Do we need a service mesh (Istio/Linkerd) for mTLS?              | DevOps  | Low      | Defer to Stage 2; use Spring Cloud SSL for now                               |

---

**Last Updated:** 2026-08-12
