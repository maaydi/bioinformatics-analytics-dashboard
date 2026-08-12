# ARCH-001 — Microservices Architecture Migration (Strangler Fig)

## Description

Redesign the existing monolithic Spring Boot + Angular + PostgreSQL application into a distributed microservices
architecture using the **Strangler Fig pattern**. The migration proceeds incrementally: services are extracted from the
monolith one bounded context at a time, deployed alongside the legacy application, and traffic is gradually rerouted via
an API Gateway until the monolith is fully drained.

The target architecture introduces:

- **API Gateway** (Spring Cloud Gateway) as the single entry point for all clients.
- **Service Discovery** (Spring Cloud Netflix Eureka) for dynamic service registration.
- **Centralized Configuration** (Spring Cloud Config Server) with environment-specific property files.
- **Circuit Breakers** (Resilience4j) for fault tolerance across service boundaries.
- **Asynchronous Messaging** (Spring Cloud Stream + Kafka) for event-driven communication.
- **Read Replication** with a Routing DataSource directing write traffic to the primary PostgreSQL node and read traffic
  to replica nodes.

All existing features (genes, analytics, import, auth, export, saved filters) are remapped to dedicated services. The
three planned features (STRUCT-001, NLQ-001, PIPE-001) are implemented as first-class microservices from inception.

---

## Scope

| Layer / Module           | Artifact                           | Description                                                                 |
|--------------------------|------------------------------------|-----------------------------------------------------------------------------|
| **Infrastructure**       | `infrastructure/api-gateway/`      | Spring Cloud Gateway with JWT validation, rate limiting, routing            |
| **Infrastructure**       | `infrastructure/discovery-server/` | Netflix Eureka Server                                                       |
| **Infrastructure**       | `infrastructure/config-server/`    | Spring Cloud Config Server (Git backend)                                    |
| **Infrastructure**       | `docker-compose.infra.yml`         | Local dev stack: Kafka, Zookeeper, PostgreSQL primary + replica             |
| **Shared Library**       | `libs/common-starter/`             | Shared auto-configuration: JWT decoder, Resilience4j, routing DS, tracing   |
| **Auth Service**         | `services/auth-service/`           | JWT issuance, user/role management, service-to-service tokens               |
| **Gene Service**         | `services/gene-service/`           | Protein catalog: search, filter, detail, CSV export (moved from monolith)   |
| **Analytics Service**    | `services/analytics-service/`      | Dashboard KPIs, charts, materialized views (read-replica only)              |
| **Import Service**       | `services/import-service/`         | Spring Batch UniProt ingestion, job tracking (primary DB only)              |
| **Export Service**       | `services/export-service/`         | PIPE-001: Async batch export pipeline (CSV/TSV/JSON/Excel)                  |
| **Structure Service**    | `services/structure-service/`      | STRUCT-001: 3D protein viewer, AlphaFold/PDB integration                    |
| **NLQ Service**          | `services/nlq-service/`            | NLQ-001: Natural language query, AI summarization, LLM provider abstraction |
| **Notification Service** | `services/notification-service/`   | Async email, in-app alerts, export completion notifications                 |
| **Audit Service**        | `services/audit-service/`          | Centralized audit log ingestion from all services via Kafka                 |
| **Monolith (Legacy)**    | `backend/` (remaining)             | Skeleton routing to services until fully drained                            |
| **Frontend**             | `frontend/`                        | Multi-base-url configuration, service-aware interceptors                    |

---

