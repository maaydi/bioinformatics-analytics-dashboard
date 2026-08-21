# common-starter

Shared Spring Boot starter for the Bioinformatics microservices ecosystem.

## Quick Start

### 1. Install locally

```bash
cd libs/common-starter
./mvnw clean install
```

### 2. Add dependency to a service

```xml

<dependency>
    <groupId>com.bioinformatics</groupId>
    <artifactId>common-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

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
