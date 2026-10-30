# common-starter

Shared Spring Boot starter for the Bioinformatics microservices ecosystem.

## Quick Start

### 1. Install locally

```bash
# From the repository root
mvn -f ./backend/pom.xml -pl libs/common-starter -am clean install
```

### 2. Add dependency to a service

```xml

<dependency>
    <groupId>com.bioinformatics</groupId>
    <artifactId>common-starter</artifactId>
</dependency>
```

The backend parent manages the versions of `common-starter` and `shared-models`.
Consumers outside this Maven reactor must specify the published version.

### Dependency ownership

The starter provides MVC, WebClient/RestClient, validation, OAuth2 resource-server
security, JPA/PostgreSQL, Flyway, Kafka, Redis caching, resilience/retry, AOP, rate limiting,
and Actuator/Brave/Zipkin tracing. Backend services do not redeclare these shared
starters. WebFlux supports the shared WebClient; it does not replace the services'
servlet web stack.

The routing datasource uses Flyway's datasource annotation, so Flyway stays in
this starter. Services declare their runtime PostgreSQL migration module.
Only import/export services include JDBC Batch and OpenFeign; only auth-service
and api-gateway include JJWT. The gateway uses `shared-models`, not this servlet
starter. OAuth2 client login and MVC Gateway are not part of the shared starter.

Test dependencies remain local to each module: use JPA slices, MockMvc, Batch
utilities, embedded Kafka, or Testcontainers only where tests need them.
Testcontainers PostgreSQL is also required by `jdbc:tc:` test URLs, even without
container imports. The parent imports the Testcontainers 1.x BOM for the existing
APIs and centrally manages library versions, including transitive Commons
Compress; per-module exclusions and replacement declarations are unnecessary.
The export writers explicitly declare the Jackson 2 APIs they still use rather
than obtaining them indirectly through JJWT. Do not narrow Jackson 2 to test
scope: the existing Spring Cloud runtime dependencies still require it.

### 3. Minimal `application.yml` per service

```yaml
spring:
  application:
    name: gene-service

common:
  jwt:
    secret: ${JWT_SECRET}          # from Config Server / Vault
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    consumer:
      group-id: ${spring.application.name}
  datasource:
    primary-url: ${DB_PRIMARY_URL}
    primary-username: ${DB_PRIMARY_USER}
    primary-password: ${DB_PRIMARY_PASS}
    replica-url: ${DB_REPLICA_URL}
    replica-username: ${DB_REPLICA_USER}
    replica-password: ${DB_REPLICA_PASS}
```

## Provided Beans

| Bean                                      | Condition                               | Description                                              |
|-------------------------------------------|-----------------------------------------|----------------------------------------------------------|
| `JwtDecoder`                              | `common.jwt.secret` present             | HS256 decoder for Bearer tokens                          |
| `JwtContext`                              | always                                  | Helper to read username / roles from current token       |
| `SecurityFilterChain`                     | `JwtDecoder` present                    | Stateless JWT security, permits health                   |
| `CircuitBreakerRegistry`                  | Resilience4j on CP                      | Default CB settings                                      |
| `RetryRegistry`                           | Resilience4j on CP                      | Default retry settings                                   |
| `RateLimiterRegistry`                     | Resilience4j on CP                      | Default rate-limiter settings                            |
| `DataSource` (routing)                    | `common.datasource.primary-url` present | PRIMARY / REPLICA routing via `@Transactional(readOnly)` |
| `KafkaTemplate<String,Object>`            | Kafka on CP                             | JSON producer with type headers                          |
| `ConcurrentKafkaListenerContainerFactory` | Kafka on CP                             | JSON consumer with error handling                        |
| `Tracing`                                 | Brave on CP                             | B3 propagation → Zipkin                                  |
| `WebClient.Builder`                       | WebFlux on CP                           | Plain + `@LoadBalanced` builders                         |
| `CommonGlobalExceptionHandler`            | always                                  | RFC 7807 `ProblemDetail` responses                       |

## Overriding in a Service

Any bean marked `@ConditionalOnMissingBean` can be replaced by defining your own:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    // custom rules …
}
```

## Migration Notes from Monolith

- **JWT generation** stays in the `auth-service` (monolith or new service).  
  Other services only *decode* via `JwtDecoder`.
- **Custom `JwtUtil`** from the monolith is replaced by Spring Security OAuth2  
  resource-server machinery.  `JwtContext` offers the same convenience methods.
- **BCrypt password encoder** is not included here — it belongs to the auth service.
- **Redis cache config** is service-specific; keep it in each service that needs caching.

## Publishing audit events

Set `spring.application.name` and `common.kafka.bootstrap-servers` to enable the
shared audit publisher, aspect, and servlet request-context filter. No audit
database access is needed in producer services.

```java
import com.bioinformatics.common.audit.annotation.Auditable;
import com.bioinformatics.shared.models.audit.AuditAction;

@Auditable(action = AuditAction.FILTER_SAVE, targetId = "#result.id")
public SavedFilterDto saveFilter(SavedFilterCreateRequest request) {
    // ...
}
```

Annotate public methods on Spring-managed beans. `targetId` supports method
arguments and `#result` (including unwrapped `ResponseEntity` bodies).
`auditOnlyOnFailure` and `skip` retain their existing behavior. By default,
the target comes from the action; set `target` to override it explicitly.
The `DEFAULT` marker is resolved before publishing and is never sent as a target.
Code without an annotation can inject `AuditEventPublisher` and call
`publish(action, target, targetId, status)`.