## Service Decomposition

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Angular Frontend                                │
│         (service-aware HttpClient, lazy-loaded feature modules)              │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │ HTTPS
┌───────────────────────────────────▼─────────────────────────────────────────┐
│                         API Gateway (Spring Cloud Gateway)                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ JWT Filter  │  │ Rate Limiter│  │  CB Filter  │  │  Routing Predicates │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
           ┌────────────┐  ┌────────────┐  ┌────────────┐
           │   Eureka   │  │   Config   │  │   Kafka    │
           │  Server    │  │  Server    │  │  Cluster   │
           └────────────┘  └────────────┘  └────────────┘
                                    │
        ┌───────────┬───────────┬───┴───┬───────────┬───────────┐
        ▼           ▼           ▼       ▼           ▼           ▼
   ┌────────┐ ┌──────────┐ ┌────────┐ ┌───────┐ ┌────────┐ ┌──────────┐
   │  Auth  │ │   Gene   │ │Analytics│ │ Import│ │ Export │ │ Structure│
   │Service │ │ Service  │ │Service │ │Service│ │Service │ │ Service  │
   └───┬────┘ └────┬─────┘ └───┬────┘ └───┬───┘ └───┬────┘ └────┬─────┘
       │           │           │          │         │           │
       └───────────┴───────────┴────┬─────┴─────────┴───────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              ┌─────────┐    ┌──────────┐    ┌──────────┐
              │ Primary │    │  Read    │    │   NLQ    │
              │   PG    │    │ Replica  │    │ Service  │
              └─────────┘    └──────────┘    └────┬─────┘
                                                   │
                              ┌────────────────────┼────────────────────┐
                              ▼                    ▼                    ▼
                         ┌─────────┐        ┌──────────┐        ┌──────────┐
                         │Notification│      │  Audit   │        │  Monolith│
                         │ Service  │        │ Service  │        │ (Legacy) │
                         └─────────┘        └──────────┘        └──────────┘
```

---

## Acceptance Criteria

### AC-1 — Infrastructure Bootstrap

```
Given the infrastructure services are started via docker-compose.infra.yml
When all containers report healthy
Then Eureka dashboard shows all services registered
  and Config Server serves properties from Git backend
  and API Gateway routes requests to the correct downstream service
  and Kafka topics are auto-created for the event catalog
```

### AC-2 — Service Extraction & Registration

```
Given the auth-service is extracted from the monolith
When it starts independently
Then it registers with Eureka
  and appears in the Gateway routing table
  and JWT validation works for login / refresh / password change
  and the monolith's auth endpoints are deprecated (return 307 redirect)

Given the gene-service is extracted
When it starts independently
Then it handles GET /api/genes, POST /api/genes/search, GET /api/genes/{accession}
  and uses the Routing DataSource (read-only queries → replica)
  and the monolith's gene endpoints are disabled
```

### AC-3 — Read Replica Routing

```
Given the analytics-service performs a dashboard KPI query
When the request is received
Then the Routing DataSource directs the connection to the read replica
  and zero queries hit the primary PostgreSQL node

Given the import-service writes a batch of proteins
When the Spring Batch chunk commits
Then the connection uses the primary PostgreSQL node
  and the read replica receives changes via streaming replication
```

### AC-4 — Circuit Breaker Protection

```
Given the gene-service is down
When the frontend requests /api/genes via the Gateway
Then the Gateway circuit breaker opens after 5 failed attempts
  and subsequent requests receive HTTP 503 with Retry-After: 30
  and the Gateway logs the failure for observability

Given the nlq-service's Gemini API call times out
When a natural language query is submitted
Then Resilience4j fallback returns HTTP 503
  and the user sees: "AI assistant is temporarily unavailable."
```

### AC-5 — Event-Driven Communication

```
Given an import job completes successfully
When the import-service publishes ProteinImportedEvent
Then the analytics-service consumes it and refreshes materialized views
  and the notification-service sends an email to the admin
  and the audit-service records the event
  all within 30 seconds of event publication
```

### AC-6 — Centralized Configuration

```
Given the operator updates gene-service.yml in the config repository
When the webhook triggers /actuator/refresh on gene-service instances
Then the new configuration is applied without restart
  and a log entry confirms the refresh

Given a gene-service instance starts in the 'prod' profile
When it contacts the Config Server
Then it receives gene-service.yml merged with gene-service-prod.yml
  and secrets are decrypted from {cipher} entries
