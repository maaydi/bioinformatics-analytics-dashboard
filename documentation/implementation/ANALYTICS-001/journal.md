# ANALYTICS-001 — Implementation Journal

---

## 2026-05-12

### Ticket created

- Created `overview.md` and `plan.md` from backlog stories US-11, US-12, US-13, US-14, US-32, US-33, US-34, US-35.
- Reviewed existing backend: `AnalyticsController.java` and `AnalyticsService.java` exist as stubs — all methods throw
  `UnsupportedOperationException`.
- No DTOs found in `analytics` package; no repository found.
- DB migration for materialized views is absent — needs `V4__materialized_views.sql`.
- Compare endpoint (`POST /api/analytics/compare`) is not defined in the controller yet.
- Implementation not yet started.

## 2026-05-28

### Initial analytics implementation

- `9c359e7` — Added analytics DTOs (`DashboardKpisDto`, `LengthHistogramBucketDto`, `OrganismCountDto`,
  `ReviewedRatioDto`, `EvidenceDistributionDto`, `KeywordFrequencyDto`).
- `310223c` — Implemented `AnalyticsController` endpoints for dashboard KPIs and chart APIs.
- `371c9f7` — Updated JavaDoc references to project markdown specs.
- `a07c763` — Implemented dashboard KPIs repository/service flow.
- `4a81e4f` — Implemented length histogram repository/service flow.

## 2026-05-29

### Finalization of analytics endpoints

- `8b40602` — Implemented `by-organism` query path.
- `102ea91` — Implemented `reviewed-ratio` query path.
- `f1a369b` — Implemented `evidence-levels` query path.
- `7e01255` — Implemented `keyword-frequency` query path.

## 2026-05-30

### Materialized views refresh integration

- `118051a` — Refactored import job with skip-limit and additional listeners.
- `a740090` — Updated analytics view/entity mapping for refresh flow.
- `1137c2f` — Added `MaterializedViewRefreshService` and integrated refresh listener into import job lifecycle.

### Post-review hardening

- `1a4e5d6` — Added explicit ordering and limit alignment for analytics ranking queries.
- `bb71254` — Added refresh execution logs to persist per-job materialized view refresh outcomes.
- `a5e37ef` — Added timeout, retry, and failure alerting path for async view refresh.
- `7a9aeed` — Fixed repository key types and finalized query ordering corrections.

---

## 2026-05-30 — Code Review

### Critical Issues (Resolved)

1. **Organism ranking query ordering**
  - Done: Added `ORDER BY total DESC` for top-organism ranking (`1a4e5d6`, `7a9aeed`).

2. **Keyword ranking query ordering**
  - Done: Added `ORDER BY count DESC` for keyword frequency ranking (`1a4e5d6`, `7a9aeed`).

3. **Evidence distribution repository ID mismatch**
  - Done: Aligned repository ID typing with entity key type (`7a9aeed`).

4. **Organism repository composite key mismatch**
  - Done: Updated repository generic ID to composite key type (`7a9aeed`).

### Medium Issues (Resolved)

5. **V12 migration unreachable index logic**
  - Done: Corrected migration logic to remove unreachable/non-existent column path.

6. **Async view refresh operational SLA gaps**
  - Done: Added timeout, retry, and failure alerting/logging (`a5e37ef`, `bb71254`).

### Missing test coverage completed

- Added `AnalyticsServiceTest` with focused unit coverage for:
  - KPI not-found behavior (`ResourceNotFoundException`)
  - KPI/histogram mapping flows
  - repository delegation with `Limit` for organism and keyword ranking queries
- Added `AnalyticsControllerIntegrationTest` with endpoint and validation coverage for:
  - `GET /api/analytics/dashboard-kpis`
  - `GET /api/analytics/length-histogram`
  - `GET /api/analytics/by-organism`
  - `GET /api/analytics/reviewed-ratio`
  - `GET /api/analytics/evidence-levels`
  - `GET /api/analytics/keyword-frequency`
  - invalid limits (`by-organism?limit=201`, `keyword-frequency?limit=501`) returning `400`
- Test run executed:
  - `mvn -Dtest=AnalyticsServiceTest,AnalyticsControllerIntegrationTest test`
  - Result: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)

### JaCoCo coverage setup and report

- Added JaCoCo plugin configuration in `backend/pom.xml` (`org.jacoco:jacoco-maven-plugin:0.8.13`) with `prepare-agent`
  and `report` executions.
- Coverage run executed:
  - `mvn -Dtest=AnalyticsServiceTest,AnalyticsControllerIntegrationTest test jacoco:report`
  - Result: `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- Report artifacts generated under:
  - `backend/target/site/jacoco/index.html`
  - `backend/target/site/jacoco/jacoco.csv`
- Measured line coverage from `jacoco.csv`:
  - `analytics-package` (all analytics classes incl. generated mapper impl + DTOs): `27.66%` (`39/141`)
  - `analytics-core` (`AnalyticsService` + `AnalyticsController`): `77.14%` (`27/35`)
- **Blocker noted:** still below the required `>= 80%` threshold for the analytics core scope.

### Additional coverage hardening (attempt to reach backend 80%)

- Added targeted tests:
  - `backend/src/test/java/com/bioinformatics/dashboard/analytics/mapper/AnalyticsMappersTest.java`
  - `backend/src/test/java/com/bioinformatics/dashboard/batch/service/ViewRefreshAlertServiceTest.java`
  - Expanded `backend/src/test/java/com/bioinformatics/dashboard/batch/service/MaterializedViewRefreshServiceTest.java`
- Full backend execution with coverage:
  - `mvn test jacoco:report`
  - Result: `Tests run: 107, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- Updated coverage metrics from `backend/target/site/jacoco/jacoco.csv`:
  - backend overall line coverage: `65.40%` (`1032/1578`)
  - analytics package line coverage: `90.07%` (`127/141`)
  - view-refresh services line coverage: `90.24%` (`74/82`)
- Status: backend global threshold `>= 80%` is **not yet reached**; analytics scope now exceeds 80%.


