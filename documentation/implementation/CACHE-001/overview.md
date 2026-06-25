# CACHE-001 — Distributed Caching with Batch Eviction Hooks

## Description

Introduce a high-performance distributed caching architecture using **Redis** to intercept read-heavy endpoints and
protect the database from high-concurrency connection saturation.
To ensure strict data integrity across administrative changes, implement automated cache eviction hooks synchronized
directly into the post-import lifecycle task of the Spring Batch pipeline. When a file ingestion job moves to
`COMPLETED`, the system will systematically purge the stale cache states alongside the concurrent materialized view
refreshes.

Key Target Targets:

- `GET /api/genes/{id}` (Protein Details)
- `/api/analytics/*` (Pre-computed dashboard endpoints)

## Scope

| Layer / Module      | Artifact                                    | Description                                                                                                           |
|:--------------------|:--------------------------------------------|:----------------------------------------------------------------------------------------------------------------------|
| **Infrastructure**  | `docker-compose.yml`, `.env`                | Introduce a dedicated Redis container instance tied into the internal network.                                        |
| **Config**          | `CacheConfig.java`                          | Spring Cache implementation using `RedisCacheManager`. Configure multi-bucket TTL strategies.                         |
| **Backend Service** | `GeneService.java`, `AnalyticsService.java` | Inject declarative caching semantics (`@Cacheable`) on performance-critical bottlenecks.                              |
| **Batch Pipeline**  | `PostImportCacheEvictionListener.java`      | Custom Spring Batch `JobExecutionListener` to programmatically evict keys upon successful batch lifecycle completion. |
| **Testing**         | `CacheIntegrationTests.java`                | Spring Boot integration test suite tracking cache hits, cache misses, and post-import purges.                         |

## Acceptance Criteria

- [ ] **Infrastructure Readiness**: Running `docker compose up` provisions a healthy, authenticated Redis instance
  accessible to the Spring Boot backend environment.
- [ ] **Read Cache Interception**: A call to `GET /api/genes/{id}` queries the PostgreSQL database exactly once.
  Subsequent identical lookups return payloads straight from Redis in $\le 10\text{ ms}$, executing 0 SQL assertions.
- [ ] **Analytics Cache Isolation**: All six analytics view paths cache their pre-computed data transfer objects
  independently into an `analytics` cache bucket.
- [ ] **Lifecycle-Bound Coherence**: Initiating a UniProt file import via `/api/admin/import/uniprot` leaves existing
  caches active during execution. The precise millisecond the Spring Batch job reaches `COMPLETED`, the cache eviction
  hook triggers and wipes both `genes` and `analytics` cache regions.
- [ ] **Error Path Isolation**: If a Spring Batch job transitions to `FAILED`, the eviction listener skips processing,
  retaining operational read caches for unaffected baseline records.
- [ ] **Coverage Metric Compliance**: All introduced caching infrastructure, listener components, and service
  configurations must clear a strict $\ge 80\%$ unit/integration testing coverage sweep.