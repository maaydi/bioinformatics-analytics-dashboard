# Implementation Plan — CACHE-001

## Architectural Overview

To preserve decoupling and adhere cleanly to SOLID principles, caching is introduced via Spring’s AOP infrastructure
alongside a localized Redis abstraction provider. Eviction is isolated from the core domain processing logic by tying a
specialized infrastructure component into the existing Spring Batch asynchronous task lifecycle.

```text
[Client REST Request] -> [Spring Cache AOP Proxy] --(Hit)--> [Redis Store]
                               |
                            (Miss)
                               v
                     [PostgreSQL Database]

[Spring Batch Completion] -> [PostImportCacheEvictionListener] -> Evicts [Redis Store]

```

---

## Breakdown of Tasks

### Phase 1: Infrastructure & Configuration Setups

#### [x] Task 1.1: Provision Redis Infrastructure Container

* **Actions**:
* Update root-level `docker-compose.yml` to bundle a standard `redis:7.2-alpine` service.
* Expose default port `6379` internally. Secure configuration with a `requirepass` parameter pulled from a consolidated
  `.env` file variable (`REDIS_PASSWORD`).


* **Verification**: Run `docker compose up -d redis` and ensure container remains stable without error loops.

#### [ ] Task 1.2: Establish Project Dependencies & Spring Properties

* **Actions**:
* Append `org.springframework.boot:spring-boot-starter-data-redis` inside backend `pom.xml`.
* Wire infrastructure configurations inside `application.yml`:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
  cache:
    type: redis

```

#### [ ] Task 1.3: Author Core `CacheConfig.java` Engine

* **Actions**:
* Create package `com.bioinformatics.dashboard.config.cache`.
* Annotate with `@EnableCaching` and define a customized `RedisCacheManager` Bean configuration.
* Segregate caches into isolated namespaces with varied TTL constraints:
* `"genes"` cache bucket: TTL = 12 Hours (Serializer: `GenericJackson2JsonRedisSerializer`).
* `"analytics"` cache bucket: TTL = 24 Hours (Clears explicitly via operations).


* **Verification**: Boot application context locally to verify the caching manager correctly establishes Redis
  connectivity.

---

### Phase 2: Service Layer Caching Adoptions

#### [ ] Task 2.1: Implement Declarative Caching for Gene Queries

* **Actions**:
* Open `GeneService.java` and supplement `getGeneById(Long id)` with an explicit
  `@Cacheable(value = "genes", key = "#id", unless = "#result == null")` declaration.


* **Verification**: Enable SQL statement trace logs. Request an ID twice; verify the console outputs a `SELECT`
  statement precisely once.

#### [ ] Task 2.2: Implement Caching across Analytics Subsystems

* **Actions**:
* Open `AnalyticsService.java`.
* Annotate operational metric retrieval methods with `@Cacheable(value = "analytics", key = "#root.methodName")`.
  Targets include dashboard KPIs, histogram buckets, and organism metrics.


* **Verification**: Validate quick sub-millisecond retrieval responses on consecutive operations.

---

### Phase 3: Spring Batch Orchestrated Eviction Hooks

#### [ ] Task 3.1: Construct `PostImportCacheEvictionListener` Listener

* **Actions**:
* Create `PostImportCacheEvictionListener.java` implementing Spring Batch's `JobExecutionListener`.
* Override `afterJob(JobExecution jobExecution)` method hook:

```java
if(jobExecution.getStatus() ==BatchStatus.COMPLETED){
        log.

info("Batch Ingestion completed successfully. Commencing global cache purge across regions...");
    cacheManager.

getCache("genes").

clear();
    cacheManager.

getCache("analytics").

clear();
}

```

* **Verification**: Inject listener into the primary batch job definition configuration file.

---

### Phase 4: Verification, Quality Assurance, and Coverage

#### [ ] Task 4.1: Write Comprehensive Integration Test Harnesses

* **Actions**:
* Implement `CacheIntegrationTests.java` leveraging an embedded Redis test container or mock utility.
* Assert lookups populate the underlying Redis structures cleanly.
* Programmatically simulate a `COMPLETED` batch context status outcome and verify Redis target keys are completely empty
  post-execution.


* **Verification**: Execute `mvn clean test` verifying all test suites validate cleanly.

#### [ ] Task 4.2: Audit Final Quality Matrix Indicators

* **Actions**:
* Run local JaCoCo test metric analyzer audits to check coverage thresholds.
* Complete the tracking parameters inside `documentation/implementation/CACHE-001/journal.md`.