```

### AC-7 — Service-to-Service Authentication

```
Given the export-service calls the gene-service to fetch protein data
When the request leaves the export-service
Then it includes an internal JWT in the X-Service-Token header
  and the gene-service validates the token against the auth-service
  and rejects the request if the token is invalid or expired
```

### AC-8 — Monolith Drainage

```
Given all services are extracted and operational
When the monolith's /api/genes endpoint is called
Then it returns HTTP 410 Gone with a message: "Use /api/genes on gene-service"

Given 30 days have passed since full extraction
When the monolith deployment is reviewed
Then it contains no business logic — only reverse-proxy configuration
  and a plan exists to decommission the monolith artifact
```

### AC-9 — Frontend Compatibility

```
Given the Angular app is configured for microservices
When the user browses to /genes
Then requests are routed through the API Gateway
  and the auth interceptor still attaches the Bearer token
  and feature modules load service-specific base URLs from environment.ts
  and the user experience is unchanged from the monolith version
```

### AC-10 — Observability

```
Given a request flows through Gateway → gene-service → auth-service
When it completes
Then distributed trace IDs are propagated across all hops
  and each service emits metrics (Micrometer) for request count, latency, errors
  and logs include the trace ID for correlation
```

---

## Key Design Decisions

### Strangler Fig Implementation

1. **Phase 0:** Bootstrap infrastructure (Gateway, Eureka, Config, Kafka, DB replicas).
2. **Phase 1:** Extract `auth-service` (lowest dependency, highest security isolation).
3. **Phase 2:** Extract `analytics-service` (read-only, safe to drain first).
4. **Phase 3:** Extract `import-service` (batch jobs are self-contained).
5. **Phase 4:** Extract `gene-service` (core domain, highest traffic).
6. **Phase 5:** Extract `export-service` (PIPE-001, depends on gene-service).
7. **Phase 6:** Extract `structure-service` (STRUCT-001, orthogonal to core).
8. **Phase 7:** Extract `nlq-service` (NLQ-001, depends on gene-service + LLM).
9. **Phase 8:** Extract `notification-service` and `audit-service` (cross-cutting).
10. **Phase 9:** Frontend adaptation and monolith drainage.

### Database-per-Service (Deferred)

- **Stage 1:** Shared PostgreSQL with schema separation (`auth`, `gene`, `analytics`, `import`, `export`).
- **Stage 2:** Physical split using CDC (Debezium) + per-service databases.
- Rationale: Avoid distributed transaction complexity until service boundaries are validated.

### API Versioning

- All service APIs are versioned via URL path: `/api/v1/genes`, `/api/v1/analytics/dashboard-kpis`.
- Gateway routing predicates include version: `Path=/api/v1/genes/**` → `gene-service`.
- Future versions (`/api/v2/**`) can route to new service instances without breaking clients.

---

## References

- `documentation/api-contract.md` — REST contracts preserved across service boundaries.
- `documentation/domain-model.md` — Schema separation strategy per bounded context.
- `documentation/implementation/REFACTOR-001/overview.md` — Pluggable provider architecture (inspiration for service
  boundaries).
- `documentation/implementation/STRUCT-001/overview.md` — STRUCT-001 scope (structure-service).
- `documentation/implementation/NLQ-001/overview.md` — NLQ-001 scope (nlq-service).
- `documentation/implementation/PIPE-001/overview.md` — PIPE-001 scope (export-service).
- `documentation/implementation/PERF-001/overview.md` — Read replica and batch performance patterns.
- Spring Cloud Gateway: https://docs.spring.io/spring-cloud-gateway/reference/
- Spring Cloud Netflix Eureka: https://docs.spring.io/spring-cloud-netflix/reference/
- Resilience4j: https://resilience4j.readme.io/
- Strangler Fig Pattern: https://martinfowler.com/bliki/StranglerFigApplication.html

---

**Ticket Created**: 2026-08-12  
**Target Release**: Phase 10 (full drainage) — estimated 16–20 weeks  
**Estimated Effort**: XL (infrastructure + 10 service extractions)