The publisher snapshots the caller's authenticated identity, HTTP method,
endpoint, IP address, and `X-Correlation-ID` before queueing work. If no incoming
correlation ID exists, it uses the tracing MDC `traceId`, or generates an ID.
Each event carries its own UUID, source application name, and creation time.
HTTP method and endpoint are sent as key/value metadata compatible with
`audit-service`. Background operations use `SYSTEM` / `INTERNAL` request metadata.
For annotated methods, authentication is captured before invocation so logout
and password changes retain the initiating actor even when the service clears
the security context. The aspect runs before standard method-security advice
and records denials of annotated methods. Thrown exceptions and returned
`ResponseEntity` error statuses are failures; business responses/exceptions are
preserved. Use null-safe expressions such as `#result?.id` for generated IDs
that do not exist on a failed request.

Publishing uses a bounded executor configured by the existing
`app.audit-pool.core-size`, `max-size`, `queue-capacity`, and
`thread-name-prefix` properties (defaults: 1, 10, 25, `audit-`).
Kafka metadata lookup and serialization happen off the request thread; sends
use `audit.events.log`, keyed by event UUID. Queue rejection and synchronous
or asynchronous Kafka failures are logged with the event ID without replacing
the business response or exception. This is best-effort delivery, not a durable
outbox: crashes or exhausted queues can lose events.

Dashboard audit producers now use this shared package and the enums from
`shared-models`; CSV exports use the shared `EXPORT_PIPELINE` target.
Dashboard-local audit persistence/search classes have been removed.
`audit-service` alone persists and searches new audit events. Historical Flyway
migrations and old audit rows are left intact; migrating/archive of existing
rows must be handled separately before removing the legacy table. Request throttling
also uses the starter's independent `com.bioinformatics.common.ratelimit` package.

### Controller audit policy

Audit business/security events, not every HTTP request. The current controller
coverage is:

| Endpoint category | Policy | Reason / recorded target |
|-------------------|--------|--------------------------|
| Auth login | Success and failure | Account access; attempted username only, never password or issued tokens |
| Auth refresh | Failure only | Invalid/replayed credential attempts are useful; routine successful renewal is noise; never record the refresh token |
| Logout, password change, service-token issuance | Success and failure | Session/credential lifecycle and privileged issuance; affected user ID |
| File/remote import start | Success and failure | Privileged catalog mutation; generated import job ID when available |
| Export creation, retry, deletion | Success and failure | Data extraction and resource lifecycle; created or affected pipeline ID |
| Export download URL / file | Success and failure | Granting or attempting data access; pipeline ID only, never signed URLs, paths or file contents |
| Export detail read | Failure only | Failed ownership/access attempts without auditing routine successful navigation |
| Explicit analytics comparison | Success and failure | User-initiated analysis; no filter payload or result data |
| Admin audit-history search | Success and failure | Access to security records; no query body or returned records |
| Existing gene search/detail and saved-filter create/load/delete | Success and failure | Preserve user actions; saved-filter listing and gene count are not save/export actions |
| Autocomplete, chart/KPI fan-out, import/export listing and status polling, export field metadata | Not annotated | High-frequency read-only UI/support traffic with little independent audit value |

Auth and export Config Server files now include `common.kafka.bootstrap-servers`;
deploy those config changes with the controller annotations so the publisher is
enabled. Import, analytics, audit and dashboard already configure it.

An accepted import/export request is not proof of batch completion, and a
download response is not proof that the client received the entire stream.
Login requests have no authenticated actor by default: their username is an
attempted account target, not a verified principal. Refresh failures do not
decode an untrusted token solely to identify a user. Controller advice does not
observe authentication/authorization failures in the HTTP filter chain or
binding/validation failures that prevent controller invocation; those require
separate security/HTTP telemetry. Audit-history searches emit one access event,
not events for every returned row; Kafka persistence itself is not annotated,
so consuming an audit event does not recursively generate another.

## Servlet API rate limiting

Import `com.bioinformatics.common.ratelimit.annotation.RateLimited` on controller
methods. `@RateLimited(key = "search")` selects a named policy;
`@RateLimited` selects `global`. The starter discovers `RateLimitConfig` through
`CommonAutoConfiguration` and binds `app.rate-limiter.*` independently of each
service's application properties. It activates only in servlet applications,
defaults to enabled, and can be disabled with `app.rate-limiter.enabled=false`.
Disabling `common.enabled` also disables the starter and its limiter.

`global` defaults to capacity 100, refill tokens 100, period 60 seconds. Named
policies are configured in the `endpoints` list with `name`, `capacity`, `tokens`
and `seconds`; see `application-example.yml`. Capacity is the initial burst;
tokens refill greedily over the period, not at fixed window boundaries. Blank
names, nonpositive values, duplicate names and a renamed `global` policy fail
configuration binding. An annotation referring to a missing policy fails
explicitly rather than silently bypassing throttling or using another budget.

Each policy shares one bucket across its annotated methods **per client IP per
service instance**. `global` is a default policy, not an additional service-wide
ceiling. Exhaustion throws the existing `RateLimitExceededException`, mapped by
`GlobalExceptionHandler` to the standard HTTP 429 error body. Failed controller
calls consume tokens too. Audit advice runs outside rate-limit advice so an
audited, throttled invocation can still be recorded as a failure.

The limiter uses the servlet request's `getRemoteAddr()`; it does not trust raw
`X-Forwarded-For` headers. Behind a proxy, configure trusted-proxy address handling
at the deployment boundary or clients may share the proxy's budget. Buckets are
in-memory, retained for the instance lifetime and reset on restart; replicas do
not share them. Gateway Redis-backed rate limits remain a separate layer and
are not replaced by this servlet aspect. This advice does not protect actuator
endpoints, filter-chain failures or requests rejected before controller invocation.

The active API policy defaults and override variables are documented in
[`backend-config/README.md`](../../../backend-config/README.md#servlet-api-rate-limits).
