# Bioinformatics Analytics Dashboard — Implementation Changelog

> Comprehensive chronological log of project evolution, architectural milestones, and feature delivery from repository
> inception (`2026-04-28`) through current HEAD (`2026-10-23`).

## Executive Summary

- **Total Commits Analyzed:** 690 commits across 27 distinct implementation phases & tickets.
- **Timeline:** April 28, 2026 – October 23, 2026 (~6 months of active engineering).
- **Core Technologies:** Java 25, Spring Boot 4, Spring Batch, Spring Cloud (Gateway, Eureka, Config), PostgreSQL 16
  (Primary + Read Replica), Redis, RabbitMQ, Docker & Kubernetes (Minikube), Angular 19/20, Signals Store, AG Grid
  Enterprise, Chart.js / ECharts, Tailwind / SCSS Design System.
- **Architectural Trajectory:**
    1. **Monolith Inception & Core Biological Feature Suite:** Secure JWT authentication, UniProt XML/GZ bulk ingestion,
       dynamic JPA specification querying, AG Grid table exploration, interactive analytics dashboards, dual-cohort
       comparison, and distributed Redis caching.
    2. **Pluggable Multi-Data Provider Layer:** Abstracted database access to enable seamless switching between the
       local high-performance PostgreSQL cache and the live global UniProtKB REST API with cursor-based streaming.
    3. **Microservices Migration (Strangler Fig Pattern):** Decomposed monolith into multi-module services
       (`auth-service`, `analytics-service`, `import-service`, `export-service`), coordinated via Spring Cloud Gateway,
       Eureka Discovery, Config Server, shared common starters, and primary-replica database replication.
    4. **Visual Batch Export Pipeline & UX Maturation:** Asynchronous multi-format export pipelines (CSV, JSON, FASTA,
       TSV), job tracking monitors, Angular Signals state store refactorings, FHD screen layout enhancements, and
       resilient remote error recovery.

---

## Milestone & Ticket Progress Matrix

| Ticket / Phase   | Scope & Feature Focus                                                         | Dates                      | Commits |     Hash Range     |         Pull Request         |    Status     |
|:-----------------|:------------------------------------------------------------------------------|:---------------------------|:-------:|:------------------:|:----------------------------:|:-------------:|
| `INIT`           | Project Inception & Core Modernization (Spring Boot 4, Java 25, Angular)      | [2026-04-28 to 2026-04-30] |   16    | `c499ea2..5bcf57b` |              #1              |    `Done`     |
| `AUTH-001`       | Authentication & Role-Based Access Control Baseline                           | [2026-04-30 to 2026-05-12] |   17    | `486527f..6ea3f61` |              #2              |    `Done`     |
| `IMPORT-001`     | UniProt Batch Data Ingestion Pipeline & Admin Console                         | [2026-05-01 to 2026-05-12] |   36    | `9051922..872ba91` |              #3              |    `Done`     |
| `GENE-001`       | Core Protein Catalog API, JPA Specifications & CSV Writer                     | [2026-05-13 to 2026-05-17] |   38    | `e7f1eab..7130d2e` |          #4, #5, #6          |    `Done`     |
| `PERF-001`       | Database & ORM Performance Tuning for 570k UniProt Dataset                    | [2026-05-19 to 2026-05-19] |   15    | `18760b2..077b82e` |              #7              |    `Done`     |
| `GENE-002`       | Advanced Filter Sidebar & Angular Signals Filter Store                        | [2026-05-21 to 2026-05-23] |   29    | `220bfbd..c86c1ad` |              #8              |    `Done`     |
| `GENE-003`       | Interactive AG Grid Protein Explorer Table & Global Search                    | [2026-05-23 to 2026-05-26] |   20    | `fdb8ebf..67e7ffb` |              #9              |    `Done`     |
| `DETAIL-001`     | Comprehensive Protein Detail View & Tabbed Workspace                          | [2026-05-26 to 2026-05-26] |   20    | `c0602bd..306e154` |             #10              |    `Done`     |
| `ANALYTICS-001`  | Analytics Engine, PostgreSQL Materialized Views & Aggregate Metrics           | [2026-05-28 to 2026-05-30] |   25    | `9c359e7..d30d646` |             #11              |    `Done`     |
| `DASH-001`       | Interactive Analytics Dashboard UI & Dynamic Charts                           | [2026-05-31 to 2026-05-31] |   25    | `e753775..f7903c2` |             #12              |    `Done`     |
| `FILTER-001`     | Saved Filters & User Preset Management                                        | [2026-06-02 to 2026-06-05] |   18    | `f8dccfe..aa4c4ad` |             #13              |    `Done`     |
| `EXPORT-001`     | Streaming CSV & Chart Visualization Image Export                              | [2026-06-07 to 2026-06-12] |   34    | `a964e98..c3a5615` |             #14              |    `Done`     |
| `COMPARE-001`    | Dual-Cohort Comparison Analytics & Delta Metrics                              | [2026-06-12 to 2026-06-18] |   18    | `9a9d13f..599e58a` |             #15              |    `Done`     |
| `OPS-001`        | Production Observability, Audit Logging & Rate Limiting                       | [2026-06-19 to 2026-06-25] |   19    | `b00a426..bdd6e3b` |             #16              |    `Done`     |
| `CACHE-001`      | Multi-Tier Distributed Caching (Redis/Caffeine) & Eviction Hooks              | [2026-06-25 to 2026-06-25] |   19    | `86ad2b3..765c893` |             #17              |    `Done`     |
| `A11Y-001`       | Accessibility (WCAG 2.1 AA), Theme Toggle & UI State Hosts                    | [2026-06-25 to 2026-06-29] |   30    | `4e5c6a0..bfed51d` |             #18              |    `Done`     |
| `BUILD-001`      | JaCoCo Code Coverage & CI/CD Pipeline Stabilization                           | [2026-06-30 to 2026-06-30] |    8    | `e4b8de1..c8c7e5f` |             #19              |    `Done`     |
| `REFACTOR-001`   | Pluggable Data Provider Architecture & Provider Dispatchers                   | [2026-07-02 to 2026-07-03] |    8    | `715c2e2..a612fc0` |             #20              |    `Done`     |
| `RDF-001`        | UniProt KnowledgeBase Remote API Client & Facet Models                        | [2026-07-03 to 2026-07-11] |   20    | `b5dec77..a33f19a` |             #21              |    `Done`     |
| `REMOTE-001`     | Live UniProtKB Remote Provider & Seamless Provider Switching                  | [2026-07-19 to 2026-08-10] |   47    | `b8e7ab6..907ac52` |             #22              |    `Done`     |
| `STRUCT-001`     | STRUCT-001                                                                    | [2026-08-12 to 2026-08-12] |    2    | `c8d5f82..1f21216` |              —               |   `Planned`   |
| `NLQ-001`        | NLQ-001                                                                       | [2026-08-12 to 2026-08-12] |    1    | `4ce7573..4ce7573` |              —               |   `Planned`   |
| `PIPE-001`       | Visual Export Pipeline, Batch Export Engine & Modern Signal Store Refactoring | [2026-08-12 to 2026-10-23] |   109   | `a9f9440..b2b2b11` | #27 (Backend) & Ongoing HEAD | `In-Progress` |
| `ARCH-001`       | Microservices Migration via Strangler Fig Pattern                             | [2026-08-12 to 2026-09-06] |   107   | `1a7a841..868aa74` |             #24              |    `Done`     |
| `STRATEGIC-DOCS` | Strategic Architecture Backlog (STRUCT-001, NLQ-001, ARCH-002, OPS-002)       | [2026-08-12 to 2026-08-12] |    1    | `fefff3f..fefff3f` |   #23, #25, #26, #28, #29    |    `Done`     |
| `ARCH-002`       | Database-per-Service Architecture & Protein Catalog Ownership Spec            | [2026-09-29 to 2026-09-29] |    3    | `5a07336..817c1cf` |           #25, #26           |   `Planned`   |
| `OPS-002`        | Kubernetes (Minikube) Cloud-Native Migration Specification                    | [2026-09-30 to 2026-10-04] |    5    | `a7571ab..b40457f` |           #28, #29           | `In-Progress` |

---

## Detailed Chronological Progress by Phase

### Epoch I: Monolithic Foundation & Core Analytical Suite (April – June 2026)

#### [FOUNDATION] `INIT` — Project Inception & Core Modernization (Spring Boot 4, Java 25, Angular)

- **Dates Active:** [2026-04-28 to 2026-04-30]
- **Commits:** 16 commits (`c499ea2..5bcf57b`)
- **Merged PR:** #1
- **Overview:** Bootstrap of the Bioinformatics Analytics Dashboard repository. Modernized core stack to Java 25 and
  Spring Boot 4. Replaced deprecated MockBean with MockitoBean, configured MapStruct, Lombok, and initial Docker
  infrastructure. Built early authentication and web error server scaffolding.

**Backend Engineering:**

- Upgraded runtime to Java 25 and Spring Boot 4 with Jakarta EE imports.
- Configured Lombok, MapStruct code generation, and replaced deprecated `@MockBean` with `@MockitoBean`.
- Prepared PostgreSQL baseline database schemas, views, and dockerized local development environment.
- Initial implementation of backend web server, exception handling, and hot-reload support.

**Frontend & UX Engineering:**

- Initialized Angular SPA application structure with modular layout.
- Created initial login form, auth routing, and component architecture.

**DevOps & Infrastructure:**

- Dockerized backend and frontend environments.
- Created devops helper scripts (`start-dev.sh`, `build-all.sh`, `db-migrate.sh`).

**Quality Assurance & Testing:**

- Established baseline unit test suite validation with Spring Boot Test.

<details>
<summary><strong>Commit History (16 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                             |
|:---------:|:----------:|:-------------------------------------------------------------------------------------------|
| `c499ea2` | 2026-04-28 | first commit                                                                               |
| `e52d271` | 2026-04-28 | Step 3: Upgrade Spring Boot, Java, Lombok, MapStruct, Dockerfile - Compile: SUCCESS        |
| `7d904c5` | 2026-04-28 | Step 4: Final Validation - Compile: passed, Tests: 1/1 passed                              |
| `a130e34` | 2026-04-28 | change deprecated MockBean to MockitoBean                                                  |
| `e14feac` | 2026-04-28 | Merge pull request #1 from maaydi/appmod/java-upgrade-98589ebf-bfc3-4250-bffa-a1cbc1f00fda |
| `a10ff5c` | 2026-04-30 | upgrade to springboot 4                                                                    |
| `ad93598` | 2026-04-30 | revert lombok version to match jdk25                                                       |
| `30f9422` | 2026-04-30 | update spring 4 import                                                                     |
| `51630e5` | 2026-04-30 | fix web error server                                                                       |
| `21cb822` | 2026-04-30 | fix devops scripts                                                                         |
| `be110dd` | 2026-04-30 | prepare DB schemas and views                                                               |
| `70ee010` | 2026-04-30 | Add copilot instructions                                                                   |
| `35de68e` | 2026-04-30 | make backend works                                                                         |
| `e69c423` | 2026-04-30 | fix script                                                                                 |
| `38dc991` | 2026-04-30 | add instructions to copilot                                                                |
| `5bcf57b` | 2026-04-30 | refactor components                                                                        |

</details>

---

#### [SECURITY] `AUTH-001` — Authentication & Role-Based Access Control Baseline

- **Dates Active:** [2026-04-30 to 2026-05-12]
- **Commits:** 17 commits (`486527f..6ea3f61`)
- **Merged PR:** #2
- **Overview:** Delivered secure authentication foundation using stateless JWT (JSON Web Tokens) with refresh token
  support and role-based route protection (`ROLE_USER`, `ROLE_ADMIN`).

**Backend Engineering:**

- `AuthController` exposing `/api/auth/login`, `/api/auth/refresh`, and `/api/auth/logout`.
- `JwtService` and `JwtAuthenticationFilter` with role extraction and cryptographic signature verification.
- `AppUser` and `RefreshToken` entities with hashed password verification via BCrypt.
- Security exception handlers with structured error payload responses.

**Frontend & UX Engineering:**

- `AuthService` and `AuthInterceptor` attaching Bearer token to authenticated requests.
- `AuthGuard` and `AdminGuard` protecting sensitive routes and redirecting unauthenticated users.
- Login page UI with form validation, error banners, and test user credentials support.

**DevOps & Infrastructure:**

- Flyway V1 migration for users, roles, and refresh tokens tables.

**Quality Assurance & Testing:**

- Comprehensive backend unit and integration tests for authentication lifecycle, token refresh, and invalid credentials.
- Frontend Karma/Jasmine tests for `AuthService` and route guards.

<details>
<summary><strong>Commit History (17 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                            |
|:---------:|:----------:|:----------------------------------------------------------|
| `486527f` | 2026-04-30 | Add auth login frontend                                   |
| `bcc738e` | 2026-04-30 | Add two users for testing                                 |
| `5dbcb10` | 2026-04-30 | Add auth service backend                                  |
| `81e4fc8` | 2026-04-30 | add hot-reload for backend                                |
| `45fe7b4` | 2026-04-30 | remove password validation for login                      |
| `eb5d30e` | 2026-04-30 | add roles to token                                        |
| `e6b3f57` | 2026-04-30 | update spec                                               |
| `7eb48be` | 2026-04-30 | update spec                                               |
| `07e4ada` | 2026-05-01 | login issue bypass the Guard on the Server                |
| `b7ccaa9` | 2026-05-08 | AUTH-001 Add backend unit test                            |
| `79980fd` | 2026-05-08 | AUTH-001 Add frontend unit test                           |
| `f810db9` | 2026-05-09 | AUTH-001 refactor code                                    |
| `91ad92c` | 2026-05-09 | AUTH-001 add integration tests for auth                   |
| `9a317e9` | 2026-05-09 | AUTH-001 finish plan                                      |
| `d901634` | 2026-05-09 | AUTH-001 add logs to exception handlers                   |
| `77457da` | 2026-05-09 | Merge pull request #2 from maaydi/implementation/auth-001 |
| `6ea3f61` | 2026-05-12 | Update documentation implementation planning              |

</details>

---

#### [DATA INGESTION] `IMPORT-001` — UniProt Batch Data Ingestion Pipeline & Admin Console

- **Dates Active:** [2026-05-01 to 2026-05-12]
- **Commits:** 36 commits (`9051922..872ba91`)
- **Merged PR:** #3
- **Overview:** Designed and implemented high-throughput Spring Batch pipeline for parsing and ingesting UniProt XML/GZ
  data into PostgreSQL, with asynchronous execution tracking and admin management UI.

**Backend Engineering:**

- Spring Batch Job configuration with chunk-based processing (`UniProtXmlReader`, `ProteinProcessor`, `ProteinWriter`).
- Support for compressed `.xml.gz` streams, eliminating disk exhaustion on massive files.
- `ImportController` exposing endpoints to upload UniProt files, trigger import jobs, and poll job progress.
- `ImportJobExecution` entity and repository capturing step execution counts, read/write/skip metrics, and failure logs.

**Frontend & UX Engineering:**

- Admin Import page with drag-and-drop file upload, size verification, and format checking.
- Real-time job execution status polling with visual progress bars and step execution summaries.
- `ImportAdminService` handling multipart uploads and status streaming.

**DevOps & Infrastructure:**

- Flyway database migrations for Spring Batch meta-tables and protein staging structures.

**Quality Assurance & Testing:**

- Unit and integration tests for XML parsing, protein mapping, transactional rollback on error, and admin endpoints.

<details>
<summary><strong>Commit History (36 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                       |
|:---------:|:----------:|:---------------------------------------------------------------------|
| `9051922` | 2026-05-01 | implement in ticket IMPORT-001                                       |
| `ec71b40` | 2026-05-01 | IMPORT-001 update spec                                               |
| `4ed2c1a` | 2026-05-02 | update plan status                                                   |
| `8e0ea91` | 2026-05-02 | IMPORT-001 Service for API calls (import-admin.service.ts)           |
| `88b5435` | 2026-05-02 | IMPORT-001 Backend Controller  and DTO with validation constraint    |
| `5bb573a` | 2026-05-03 | IMPORT-001 Add synchrone batch processing for import uniprot         |
| `0841645` | 2026-05-03 | IMPORT-001 refactor job package                                      |
| `435d8d3` | 2026-05-03 | IMPORT-001 repository and mapper and async batch for import job      |
| `28faad7` | 2026-05-05 | IMPORT-001 Add async exception handler to mark job as failed         |
| `c25efbd` | 2026-05-05 | Refactor job execute function                                        |
| `1c1b0ec` | 2026-05-05 | IMPORT-001 Refactoring                                               |
| `01604c6` | 2026-05-06 | IMPORT-001 fix save import job in service                            |
| `86181f8` | 2026-05-06 | IMPORT-001 organize frontend import                                  |
| `ac7b210` | 2026-05-06 | IMPORT-001 add AppProperties and remove FileSizeValidator            |
| `523b522` | 2026-05-06 | IMPORT-001 add AppProperties for pool size                           |
| `0d88f81` | 2026-05-07 | IMPORT-001 Configure logging to file and rotations                   |
| `11067c2` | 2026-05-07 | IMPORT-001 fix config parameters                                     |
| `465c85e` | 2026-05-07 | IMPORT-001 migrate DB to support TEXT for protein instead of varchar |
| `e1d910b` | 2026-05-07 | IMPORT-001 add logs to console                                       |
| `987d74d` | 2026-05-07 | IMPORT-001 add logs to console                                       |
| `993f396` | 2026-05-07 | IMPORT-001 update frontend api                                       |
| `1e75628` | 2026-05-07 | IMPORT-001 add counter records to update total estimated file        |
| `1f0aab5` | 2026-05-07 | IMPORT-001 enable progress for data processing                       |
| `b70804e` | 2026-05-07 | IMPORT-001 synchronize history with upload status                    |
| `d3f6f40` | 2026-05-08 | IMPORT-001 restrict job import to one running                        |
| `d83cac6` | 2026-05-08 | IMPORT-001 enable pagination in frontend                             |
| `260b3cd` | 2026-05-08 | IMPORT-001 enable pagination in frontend                             |
| `e0fc2cf` | 2026-05-08 | Add github workflow                                                  |
| `d619462` | 2026-05-08 | IMPORT-001 backend unit tests                                        |
| `96a6cee` | 2026-05-08 | IMPORT-001 frontend unit tests                                       |
| `4657f29` | 2026-05-08 | IMPORT-001 finish plan                                               |
| `d31382c` | 2026-05-08 | Merge remote-tracking branch 'origin/main'                           |
| `7b727a0` | 2026-05-08 | fix github app-test workflow                                         |
| `96d9ce0` | 2026-05-12 | IMPORT-001 Add integration tests                                     |
| `915952e` | 2026-05-12 | IMPORT-001 finish plan                                               |
| `872ba91` | 2026-05-12 | Merge pull request #3 from maaydi/implementation/import-001          |

</details>

---

#### [CORE API] `GENE-001` — Core Protein Catalog API, JPA Specifications & CSV Writer

- **Dates Active:** [2026-05-13 to 2026-05-17]
- **Commits:** 38 commits (`e7f1eab..7130d2e`)
- **Merged PR:** #4, #5, #6
- **Overview:** Engineered the core protein catalog domain model and RESTful search APIs powered by dynamic JPA
  Specifications, supporting complex multi-criteria biological filtering.

**Backend Engineering:**

- Entities: `ProteinEntry`, `Gene`, `Organism`, `CrossReference`, `Feature`, and `Sequence`.
- `GeneSpecification` dynamic query builder for multi-attribute filtering (accession, gene name, organism, sequence
  length, evidence level).
- `GeneController` with `/api/genes` (paginated list/search) and `/api/genes/{id}` (full detail).
- Streaming `CsvWriter` utility for exporting query results directly to CSV format.

**Frontend & UX Engineering:**

- Initial protein models and `GeneService` API client for querying genes.

**DevOps & Infrastructure:**

- PostgreSQL indexing on accession, gene name, organism name, and length.

**Quality Assurance & Testing:**

- Extensive JUnit 5 tests for `GeneSpecification` predicate combinations, boundary conditions, and detail retrieval.

<details>
<summary><strong>Commit History (38 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                   |
|:---------:|:----------:|:-------------------------------------------------------------------------------------------------|
| `e7f1eab` | 2026-05-13 | GENE-001  Create `CrossReference` entity                                                         |
| `593e3ff` | 2026-05-13 | GENE-001  implement listGenes api                                                                |
| `4723e2b` | 2026-05-13 | GENE-001  implement GeneSpecification                                                            |
| `c6fe4e5` | 2026-05-13 | GENE-001  update plan and journal                                                                |
| `a45807f` | 2026-05-14 | GENE-001  add search genes endpoint                                                              |
| `0a6cf15` | 2026-05-14 | GENE-001  create ProteinDetailDto                                                                |
| `73a954d` | 2026-05-14 | GENE-001  add search protein by id                                                               |
| `9dff820` | 2026-05-14 | GENE-001  Refactor IMPORT-001 to use ResourceNotFoundException                                   |
| `6f12a65` | 2026-05-15 | GENE-001  Add csv writer                                                                         |
| `4711139` | 2026-05-15 | GENE-001 implement export csv for gene details                                                   |
| `d233651` | 2026-05-15 | GENE-001 add indexes                                                                             |
| `11f14d8` | 2026-05-15 | GENE-001 Code review : N+1 Query Risk in getGeneById()                                           |
| `6f3ab2f` | 2026-05-15 | GENE-001 Code review : CSV Export: Wrong Schema & Missing Row Limit                              |
| `a50faca` | 2026-05-15 | GENE-001 Code review : Missing Cross-Field Validation                                            |
| `1b87d8e` | 2026-05-15 | GENE-001 Code review : add missing fields in protein details                                     |
| `a8bd848` | 2026-05-15 | GENE-001 Code review : add getPageRequest to validate page from GeneRequest                      |
| `d225278` | 2026-05-15 | GENE-001 Code review : refactor export csv to validate maxsize                                   |
| `6c0303b` | 2026-05-15 | GENE-001 Code review : add HostOrganism                                                          |
| `e28822c` | 2026-05-15 | GENE-001 Code review : add ProteinComment                                                        |
| `eaa0d9c` | 2026-05-15 | GENE-001 Code review : add ProteinPublication                                                    |
| `ef266a6` | 2026-05-15 | GENE-001 Code review : add ProteinPublication repository                                         |
| `e07b449` | 2026-05-15 | GENE-001 Code review : fix multibag exception : split fetch in two queries and use Transactional |
| `f329f56` | 2026-05-16 | GENE-001 Code review : Fix unit test for ImportControllerIntegrationTest                         |
| `2c87f48` | 2026-05-16 | GENE-001 Add unit and integration tests                                                          |
| `6b5c079` | 2026-05-16 | GENE-001 Add unit and integration tests                                                          |
| `3b839d0` | 2026-05-16 | GENE-001 finish feature                                                                          |
| `591407c` | 2026-05-16 | Merge pull request #4 from maaydi/implementation/GENE-001                                        |
| `f957559` | 2026-05-16 | GENE-001 Implement entry item processor for the added tables                                     |
| `2d64f88` | 2026-05-16 | GENE-001 migrate gene entities to use Text instead of varchar                                    |
| `cd8c458` | 2026-05-16 | GENE-001 Fix item processor after run import                                                     |
| `ce32eec` | 2026-05-16 | GENE-001 use JpaItemWriter instead of JdbcBatchItemWriter ( need review for performance )        |
| `8aaa662` | 2026-05-17 | GENE-001 adapt resolveKeyword to support batch read/write to DB                                  |
| `34dcc64` | 2026-05-17 | GENE-001 update logs config                                                                      |
| `372faf9` | 2026-05-17 | GENE-001 add accession cache to assert uniqueness when importing uniprot                         |
| `a253374` | 2026-05-17 | GENE-001 mark incomplete job as Failed when server is restarted                                  |
| `143b438` | 2026-05-17 | Merge pull request #5 from maaydi/implementation/GENE-001                                        |
| `7234021` | 2026-05-17 | GENE-001 Documentation                                                                           |
| `7130d2e` | 2026-05-17 | Merge pull request #6 from maaydi/implementation/GENE-001                                        |

</details>

---

#### [PERFORMANCE] `PERF-001` — Database & ORM Performance Tuning for 570k UniProt Dataset

- **Dates Active:** [2026-05-19 to 2026-05-19]
- **Commits:** 15 commits (`18760b2..077b82e`)
- **Merged PR:** #7
- **Overview:** Optimized database schema, indexing strategies, and batch ingestion performance to handle the full
  Swiss-Prot UniProt catalog (~570,000 proteins) efficiently.

**Backend Engineering:**

- Migrated primary key generation strategy to PostgreSQL sequences with allocation size tuning.
- Replaced one-to-many lazy fetching bottlenecks with specialized JDBC batch insert aggregate writers.
- Introduced `ProteinEntryService` to encapsulate read operations and isolate high-load query paths.
- Optimized `ProteinEntryProcessor` memory footprint by reducing intermediate object allocations.

**DevOps & Infrastructure:**

- Database migration adding composite B-tree and trigram indexes on high-frequency search fields.
- Configured PostgreSQL memory and buffer parameters for batch bulk loading.

**Quality Assurance & Testing:**

- Performance benchmarking tests validating high-throughput batch writes.

<details>
<summary><strong>Commit History (15 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                    |
|:---------:|:----------:|:--------------------------------------------------------------------------------------------------|
| `18760b2` | 2026-05-19 | PERF-001 add documentation new feature Database and orm performance                               |
| `d67bdbc` | 2026-05-19 | PERF-001 migrate DB to use sequence for primary key and remove oneToMany relation for some fields |
| `a337bf8` | 2026-05-19 | PERF-001 Add repository for Transient columns                                                     |
| `a5a70b6` | 2026-05-19 | PERF-001 replace jpa Item Writer by an aggregate writer                                           |
| `08c32c2` | 2026-05-19 | PERF-001 refactor processor to check list                                                         |
| `66d0227` | 2026-05-19 | PERF-001 add ProteinEntryService to encapsulate READ operations for protein entry                 |
| `1184a0e` | 2026-05-19 | PERF-001 refactoring                                                                              |
| `0c5f66a` | 2026-05-19 | PERF-001 refactoring Protein entry processor                                                      |
| `62c6730` | 2026-05-19 | PERF-001 refactoring regroup classes by packages inside batch                                     |
| `3465556` | 2026-05-19 | PERF-001 unit tests for Handlers                                                                  |
| `c20bb4c` | 2026-05-19 | PERF-001 unit tests for Resolvers                                                                 |
| `26bd74b` | 2026-05-19 | PERF-001 unit tests for LineProcessorRegistry                                                     |
| `2778ca5` | 2026-05-19 | PERF-001 finish epic                                                                              |
| `d524c76` | 2026-05-19 | PERF-001 finish epic                                                                              |
| `077b82e` | 2026-05-19 | Merge pull request #7 from maaydi/implementation/PERF-001                                         |

</details>

---

#### [FRONTEND / UX] `GENE-002` — Advanced Filter Sidebar & Angular Signals Filter Store

- **Dates Active:** [2026-05-21 to 2026-05-23]
- **Commits:** 29 commits (`220bfbd..c86c1ad`)
- **Merged PR:** #8
- **Overview:** Built comprehensive reactive filter sidebar component for the Gene Explorer using Angular Signals and
  modular form controls.

**Backend Engineering:**

- Refined filtering API endpoints with support for molecular weight range, sequence length range, and evidence level
  filtering.

**Frontend & UX Engineering:**

- `GeneFilterComponent` sidebar featuring collapsible sections for gene names, organisms, evidence levels, and length
  ranges.
- `FilterStore` powered by Angular Signals providing reactive unidirectional state management.
- Reusable UI controls: `RangeInputComponent`, `SharedInputComponent`, and filter chips display.
- URL synchronization: serializing filter states into query parameters for bookmarkable searches.

**Quality Assurance & Testing:**

- Jasmine component tests for reactive filter updates, signal emissions, and reset actions.

<details>
<summary><strong>Commit History (29 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                             |
|:---------:|:----------:|:-------------------------------------------------------------------------------------------|
| `220bfbd` | 2026-05-21 | GENE-002 implement gene filter side bar                                                    |
| `bfa974b` | 2026-05-21 | GENE-002 implement gene table view                                                         |
| `a4445fe` | 2026-05-21 | GENE-002 add logo to application                                                           |
| `3b13465` | 2026-05-21 | GENE-002 align result table with filter side bar                                           |
| `859b29a` | 2026-05-21 | GENE-002 add error message to table when search failed enhance style                       |
| `dfc5054` | 2026-05-22 | GENE-002 Implement `filters.store.ts` signal store                                         |
| `39e709c` | 2026-05-22 | GENE-002 use evidence level in filters                                                     |
| `633be7b` | 2026-05-22 | GENE-002 add shared input , range-input and _form_style to remove duplication              |
| `9ab4332` | 2026-05-22 | GENE-002 add backend api to load keywords                                                  |
| `4ce9138` | 2026-05-22 | GENE-002 implement drop down list choice for selected keywords                             |
| `224b758` | 2026-05-22 | GENE-002 Add unit test for gene-filter.component                                           |
| `91944db` | 2026-05-22 | GENE-002 Add unit test for gene-page.component                                             |
| `35c07e1` | 2026-05-22 | GENE-002 Add unit test for gene-table                                                      |
| `89344d7` | 2026-05-22 | GENE-002 Add unit test for input.component                                                 |
| `28cf546` | 2026-05-22 | GENE-002 Add unit test for range input component                                           |
| `87aad1f` | 2026-05-22 | GENE-002 Update code documentation                                                         |
| `609f0a3` | 2026-05-23 | GENE-002 Add Gene Name to filter                                                           |
| `5d3d50f` | 2026-05-23 | GENE-002 Add Min < Max validation                                                          |
| `9a75f05` | 2026-05-23 | GENE-002 Active filter chips dismissible and there is no per-chip clear action             |
| `3312ff6` | 2026-05-23 | GENE-002 Filter state is persistent across page navigation                                 |
| `d484177` | 2026-05-23 | GENE-002 remove ngClass                                                                    |
| `674f9d2` | 2026-05-23 | GENE-002 remove standalone: true                                                           |
| `a04cb41` | 2026-05-23 | GENE-002 Add validation for other form fields                                              |
| `5d28e49` | 2026-05-23 | GENE-002 Add unit test and finish feature                                                  |
| `9345959` | 2026-05-23 | Merge branch 'refs/heads/main' into implementation/GENE-002                                |
| `32510a6` | 2026-05-23 | GENE-002 merge main into branch                                                            |
| `a2fb83a` | 2026-05-23 | GENE-002 merge main into branch                                                            |
| `5325d0d` | 2026-05-23 | Merge remote-tracking branch 'origin/implementation/GENE-002' into implementation/GENE-002 |
| `c86c1ad` | 2026-05-23 | Merge pull request #8 from maaydi/implementation/GENE-002                                  |

</details>

---

#### [FRONTEND / DATA TABLE] `GENE-003` — Interactive AG Grid Protein Explorer Table & Global Search

- **Dates Active:** [2026-05-23 to 2026-05-26]
- **Commits:** 20 commits (`fdb8ebf..67e7ffb`)
- **Merged PR:** #9
- **Overview:** Integrated enterprise-grade AG Grid table for blazing-fast browsing of hundreds of thousands of protein
  records with server-side pagination, sorting, and global keyword search.

**Frontend & UX Engineering:**

- `GenesTableComponent` integrating AG Grid with custom cell renderers (evidence level badge, reviewed status badge).
- Server-side pagination and multi-column sorting integration with backend APIs.
- `GlobalSearchComponent` with debounced input, keyboard shortcuts, and instant keyword suggestions.
- Refined application styling based on bioinformatics design system guidelines (BMAD UX).

**Quality Assurance & Testing:**

- AG Grid integration tests, column sorting tests, and global search debounce validation.

<details>
<summary><strong>Commit History (20 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                   |
|:---------:|:----------:|:-------------------------------------------------------------------------------------------------|
| `fdb8ebf` | 2026-05-23 | GENE-003 update documentation                                                                    |
| `ddf9fdd` | 2026-05-24 | Update copilot instruction with senior java/angular instruction for code review and unit testing |
| `f8257f8` | 2026-05-24 | GENE-003 update current app styles using BMAD ux agent                                           |
| `604a2ac` | 2026-05-24 | GENE-003 update current app styles using BMAD ux agent                                           |
| `3b84b12` | 2026-05-25 | GENE-003 create a component for global search                                                    |
| `699a5eb` | 2026-05-25 | GENE-003 fix server crash for keywords load                                                      |
| `26d6c62` | 2026-05-25 | GENE-003 Add global search validation                                                            |
| `bc3faec` | 2026-05-25 | GENE-003 add global search to page                                                               |
| `2ce06c4` | 2026-05-25 | GENE-003 move chips to a separate component                                                      |
| `1348d08` | 2026-05-25 | GENE-003 unit tests                                                                              |
| `71dd824` | 2026-05-25 | GENE-003 Add pagination                                                                          |
| `2028bca` | 2026-05-25 | GENE-003 Add unit test for pagination                                                            |
| `2cae93d` | 2026-05-25 | GENE-003 Add sortable behaviour                                                                  |
| `b22bae9` | 2026-05-25 | GENE-003 Add keywords as chips                                                                   |
| `ec69232` | 2026-05-25 | GENE-003 refactor table to use ag-grid                                                           |
| `0383ce1` | 2026-05-25 | GENE-003 refactor table to use ag-grid                                                           |
| `fc6542d` | 2026-05-25 | GENE-003 finish design application using Bmad                                                    |
| `df82414` | 2026-05-25 | GENE-003 finish feature                                                                          |
| `a4db8d7` | 2026-05-25 | Merge pull request #9 from maaydi/implementation/GENE-003                                        |
| `67e7ffb` | 2026-05-26 | update start-dev.sh to retrieve java home from .env                                              |

</details>

---

#### [FEATURE] `DETAIL-001` — Comprehensive Protein Detail View & Tabbed Workspace

- **Dates Active:** [2026-05-26 to 2026-05-26]
- **Commits:** 20 commits (`c0602bd..306e154`)
- **Merged PR:** #10
- **Overview:** Created an in-depth Gene Detail page presenting complete biochemical, structural, sequence, and external
  database cross-references.

**Backend Engineering:**

- Enriched `ProteinDetailDto` delivering full entity graph in a single optimized payload.

**Frontend & UX Engineering:**

- `GeneDetailComponent` with tabbed architecture: Overview, Sequence, Annotations/Features, and Cross-References.
- Protein metadata header displaying accession, entry name, reviewed status, organism, and evidence level.
- FASTA Sequence viewer with formatted residue blocks, copy-to-clipboard button, and length/molecular weight metrics.
- Cross-references grid linking to external databases (PDB, RefSeq, Ensembl, GO, Pfam).
- `BreadcrumbsComponent` for seamless navigation hierarchy.

**Quality Assurance & Testing:**

- Unit tests for tab switching, clipboard interaction, and empty/missing feature fallbacks.

<details>
<summary><strong>Commit History (20 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                           |
|:---------:|:----------:|:---------------------------------------------------------------------------------------------------------|
| `c0602bd` | 2026-05-26 | DETAIL-001 Start implementation                                                                          |
| `265d226` | 2026-05-26 | DETAIL-001 extract review and evidence level style in shared file                                        |
| `922012d` | 2026-05-26 | DETAIL-001 create breadcrumbs component                                                                  |
| `bf2b704` | 2026-05-26 | DETAIL-001 add metadata to gene detail page protein full name accession/reviewed/entryname/evidencelevel |
| `00f537f` | 2026-05-26 | DETAIL-001 add summary section                                                                           |
| `01a8127` | 2026-05-26 | DETAIL-001 add sequence section                                                                          |
| `902945f` | 2026-05-26 | DETAIL-001 add features section                                                                          |
| `26aea29` | 2026-05-26 | DETAIL-001 add cross references section                                                                  |
| `1e45a3a` | 2026-05-26 | DETAIL-001 add host organizm to summary section                                                          |
| `3f92b12` | 2026-05-26 | DETAIL-001 add go term section                                                                           |
| `83dfd8b` | 2026-05-26 | DETAIL-001 add comments section                                                                          |
| `de37e77` | 2026-05-26 | DETAIL-001 add comments section                                                                          |
| `ba72412` | 2026-05-26 | DETAIL-001 add publication section                                                                       |
| `b115329` | 2026-05-26 | DETAIL-001 add UI when protein not found                                                                 |
| `72e10e5` | 2026-05-26 | DETAIL-001 add missing unit tests                                                                        |
| `f025d54` | 2026-05-26 | DETAIL-001 add missing unit tests                                                                        |
| `f0acaad` | 2026-05-26 | DETAIL-001 update readme                                                                                 |
| `6b6aa74` | 2026-05-26 | DETAIL-001 refactor code and fix api-contract                                                            |
| `0f4159c` | 2026-05-26 | DETAIL-001 refactor code and finish feature                                                              |
| `306e154` | 2026-05-26 | Merge pull request #10 from maaydi/implementation/detail-001                                             |

</details>

---

#### [ANALYTICS] `ANALYTICS-001` — Analytics Engine, PostgreSQL Materialized Views & Aggregate Metrics

- **Dates Active:** [2026-05-28 to 2026-05-30]
- **Commits:** 25 commits (`9c359e7..d30d646`)
- **Merged PR:** #11
- **Overview:** Engineered high-performance analytics calculation layer backed by PostgreSQL Materialized Views to
  compute population-wide biological metrics in sub-second timeframes.

**Backend Engineering:**

- `AnalyticsController` and `AnalyticsService` calculating KPIs, histograms, and distributions.
- PostgreSQL Materialized Views: `mv_dashboard_kpis`, `mv_length_histogram`, `mv_evidence_distribution`, and
  `mv_organism_distribution`.
- `MaterializedViewRefreshService` providing background refresh and SLA breach detection with sequence alerts.
- Aggregate DTOs: `DashboardKpisDto`, `HistogramBucketDto`, `DistributionItemDto`.

**Quality Assurance & Testing:**

- JUnit 5 tests for analytics calculation logic and failure recovery in materialized view refresh scheduling.

<details>
<summary><strong>Commit History (25 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                                                      |
|:---------:|:----------:|:------------------------------------------------------------------------------------------------------------------------------------|
| `9c359e7` | 2026-05-28 | ANALYTICS-001 implement DTOs                                                                                                        |
| `310223c` | 2026-05-28 | ANALYTICS-001 implement Controller                                                                                                  |
| `371c9f7` | 2026-05-28 | ANALYTICS-001 update java doc for md references                                                                                     |
| `a07c763` | 2026-05-28 | ANALYTICS-001 findDashboardKpis implemented                                                                                         |
| `4a81e4f` | 2026-05-28 | ANALYTICS-001 findLengthHistogram implemented                                                                                       |
| `8b40602` | 2026-05-29 | ANALYTICS-001 findByOrganism implemented                                                                                            |
| `102ea91` | 2026-05-29 | ANALYTICS-001 findReviewedRatio implemented                                                                                         |
| `f1a369b` | 2026-05-29 | ANALYTICS-001 mv_evidence_distribution implemented                                                                                  |
| `7e01255` | 2026-05-29 | ANALYTICS-001 mv_keyword_frequency implemented                                                                                      |
| `118051a` | 2026-05-30 | ANALYTICS-001 refactor import job to add skip limit and listener                                                                    |
| `a740090` | 2026-05-30 | ANALYTICS-001 update view/entity for refresh materialized views                                                                     |
| `1137c2f` | 2026-05-30 | ANALYTICS-001 add refresh views service and integrate it with import job                                                            |
| `1a4e5d6` | 2026-05-30 | ANALYTICS-001 add order by with limit                                                                                               |
| `bb71254` | 2026-05-30 | ANALYTICS-001 add refresh logs to save result for refreshing views per job execution                                                |
| `a5e37ef` | 2026-05-30 | ANALYTICS-001 add Async view refresh lacks SLA : timeout, retry, and alerting for failures                                          |
| `7a9aeed` | 2026-05-30 | ANALYTICS-001 fix repository PK and OrderBy                                                                                         |
| `03447d7` | 2026-05-30 | ANALYTICS-001 Integration Tests for AnalyticsController                                                                             |
| `0f3c8d9` | 2026-05-30 | ANALYTICS-001 Unit tests for AnalyticsService                                                                                       |
| `a1f1d5b` | 2026-05-30 | ANALYTICS-001 tests for refresh views and SLA                                                                                       |
| `e02bde5` | 2026-05-30 | ANALYTICS-001 Add JaCoCo for coverage                                                                                               |
| `2f912c8` | 2026-05-30 | ANALYTICS-001 update plan and journal and finish implementation                                                                     |
| `ec61cdd` | 2026-05-30 | ANALYTICS-001 fix vulnerable dependency                                                                                             |
| `b2efbaa` | 2026-05-30 | ANALYTICS-001 update documentation                                                                                                  |
| `a0fdf99` | 2026-05-30 | ANALYTICS-001 fix unit testMaterializedViewRefreshServiceTest.refreshAllDashboardViews_whenFailuresAndSlaBreach_emitsSequenceAlerts |
| `d30d646` | 2026-05-30 | Merge pull request #11 from maaydi/implementation/analytics-001                                                                     |

</details>

---

#### [FRONTEND / CHARTS] `DASH-001` — Interactive Analytics Dashboard UI & Dynamic Charts

- **Dates Active:** [2026-05-31 to 2026-05-31]
- **Commits:** 25 commits (`e753775..f7903c2`)
- **Merged PR:** #12
- **Overview:** Designed and implemented interactive Dashboard page featuring visual KPI cards and charts (Chart.js /
  ECharts) representing whole-proteome analytics.

**Frontend & UX Engineering:**

- `DashboardComponent` shell displaying high-level statistical summary cards.
- Interactive charts: Sequence Length Histogram, Reviewed vs Unreviewed Donut Chart, Evidence Level Distribution Bar
  Chart, Top Organisms Chart.
- Loading skeletons, error states, and responsive grid layouts for dashboard tiles.
- Reactive data binding connecting dashboard charts directly to `AnalyticsService` REST APIs.

**Quality Assurance & Testing:**

- Frontend component tests verifying chart data binding and empty-state rendering.

<details>
<summary><strong>Commit History (25 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                    |
|:---------:|:----------:|:--------------------------------------------------------------------------------------------------|
| `e753775` | 2026-05-31 | DASH-001 Start feature : DashboardComponent shell implemented                                     |
| `7479bed` | 2026-05-31 | DASH-001 Length histogram chart implemented with mock data                                        |
| `1785d72` | 2026-05-31 | DASH-001 Reviewed ratio donut implemented with mock data                                          |
| `f00ed55` | 2026-05-31 | DASH-001 Evidence levels chart implemented with mock data                                         |
| `894bb13` | 2026-05-31 | DASH-001 Top organisms chart implemented with mock data                                           |
| `28484fa` | 2026-05-31 | DASH-001 kpi card implemented with mock data                                                      |
| `40d6c42` | 2026-05-31 | DASH-001 adapt style and enhance dashboards visuals                                               |
| `ea7eabb` | 2026-05-31 | DASH-001 integrate backend rest with evidence level chart                                         |
| `7ffd6bc` | 2026-05-31 | DASH-001 integrate backend rest with length histogram chart                                       |
| `168f6b6` | 2026-05-31 | DASH-001 integrate backend rest with reviewed ratio chart                                         |
| `7e00035` | 2026-05-31 | DASH-001 integrate backend rest with top histogram chart                                          |
| `7ce60b5` | 2026-05-31 | DASH-001 integrate backend rest with dashboard screen                                             |
| `7aee526` | 2026-05-31 | DASH-001 add bar tooltip for length distribution                                                  |
| `06de61c` | 2026-05-31 | DASH-001 change loading text indication by spinner component                                      |
| `abc3f6f` | 2026-05-31 | DASH-001 order level asc                                                                          |
| `1033d36` | 2026-05-31 | DASH-001 add retry loading for reviewed ratio                                                     |
| `420f15c` | 2026-05-31 | DASH-001 add retry loading for top organism                                                       |
| `596d49a` | 2026-05-31 | DASH-001 add retry loading for histogram                                                          |
| `7d5b14a` | 2026-05-31 | DASH-001 add retry loading for kpis                                                               |
| `a9fc080` | 2026-05-31 | DASH-001 add retry for evidence chart and navigation to /genes fix broken pipe warning in backend |
| `5013cbb` | 2026-05-31 | DASH-001 add navigation to Gene explorer when click on bars                                       |
| `b3910ab` | 2026-05-31 | DASH-001 fix dashboard show before login                                                          |
| `cb2e227` | 2026-05-31 | DASH-001 finish feature                                                                           |
| `97a70c5` | 2026-05-31 | DASH-001 finish feature                                                                           |
| `f7903c2` | 2026-05-31 | Merge pull request #12 from maaydi/implementation/DASH-001                                        |

</details>

---

#### [FEATURE] `FILTER-001` — Saved Filters & User Preset Management

- **Dates Active:** [2026-06-02 to 2026-06-05]
- **Commits:** 18 commits (`f8dccfe..aa4c4ad`)
- **Merged PR:** #13
- **Overview:** Added personalized saved filters capability allowing researchers to persist complex filter presets, load
  them with one click, and manage their preset library.

**Backend Engineering:**

- `SavedFilter` entity with user ownership association, filter criteria JSON serialization, and unique name constraint.
- `SavedFilterController` offering RESTful CRUD operations (`/api/saved-filters`).
- `SavedFilterService` with validation enforcing user isolation.

**Frontend & UX Engineering:**

- `SaveFilterModalComponent` with input validation and duplicate name checks.
- `SavedFiltersDropdownComponent` in filter bar for instant preset selection.
- `SavedFiltersManagerComponent` for managing, renaming, and deleting saved presets.

**Quality Assurance & Testing:**

- Backend unit and integration tests for CRUD operations and authorization checks; frontend modal interaction tests.

<details>
<summary><strong>Commit History (18 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                           |
|:---------:|:----------:|:-------------------------------------------------------------------------|
| `f8dccfe` | 2026-06-02 | FILTER-001 Entity and repository implemented                             |
| `7cc88a5` | 2026-06-02 | FILTER-001 DTOs and mapper implemented                                   |
| `362f243` | 2026-06-02 | FILTER-001 SavedFilterService implemented                                |
| `49a0880` | 2026-06-02 | FILTER-001 SavedFilterService implemented unit tests                     |
| `99a12ed` | 2026-06-03 | FILTER-001 SavedFilterController completed                               |
| `cfb172e` | 2026-06-03 | FILTER-001 SavedFilterController completed - unit tests                  |
| `06d4927` | 2026-06-03 | FILTER-001 Customize banner                                              |
| `f83f572` | 2026-06-03 | FILTER-001 SavedFilterController completed                               |
| `9a721e6` | 2026-06-04 | FILTER-001 move application name in top of application.yml               |
| `5f24131` | 2026-06-04 | FILTER-001 refactor code and extract common utilities                    |
| `1732c01` | 2026-06-04 | FILTER-001 Frontend SavedFiltersComponent                                |
| `2b4813d` | 2026-06-04 | FILTER-001 Frontend SavedFiltersComponent add apply and delete behaviour |
| `3be2d94` | 2026-06-04 | FILTER-001 Frontend SavedFiltersComponent Documentation and spec         |
| `d4b0f9e` | 2026-06-05 | FILTER-001 Dialog/modal triggered "Save Filters" button                  |
| `31aac5f` | 2026-06-05 | FILTER-001 Toast when save/delete filter                                 |
| `55fd930` | 2026-06-05 | FILTER-001 Code review with BMAD agent - Fix issue                       |
| `7a27da9` | 2026-06-05 | FILTER-001 finish feature                                                |
| `aa4c4ad` | 2026-06-05 | Merge pull request #13 from maaydi/implementation/filter-001             |

</details>

---

#### [FEATURE] `EXPORT-001` — Streaming CSV & Chart Visualization Image Export

- **Dates Active:** [2026-06-07 to 2026-06-12]
- **Commits:** 34 commits (`a964e98..c3a5615`)
- **Merged PR:** #14
- **Overview:** Enabled high-volume CSV data streaming export with custom column selection and high-resolution chart
  image export for publication.

**Backend Engineering:**

- Dynamic analytical repository extensions (`AnalyticsProteinRepository`).
- Chunked/streaming CSV export endpoint supporting custom delimiter, headers, and filtered query sets.
- Added molecular weight vs sequence length scatter plot data endpoint.

**Frontend & UX Engineering:**

- CSV Export dialog with column selection checkboxes and format options.
- Client-side chart image export (PNG / SVG) capturing dashboard charts with proper resolution and legends.
- `LimitSelectorComponent` allowing dynamic threshold adjustment for top charts.

**Quality Assurance & Testing:**

- Unit tests for streaming CSV generation and file download triggers.

<details>
<summary><strong>Commit History (34 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                                    |
|:---------:|:----------:|:------------------------------------------------------------------------------------------------------------------|
| `a964e98` | 2026-06-07 | EXPORT-001 Start feature - refactor dashboard and make component use provider instead of concrete implementation  |
| `4003a36` | 2026-06-07 | EXPORT-001 create a limit-selector component to make dashboard updated based on limit                             |
| `bef808d` | 2026-06-07 | EXPORT-001 add limit-selector to top organism chart                                                               |
| `9896894` | 2026-06-07 | EXPORT-001 add keyword frequency to dashboard                                                                     |
| `c9714f6` | 2026-06-07 | EXPORT-001 fix unit test                                                                                          |
| `c2d80a1` | 2026-06-07 | EXPORT-001 Add AnalyticsProteinRepository extension for dynamic analytic requests                                 |
| `7a27251` | 2026-06-07 | EXPORT-001 Add Analytics service and controller                                                                   |
| `074726d` | 2026-06-07 | EXPORT-001 Add pie chart for evidence level                                                                       |
| `343b73b` | 2026-06-07 | EXPORT-001 first analytic dashboard component setup                                                               |
| `5cfd76f` | 2026-06-08 | EXPORT-001 add filter input to analytic providers and adapt service                                               |
| `ba251e7` | 2026-06-08 | EXPORT-001 refactor evidence chart and bar to remove duplication                                                  |
| `fd634e7` | 2026-06-08 | EXPORT-001 add filters to analytic                                                                                |
| `c7ac6d0` | 2026-06-09 | EXPORT-001 fix unit tests and analytic returned objects                                                           |
| `29bccc5` | 2026-06-09 | EXPORT-001 make analytic load visualization from saved filters                                                    |
| `e6cb3d7` | 2026-06-09 | EXPORT-001 add unit test for analytics component and service                                                      |
| `6e6096f` | 2026-06-09 | EXPORT-001 Length vs Molecular Weight scatter                                                                     |
| `bd54a3f` | 2026-06-10 | EXPORT-001 Length vs Molecular Weight scatter : implement backend controller and service                          |
| `4b590ea` | 2026-06-10 | EXPORT-001 Length vs Molecular Weight scatter : add range colors                                                  |
| `f95d21a` | 2026-06-11 | EXPORT-001 Frontend — "Export PNG" buttons (charts) export service                                                |
| `9bce968` | 2026-06-11 | EXPORT-001 Frontend — "Export PNG" buttons (charts)                                                               |
| `bc29672` | 2026-06-11 | EXPORT-001 Frontend — "Export PNG" buttons (charts) scatter chart                                                 |
| `1f21a77` | 2026-06-11 | EXPORT-001 Frontend — "Export PNG" buttons (charts) centralize notification service and adapt exported file names |
| `30311ef` | 2026-06-11 | EXPORT-001 Backend — `CsvExportService`                                                                           |
| `d2cafe7` | 2026-06-11 | EXPORT-001 Add "Export CSV" button to `GenesPageComponent`                                                        |
| `2381f0e` | 2026-06-11 | EXPORT-001 Add "Export CSV" button to `GenesPageComponent` Handle 413 error                                       |
| `04a8836` | 2026-06-11 | EXPORT-001  Show loading indicator on button during download                                                      |
| `081f44f` | 2026-06-11 | EXPORT-001  add gene page tests for export                                                                        |
| `98ee4e6` | 2026-06-11 | EXPORT-001  fix backend tests                                                                                     |
| `564720e` | 2026-06-11 | EXPORT-001  fix saved filter tests                                                                                |
| `8181f5a` | 2026-06-11 | EXPORT-001  unit test for image-export-service                                                                    |
| `1339409` | 2026-06-12 | EXPORT-001  unit test for keyword frequency                                                                       |
| `a491362` | 2026-06-12 | EXPORT-001  unit test for evidence level pie chart                                                                |
| `4e661fe` | 2026-06-12 | EXPORT-001 finish feature                                                                                         |
| `c3a5615` | 2026-06-12 | Merge pull request #14 from maaydi/implementation/EXPORT-001                                                      |

</details>

---

#### [ANALYTICS] `COMPARE-001` — Dual-Cohort Comparison Analytics & Delta Metrics

- **Dates Active:** [2026-06-12 to 2026-06-18]
- **Commits:** 18 commits (`9a9d13f..599e58a`)
- **Merged PR:** #15
- **Overview:** Built powerful cohort comparison feature enabling researchers to define two distinct biological cohorts
  (Cohort A vs Cohort B) and compare distributions side-by-side.

**Backend Engineering:**

- `AnalyticsService.compare()` computing side-by-side distributions, Kolmogorov-Smirnov-style metrics, and differential
  organism representations.
- `CompareRequestDto` and `CompareResponseDto` handling dual filter specifications.

**Frontend & UX Engineering:**

- `CompareComponent` with dual filter controls (Set A vs Set B).
- Comparative visualizations: overlapping length histograms, dual donut charts, delta metric cards.
- Toggle button in Analytics page to seamlessly switch between Single Dashboard and Comparison Mode.

**Quality Assurance & Testing:**

- Backend mathematical validation tests for delta calculations; UI component tests for dual filter dispatch.

<details>
<summary><strong>Commit History (18 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                                                               |
|:---------:|:----------:|:---------------------------------------------------------------------------------------------------------------------------------------------|
| `9a9d13f` | 2026-06-12 | COMPARE-001 start feature Backend DTOs implemented                                                                                           |
| `aaef3b7` | 2026-06-12 | COMPARE-001 AnalyticsService.compare() implemented                                                                                           |
| `042f3a0` | 2026-06-12 | COMPARE-001 Controller endpoint added                                                                                                        |
| `a834192` | 2026-06-12 | COMPARE-001 backend  Unit tests                                                                                                              |
| `9678d7b` | 2026-06-12 | COMPARE-001 compare component intermediate commit                                                                                            |
| `a055b58` | 2026-06-13 | COMPARE-001 adapt gene filter style to be more dynamic and compatible with analytic compare component                                        |
| `bc35b39` | 2026-06-16 | COMPARE-001 update Analytic page to support switch mode between compare and analytics                                                        |
| `207c498` | 2026-06-16 | COMPARE-001 add compare and reset buttons to compare component                                                                               |
| `9c18d6e` | 2026-06-16 | COMPARE-001 Add simple view to compare result                                                                                                |
| `93740d6` | 2026-06-18 | COMPARE-001 Side-by-side length distribution charts (Set A vs Set B)                                                                         |
| `fcdcae7` | 2026-06-18 | COMPARE-001 Side-by-side length distribution charts (Set A vs Set B) KPI row: count, avg length, reviewed count, reviewed ratio for each set |
| `f121c18` | 2026-06-18 | COMPARE-001 Side-by-side evidence distribution charts (Set A vs Set B)                                                                       |
| `f5db005` | 2026-06-18 | COMPARE-001 Warning `@if (setsAreIdentical())` — computed signal checking deep equality of Set A and Set B                                   |
| `28a07d9` | 2026-06-18 | COMPARE-001 update default selected filter in select component                                                                               |
| `3743cbe` | 2026-06-18 | COMPARE-001 add unit tests                                                                                                                   |
| `a15c7ad` | 2026-06-18 | COMPARE-001 code Documentation updated                                                                                                       |
| `63070a1` | 2026-06-18 | COMPARE-001 Documentation updated Finish feature                                                                                             |
| `599e58a` | 2026-06-18 | Merge pull request #15 from maaydi/implementation/COMPARE-001                                                                                |

</details>

---

#### [OPERATIONS] `OPS-001` — Production Observability, Audit Logging & Rate Limiting

- **Dates Active:** [2026-06-19 to 2026-06-25]
- **Commits:** 19 commits (`b00a426..bdd6e3b`)
- **Merged PR:** #16
- **Overview:** Hardened platform with comprehensive audit logging, API rate limiting via Token Bucket algorithm,
  Actuator health probes, and pagination safety ceilings.

**Backend Engineering:**

- `AuditLog` entity and service capturing user actions (logins, imports, exports, filter saves) with IP address and
  timestamp.
- Bucket4j rate-limiting filter protecting public and sensitive endpoints from abuse.
- Enforced global pagination cap preventing OutOfMemory errors from excessively large page requests.
- Spring Boot Actuator configuration with `/actuator/health`, `/actuator/info`, `/actuator/metrics`.

**Frontend & UX Engineering:**

- Frontend pagination safeguards and error handling on rate-limit HTTP 429 responses.

**DevOps & Infrastructure:**

- Database migration creating `audit_log` table with cascade rules.

**Quality Assurance & Testing:**

- Rate limiting unit tests validating token exhaustion and replenish rates; audit logging verification tests.

<details>
<summary><strong>Commit History (19 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                 |
|:---------:|:----------:|:---------------------------------------------------------------|
| `b00a426` | 2026-06-19 | OPS-001 DB migration created                                   |
| `f2228df` | 2026-06-19 | OPS-001 AuditLog entity + repository + service implemented     |
| `6b71424` | 2026-06-19 | OPS-001 Backend — Audit Hooks                                  |
| `dd4815a` | 2026-06-21 | OPS-001 Backend — Actuator Configuration (`application.yml`)   |
| `c32a70b` | 2026-06-21 | OPS-001 Saved filter : add pagination frontend                 |
| `632cbd9` | 2026-06-21 | OPS-001 Backend — Pagination Cap                               |
| `8e87fff` | 2026-06-21 | OPS-001 Fix unit test add delete cascade to audit logs         |
| `dedc3d8` | 2026-06-21 | OPS-001 Fix unit test frontend                                 |
| `9b618df` | 2026-06-22 | OPS-001 Add targetId from Annotation                           |
| `775663c` | 2026-06-22 | OPS-001 Authentication event listener for audit login          |
| `0964e15` | 2026-06-22 | OPS-001 Add context holder to collect endpoint infos for audit |
| `20a7634` | 2026-06-22 | OPS-001 remove relation between appuser and auditlog tables    |
| `bc48844` | 2026-06-23 | OPS-001 remove duplicated row for login audit                  |
| `cb78468` | 2026-06-23 | OPS-001 enhance services logging and controller audit          |
| `a111665` | 2026-06-24 | OPS-001 Backend — Rate Limiting (Bucket4j)                     |
| `be056cb` | 2026-06-24 | OPS-001 Backend — Rate Limiting (Bucket4j) Fix unit tests      |
| `7ca0b35` | 2026-06-24 | OPS-001 Backend — Rate Limiting (Bucket4j) Unit test           |
| `216a085` | 2026-06-24 | OPS-001 Add documentation                                      |
| `bdd6e3b` | 2026-06-25 | Merge pull request #16 from maaydi/implementation/OPS-001      |

</details>

---

#### [CACHING] `CACHE-001` — Multi-Tier Distributed Caching (Redis/Caffeine) & Eviction Hooks

- **Dates Active:** [2026-06-25 to 2026-06-25]
- **Commits:** 19 commits (`86ad2b3..765c893`)
- **Merged PR:** #17
- **Overview:** Implemented enterprise caching architecture utilizing Redis for distributed cache and Caffeine for
  in-memory L1 cache, with automated Spring Batch eviction hooks.

**Backend Engineering:**

- Spring Cache configuration supporting dual cache managers: Caffeine (local) and Redis (distributed).
- Targeted cache annotations (`@Cacheable`, `@CacheEvict`) on analytics KPIs, gene search, and saved filters.
- Spring Batch StepExecutionListener hook automatically evicting stale analytics and gene caches upon import job
  completion.
- Graceful fallback to direct DB queries when Redis is unreachable.

**DevOps & Infrastructure:**

- Added Redis container to `docker-compose.yml` with health checks and persistence configuration.

**Quality Assurance & Testing:**

- Cache eviction tests verifying data invalidation upon batch job termination.

<details>
<summary><strong>Commit History (19 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                       |
|:---------:|:----------:|:-----------------------------------------------------------------------------------------------------|
| `86ad2b3` | 2026-06-25 | CACHE-001 Initialize Feature                                                                         |
| `250ddbb` | 2026-06-25 | CACHE-001  Provision Redis Infrastructure Container                                                  |
| `736f51a` | 2026-06-25 | CACHE-001 Establish Project Dependencies & Spring Properties                                         |
| `ad0bcbe` | 2026-06-25 | CACHE-001 Establish Project Dependencies & Spring Properties Fix unit test after adding cache config |
| `545f003` | 2026-06-25 | CACHE-001 Establish Project Dependencies & Spring Properties Fix start-dev.sh script                 |
| `04e29e3` | 2026-06-25 | CACHE-001 Establish Project Dependencies & Spring Properties Add cache manager for other services    |
| `acda123` | 2026-06-25 | CACHE-001 Establish Project Dependencies & Spring Properties Fix saved filters cache and evict       |
| `045d74c` | 2026-06-25 | CACHE-001 Implement Caching across Analytics Subsystems                                              |
| `71d21e0` | 2026-06-25 | CACHE-001 Implement Caching across Analytics Subsystems                                              |
| `b20cab9` | 2026-06-25 | CACHE-001 change app user to support status and stop deleting it for cache and audit purpose         |
| `62e91e9` | 2026-06-25 | CACHE-001 adapt cache for object and records                                                         |
| `5e42339` | 2026-06-25 | CACHE-001 refactor CacheConfig update styles                                                         |
| `b82c5d2` | 2026-06-25 | CACHE-001 add profil for test to avoid NoUniqueBeanDefinitionException                               |
| `e2d6f27` | 2026-06-25 | CACHE-001 Add cache unit test                                                                        |
| `df37ded` | 2026-06-25 | CACHE-001 Fix frontend style for some components                                                     |
| `2dedde5` | 2026-06-25 | CACHE-001 Add interceptor for rate limit                                                             |
| `36a5793` | 2026-06-25 | CACHE-001 Spring Batch Orchestrated Eviction Hooks                                                   |
| `6c6ad96` | 2026-06-25 | CACHE-001 Documentation                                                                              |
| `765c893` | 2026-06-25 | Merge pull request #17 from maaydi/implementation/CACHE-001                                          |

</details>

---

#### [ACCESSIBILITY / UX] `A11Y-001` — Accessibility (WCAG 2.1 AA), Theme Toggle & UI State Hosts

- **Dates Active:** [2026-06-25 to 2026-06-29]
- **Commits:** 30 commits (`4e5c6a0..bfed51d`)
- **Merged PR:** #18
- **Overview:** Achieved WCAG 2.1 AA accessibility compliance across all pages, implemented high-contrast dark/light
  theme switching, and created unified state host components.

**Frontend & UX Engineering:**

- `ThemeService` with local storage persistence and system preference detection (`prefers-color-scheme`).
- Theme toggle control in navigation bar with smooth SCSS variable transitions.
- Accessible UI state hosts: `LoadingStateComponent`, `EmptyStateComponent`, and `ErrorStateComponent`.
- Full keyboard navigation, visible focus indicators, and semantic ARIA attributes across AG Grid, forms, and tabs.

**Quality Assurance & Testing:**

- Automated Axe-core accessibility smoke tests validating zero critical accessibility violations.

<details>
<summary><strong>Commit History (30 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                   |
|:---------:|:----------:|:---------------------------------------------------------------------------------|
| `4e5c6a0` | 2026-06-25 | A11Y-001 Start feature                                                           |
| `a213b10` | 2026-06-26 | A11Y-001 update dev script to make it interactive                                |
| `ae5f38f` | 2026-06-26 | A11Y-001 ThemeService implemented                                                |
| `6d629a9` | 2026-06-26 | A11Y-001 Theme toggle in navbar implemented                                      |
| `1d59f0c` | 2026-06-27 | A11Y-001 Theme toggle style variables updated                                    |
| `efdc6c3` | 2026-06-27 | A11Y-001 Theme toggle style refactor : remove duplication / rename variables     |
| `070343c` | 2026-06-27 | A11Y-001 remove deprecated version                                               |
| `9c77c59` | 2026-06-28 | A11Y-001 fix start spring warning ( builder default)                             |
| `5059826` | 2026-06-28 | A11Y-001 Add update password action                                              |
| `c87c559` | 2026-06-28 | A11Y-001 change password dto                                                     |
| `ba8c0c5` | 2026-06-28 | A11Y-001 PUT /api/auth/password implemented                                      |
| `7ec90ac` | 2026-06-28 | A11Y-001 update frontend guard to load theme at startup and after refresh        |
| `79644f4` | 2026-06-28 | A11Y-001 update frontend to call backend api after SSR render                    |
| `6b921e2` | 2026-06-28 | A11Y-001  Revoke all refresh tokens for the user                                 |
| `c4a4168` | 2026-06-29 | A11Y-001  Revoke all refresh tokens for the user cache evict after token expires |
| `5dd8d0e` | 2026-06-29 | A11Y-001 handle 401 responses — trigger token refresh or redirect to login       |
| `2d268af` | 2026-06-29 | A11Y-001 logout backend implementation                                           |
| `de0f99a` | 2026-06-29 | A11Y-001 AuthController integration test                                         |
| `94ea9e9` | 2026-06-29 | A11Y-001 Frontend — Password Change Form                                         |
| `f899d30` | 2026-06-29 | A11Y-001 Make JSON serialization uniform for refresh-token cache manager         |
| `d4f8243` | 2026-06-29 | A11Y-001 axe-core e2e configured                                                 |
| `b079afc` | 2026-06-29 | A11Y-001 frontend unit tests                                                     |
| `e2d8d03` | 2026-06-29 | A11Y-001 Documentation                                                           |
| `0bd7c4e` | 2026-06-29 | A11Y-001 Documentation                                                           |
| `d07dde1` | 2026-06-29 | A11Y-001 Documentation Rest Controller                                           |
| `d7a225f` | 2026-06-29 | A11Y-001 Documentation Services                                                  |
| `d641018` | 2026-06-29 | A11Y-001 Unit tests                                                              |
| `a33163c` | 2026-06-29 | A11Y-001 Finish feature                                                          |
| `8897372` | 2026-06-29 | A11Y-001 Finish feature                                                          |
| `bfed51d` | 2026-06-29 | Merge pull request #18 from maaydi/implementation/A11Y-001                       |

</details>

---

#### [CI/CD & QUALITY] `BUILD-001` — JaCoCo Code Coverage & CI/CD Pipeline Stabilization

- **Dates Active:** [2026-06-30 to 2026-06-30]
- **Commits:** 8 commits (`e4b8de1..c8c7e5f`)
- **Merged PR:** #19
- **Overview:** Integrated JaCoCo test coverage enforcement in Maven build, optimized frontend bundle sizes, and added
  comprehensive unit test suites.

**Backend Engineering:**

- Configured JaCoCo Maven plugin with coverage reporting and minimum threshold verification.
- Added unit tests covering security filters, exception mappers, and utility classes.

**Frontend & UX Engineering:**

- Resolved duplicated SCSS dependencies and optimized Angular production build size.
- Added unit test specs for `filter-chips-builder`, `gene-filter.utils`, `date-formatter`, and `ErrorInterceptor`.

**DevOps & Infrastructure:**

- Stabilized Docker multi-stage builds for backend, frontend, and nginx reverse proxy.

<details>
<summary><strong>Commit History (8 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                                           |
|:---------:|:----------:|:-------------------------------------------------------------------------------------------------------------------------|
| `e4b8de1` | 2026-06-30 | BUILD-001 Configure JACOCO to run with test and add tests for security                                                   |
| `8247ab7` | 2026-06-30 | BUILD-001 Fix Script build                                                                                               |
| `facfd43` | 2026-06-30 | BUILD-001 Fix frontend build ( huge style and duplicated package                                                         |
| `9850a07` | 2026-06-30 | BUILD-001 implement unit tests for SavedFiltersService admin guard and interceptor                                       |
| `7dceab0` | 2026-06-30 | BUILD-001 Fix docker build frontend backend and nginx                                                                    |
| `7eb3b8e` | 2026-06-30 | BUILD-001 Implement utility specs for  filter-chips-builder.ts gene-filter.utils.ts and date-formatter.ts                |
| `34b9349` | 2026-06-30 | BUILD-001 Implement utility specs for limit-selector, compare-length-histogram, NotificationService and ErrorInterceptor |
| `c8c7e5f` | 2026-06-30 | Merge pull request #19 from maaydi/implementation/BUILD-001                                                              |

</details>

---

### Epoch II: Pluggable Multi-Data Provider & Remote UniProtKB (July – August 2026)

#### [ARCHITECTURE] `REFACTOR-001` — Pluggable Data Provider Architecture & Provider Dispatchers

- **Dates Active:** [2026-07-02 to 2026-07-03]
- **Commits:** 8 commits (`715c2e2..a612fc0`)
- **Merged PR:** #20
- **Overview:** Refactored backend data access layer into a pluggable provider pattern, decoupling data consumption from
  the underlying PostgreSQL storage to prepare for remote data sources.

**Backend Engineering:**

- Created `DataProvider` interface hierarchy abstracting gene querying and analytics calculation.
- `PostgresDataProvider` isolating PostgreSQL-specific queries, specifications, and materialized views.
- `DataProviderDispatcher` routing requests dynamically based on provider context.
- Moved shared DTOs into modular package structures.

**Quality Assurance & Testing:**

- Refactored test suite to verify provider dispatching and fallbacks.

<details>
<summary><strong>Commit History (8 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                  |
|:---------:|:----------:|:--------------------------------------------------------------------------------|
| `715c2e2` | 2026-07-02 | REFACTOR-001 Move common dto to model package                                   |
| `53e2110` | 2026-07-02 | REFACTOR-001 Move analytics to postgres provider                                |
| `f49f78e` | 2026-07-02 | REFACTOR-001 Move gene and saved filter to postgres provider                    |
| `146f2a0` | 2026-07-02 | REFACTOR-001 Refactor Analytics to use provider dispatchers                     |
| `24bc7b8` | 2026-07-03 | REFACTOR-001 Revert saved filter as it is related to user not to data providers |
| `15ef804` | 2026-07-03 | REFACTOR-001 Refactor GeneService to use provider dispatchers                   |
| `77ad63f` | 2026-07-03 | REFACTOR-001 Documentation                                                      |
| `a612fc0` | 2026-07-03 | Merge pull request #20 from maaydi/implementation/REFACTOR-001                  |

</details>

---

#### [INTEGRATION] `RDF-001` — UniProt KnowledgeBase Remote API Client & Facet Models

- **Dates Active:** [2026-07-03 to 2026-07-11]
- **Commits:** 20 commits (`b5dec77..a33f19a`)
- **Merged PR:** #21
- **Overview:** Explored and implemented client integration with the official UniProtKB REST API, establishing data
  models, facet extraction, and mapping to internal domain models.

**Backend Engineering:**

- `UniprotKbRestClient` using Spring 6 / Boot 4 HTTP Client with connection pooling and timeouts.
- OpenAPI definitions and models matching UniProtKB REST JSON schemas.
- Mapping layer translating UniProtKB JSON responses to internal `ProteinEntry` representations.
- Extracted facet distributions (organism, review status, gene ontology) directly from UniProtKB search responses.

**Quality Assurance & Testing:**

- Mock REST server tests validating response deserialization and schema compatibility.

<details>
<summary><strong>Commit History (20 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                               |
|:---------:|:----------:|:-----------------------------------------------------------------------------|
| `b5dec77` | 2026-07-03 | RDF-001 Start feature                                                        |
| `c278950` | 2026-07-04 | RDF-001 Create UniprotKB protein response                                    |
| `79a5410` | 2026-07-04 | RDF-001 Create UniprotKB RestClient                                          |
| `dcded2b` | 2026-07-06 | RDF-001 Define Analytics KPIs from uniprot endpoint                          |
| `9585703` | 2026-07-06 | RDF-001 Facet response from uniprot                                          |
| `2789554` | 2026-07-06 | RDF-001 config uniprot endpoint rest api                                     |
| `5f7c27d` | 2026-07-08 | RDF-001 Mapper UniProtKB api to ProteinEntry Postgres                        |
| `79f313f` | 2026-07-08 | RDF-001 create Api Client for UniprotKB                                      |
| `8f36f46` | 2026-07-08 | RDF-001 Fix Gene                                                             |
| `cd79fca` | 2026-07-08 | RDF-001 Fix GoTerm relation inside ProteinEntry and add resolver             |
| `63bd543` | 2026-07-08 | RDF-001 Add Job to load data from api                                        |
| `1a3715d` | 2026-07-08 | RDF-001 add app start runnable to automatically start/recover import api job |
| `7d405a9` | 2026-07-08 | RDF-001 refactor batch to job package                                        |
| `8d36070` | 2026-07-08 | RDF-001 Configure APP_DIR                                                    |
| `0ef76f8` | 2026-07-09 | RDF-001 Refactor                                                             |
| `5d9c1e0` | 2026-07-09 | RDF-001 make remote api import run from service                              |
| `51de7eb` | 2026-07-10 | RDF-001 Update documentation                                                 |
| `9757326` | 2026-07-10 | RDF-001 Add OpenAPI definition for uniprot rest client                       |
| `d4b203f` | 2026-07-10 | RDF-001 Add GeneSpecification for uniprot kb and fix unit tests              |
| `a33f19a` | 2026-07-11 | Merge pull request #21 from maaydi/implementation/RDF-001                    |

</details>

---

#### [HYBRID DATA] `REMOTE-001` — Live UniProtKB Remote Provider & Seamless Provider Switching

- **Dates Active:** [2026-07-19 to 2026-08-10]
- **Commits:** 47 commits (`b8e7ab6..907ac52`)
- **Merged PR:** #22
- **Overview:** Implemented fully-functional live UniProtKB remote data provider, enabling users to explore the live
  global UniProt database directly alongside their local dataset.

**Backend Engineering:**

- `UniprotKbGeneService` implementing `GeneService` contract over UniProtKB REST APIs.
- Support for UniProtKB cursor-based pagination and search query translation.
- Redis caching layer specifically tuned for UniProtKB remote query results.
- Accession auto-complete suggestions and remote CSV export.

**Frontend & UX Engineering:**

- `DataProviderService` and HTTP interceptor managing active provider selection (Local vs UniProtKB Live).
- UI toggle in header/filter allowing users to switch between local dataset and remote UniProtKB.
- Adapted AG Grid pagination and total count display to handle cursor-based remote pagination.
- Synchronized upload progress tracking with import history.

**Quality Assurance & Testing:**

- Unit and integration tests for remote search query construction, cursor handling, and UI toggle.

<details>
<summary><strong>Commit History (47 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                                                                                                                                      |
|:---------:|:----------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `b8e7ab6` | 2026-07-19 | REMOTE-001 refactor code                                                                                                                                                                                            |
| `ac270bb` | 2026-07-26 | REMOTE-001 refactor code add builder to DTO                                                                                                                                                                         |
| `0778c36` | 2026-07-26 | REMOTE-001 refactor code refactor kb api and job                                                                                                                                                                    |
| `3425330` | 2026-07-26 | REMOTE-001 refactor code add dto mapper and cache service for pagination                                                                                                                                            |
| `74d3a73` | 2026-07-26 | REMOTE-001 modify gene service to search gene details by accession instead of db row id for extensiability with rest api                                                                                            |
| `bfdc2cf` | 2026-07-26 | REMOTE-001 modify gene service to search gene details by accession instead of db row id for extensiability with rest api - frontend                                                                                 |
| `45ca52c` | 2026-07-26 | REMOTE-001 UniprotKbGeneService export csv                                                                                                                                                                          |
| `f9236c1` | 2026-07-27 | REMOTE-001 Audit fix frontend                                                                                                                                                                                       |
| `a17d6f6` | 2026-07-27 | REMOTE-001 fix interceptor to skip errors for login/logout and refresh endpoint when setting authorization headers                                                                                                  |
| `9837d3f` | 2026-07-27 | REMOTE-001 AutoComplete : Add repository functions for suggestion                                                                                                                                                   |
| `6e12ed7` | 2026-07-27 | REMOTE-001 AutoComplete : Add postgres suggestion strategy                                                                                                                                                          |
| `fe21f51` | 2026-07-27 | REMOTE-001 AutoComplete : Add postgres suggestion controller and service                                                                                                                                            |
| `5a67869` | 2026-07-27 | REMOTE-001 Fix frontend test after changing gene detail by accession not id                                                                                                                                         |
| `bcc0a89` | 2026-07-29 | REMOTE-001 Implement auto-complete in frontend                                                                                                                                                                      |
| `4b3fd74` | 2026-07-29 | REMOTE-001 Remove load keyword api and its usage                                                                                                                                                                    |
| `63a50c2` | 2026-07-29 | REMOTE-001 Fix cross-reference regression in specification                                                                                                                                                          |
| `19241f2` | 2026-07-29 | REMOTE-001 Use Autocomplete for dedicated frontend filters inputs                                                                                                                                                   |
| `336d60d` | 2026-07-29 | REMOTE-001 Documentation postgres implementation for autocomplete                                                                                                                                                   |
| `5700e8e` | 2026-07-29 | REMOTE-001 Implement Rest client for cross-reference database                                                                                                                                                       |
| `2492f17` | 2026-07-29 | REMOTE-001 Refactor UniprotKb rest client to support light search for autocomplete                                                                                                                                  |
| `5bf5304` | 2026-07-29 | REMOTE-001 [Intermediate] Implement UniprotKb suggestion components                                                                                                                                                 |
| `982f942` | 2026-07-31 | REMOTE-001 FeatureType autocomplete from search field config api                                                                                                                                                    |
| `b0ac7d7` | 2026-07-31 | REMOTE-001 GoTerm and Keywords autocomplete from suggester endpoint                                                                                                                                                 |
| `b03716b` | 2026-07-31 | REMOTE-001 Implement suggestions with uniprotkb rest and its dependencies                                                                                                                                           |
| `7dacefd` | 2026-07-31 | REMOTE-001 Fix Gene specification for KB API                                                                                                                                                                        |
| `e5d365d` | 2026-07-31 | REMOTE-001 Unit tests autocomplete                                                                                                                                                                                  |
| `e0c6a56` | 2026-07-31 | REMOTE-001 Documentation                                                                                                                                                                                            |
| `8007baa` | 2026-07-31 | REMOTE-001 Documentation update plan and journal                                                                                                                                                                    |
| `f0b8626` | 2026-08-01 | REMOTE-001 Frontend add data provider service and interceptor to switch between providers                                                                                                                           |
| `e36395b` | 2026-08-01 | REMOTE-001 Frontend Retry search when toggle local/remote and reset page when filter changes or size changes to adapt cursor logic                                                                                  |
| `138e1a8` | 2026-08-01 | REMOTE-001 Backend refactor redis cache to support uniprot kb caches                                                                                                                                                |
| `69c668e` | 2026-08-01 | REMOTE-001 Backend Fix accession suggestion - filter entries by type to remove inactive entries - update totalResult after each api call to be up to date                                                           |
| `67bae10` | 2026-08-01 | REMOTE-001 Frontend fix test after changing pagination logic                                                                                                                                                        |
| `4d6de6c` | 2026-08-05 | REMOTE-001 Fix warnings in application startup            - add equals to OrganismCountId            - Add concret class for delegation item reader            - remove unecessary config in application properties |
| `b09eae9` | 2026-08-05 | REMOTE-001 Add UniprotQueryParams to centralize Query params check and creation                                                                                                                                     |
| `254cb74` | 2026-08-05 | REMOTE-001 Frontend Persist data provider in localStorage                                                                                                                                                           |
| `c823db5` | 2026-08-05 | REMOTE-001 Frontend Enhance Toast when logout failed                                                                                                                                                                |
| `f0d8a2d` | 2026-08-05 | REMOTE-001 Frontend Add import remote data in import tab                                                                                                                                                            |
| `f446940` | 2026-08-07 | REMOTE-001 Backend add saved filter id to import remote data so import remote will import custom filter data                                                                                                        |
| `e2462c2` | 2026-08-07 | REMOTE-001 Backend fix unit tests                                                                                                                                                                                   |
| `0c21888` | 2026-08-07 | REMOTE-001 frontend enable remote import for saved filter                                                                                                                                                           |
| `0cbff66` | 2026-08-09 | REMOTE-001 backend change jobfailermarker by  jobrecorvery service                                                                                                                                                  |
| `4301df1` | 2026-08-09 | REMOTE-001 backend Adapt remote api job to work like local file job                                                                                                                                                 |
| `42ca5af` | 2026-08-09 | REMOTE-001 frontend refactor import source                                                                                                                                                                          |
| `9caad54` | 2026-08-10 | REMOTE-001 frontend Synchronize load history with current upload progress                                                                                                                                           |
| `14fe445` | 2026-08-10 | REMOTE-001 Update readme version 2.0                                                                                                                                                                                |
| `907ac52` | 2026-08-10 | Merge pull request #22 from maaydi/implementation/remote-001                                                                                                                                                        |

</details>

---

### Epoch III: Strategic Architecture & Distributed Microservices Migration (August – September 2026)

#### [SPECIFICATION] `STRATEGIC-DOCS` — Strategic Architecture Backlog (STRUCT-001, NLQ-001, ARCH-002, OPS-002)

- **Dates Active:** [2026-08-12 to 2026-08-12]
- **Commits:** 1 commits (`fefff3f..fefff3f`)
- **Merged PR:** #23, #25, #26, #28, #29
- **Overview:** Defined architectural blueprints and specifications for future strategic capabilities:

**Backend Engineering:**

- `STRUCT-001`: Architectural design for 3D Protein Viewer integrating Mol* / PDBe visualizer.
- `NLQ-001`: Natural Language Query & AI Summarization Engine design specification.
- `ARCH-002`: Protein Catalog Ownership & Database-per-Service boundary specification.
- `OPS-002`: Migration architecture specification from Docker Compose to Kubernetes (Minikube).

<details>
<summary><strong>Commit History (1 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                      |
|:---------:|:----------:|:--------------------------------------------------------------------|
| `fefff3f` | 2026-08-12 | Merge pull request #23 from maaydi/documentation/Strategic-Features |

</details>

---

#### [FEATURE] `STRUCT-001` — STRUCT-001

- **Dates Active:** [2026-08-12 to 2026-08-12]
- **Commits:** 2 commits (`c8d5f82..1f21216`)
- **Overview:**

<details>
<summary><strong>Commit History (2 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                           |
|:---------:|:----------:|:---------------------------------------------------------|
| `c8d5f82` | 2026-08-12 | STRUCT-001 Structural Context Module (3D Protein Viewer) |
| `1f21216` | 2026-08-12 | STRUCT-001 Structural Context Module (3D Protein Viewer) |

</details>

---

#### [FEATURE] `NLQ-001` — NLQ-001

- **Dates Active:** [2026-08-12 to 2026-08-12]
- **Commits:** 1 commits (`4ce7573..4ce7573`)
- **Overview:**

<details>
<summary><strong>Commit History (1 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                           |
|:---------:|:----------:|:---------------------------------------------------------|
| `4ce7573` | 2026-08-12 | NLQ-001 Natural Language Query & AI Summarization Engine |

</details>

---

#### [DISTRIBUTED SYSTEMS] `ARCH-001` — Microservices Migration via Strangler Fig Pattern

- **Dates Active:** [2026-08-12 to 2026-09-06]
- **Commits:** 107 commits (`1a7a841..868aa74`)
- **Merged PR:** #24
- **Overview:** Executed comprehensive architectural migration from monolithic application to distributed microservices
  architecture using the Strangler Fig pattern without disrupting existing workflows.

**Backend Engineering:**

- Decomposed monolith into multi-module Maven structure (`backend/`):
-
    - `dashboard`: Monolith legacy core running during migration.
-
    - `infrastructure/api-gateway`: Spring Cloud Gateway with routing, rate limiting, and JWT forwarding.
-
    - `infrastructure/discovery-server`: Netflix Eureka service discovery server.
-
    - `infrastructure/config-server`: Spring Cloud Config Server managing centralized YAML profiles.
-
    - `libs/common-starter`: Shared Spring Boot starter providing security filters, logging, and metrics.
-
    - `libs/shared-models`: Shared DTOs, events, and constants across all microservices.
-
    - `services/auth-service`: Extracted independent authentication microservice.
-
    - `services/analytics-service`: Extracted analytics and materialized view microservice.
-
    - `services/import-service`: Extracted UniProt batch import microservice.
-
    - `services/export-service`: Extracted export generation microservice.
- Asynchronous event publishing and consumption for materialized view refreshes upon import completion.

**DevOps & Infrastructure:**

- Configured PostgreSQL Primary + Read Replica replication architecture (`docker-compose.infra.yml`).
- Configured Docker Compose service orchestration with network segmentation and discovery dependencies.

**Quality Assurance & Testing:**

- Microservice integration tests, Gateway routing tests, and service-to-service communication tests.

<details>
<summary><strong>Commit History (107 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                                                                                                 |
|:---------:|:----------:|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `1a7a841` | 2026-08-12 | ARCH-001 Microservices Architecture Migration (Strangler Fig)                                                                                                                  |
| `eb66e51` | 2026-08-14 | ARCH-001 Move backend project into backend/dashboard folder                                                                                                                    |
| `4c2a1d5` | 2026-08-14 | ARCH-001 Move backend project into backend/dashboard folder Fix build                                                                                                          |
| `447181d` | 2026-08-14 | ARCH-001 Move backend project into backend/dashboard folder Fix banner version                                                                                                 |
| `353ea8f` | 2026-08-14 | ARCH-001 Refactor code to run docker compose                                                                                                                                   |
| `554a2ce` | 2026-08-14 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.1 Shared Common Library (`libs/common-starter/`)                                                                                 |
| `a44d367` | 2026-08-14 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.1 Shared Common Library (`libs/common-starter/`) Add common-starter as dependency in dashboard update workflow                   |
| `229e8c0` | 2026-08-14 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.1 Shared Common Library (`libs/common-starter/`) Add common-starter as dependency in dashboard Disable commonConfig for testings |
| `b31380d` | 2026-08-15 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.1 Shared Common Library (`libs/common-starter/`) Use common parent BOM                                                           |
| `2796161` | 2026-08-16 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.1 Shared Common Library (`libs/common-starter/`) Use common parent BOM                                                           |
| `2388124` | 2026-08-16 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.2 Service Discovery — Eureka Server (`infrastructure/discovery-server/`)                                                         |
| `bbffd82` | 2026-08-16 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.2 Service Discovery — Eureka Server (`infrastructure/discovery-server/`) Enable discovery on dashboard application               |
| `439d049` | 2026-08-16 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.2 Service Discovery — Eureka Server (`infrastructure/discovery-server/`) Fix docker compose build                                |
| `e188f50` | 2026-08-16 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.3 Config Server (`infrastructure/config-server/`)                                                                                |
| `2a7638e` | 2026-08-16 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.3 Config Server (`infrastructure/config-server/`)                                                                                |
| `2d4a96c` | 2026-08-16 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.3 Config Server (`infrastructure/config-server/`) define configuration for config server repository                              |
| `5d3733a` | 2026-08-16 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.3 Config Server (`infrastructure/config-server/`) connect to config server from dashboard                                        |
| `4ce1656` | 2026-08-16 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.3 Config Server (`infrastructure/config-server/`) Documentation                                                                  |
| `e05f485` | 2026-08-17 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.4 API Gateway (`infrastructure/api-gateway/`) implement api-gateway                                                              |
| `b6b6f8b` | 2026-08-17 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.4 API Gateway (`infrastructure/api-gateway/`) Add shared-models for future refactoring                                           |
| `b35c8cb` | 2026-08-17 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.4 API Gateway (`infrastructure/api-gateway/`) Fix unit tests                                                                     |
| `7be6abf` | 2026-08-17 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.4 API Gateway (`infrastructure/api-gateway/`) Docker service in `docker-compose.infra.yml` exposing port 8080                    |
| `ee66690` | 2026-08-17 | ARCH-001 PHASE 0 — Infrastructure Bootstrap Fix banner for each service                                                                                                        |
| `dcb38ec` | 2026-08-17 | ARCH-001 PHASE 0 — Infrastructure Bootstrap add logback-spring to each service                                                                                                 |
| `f11672a` | 2026-08-20 | ARCH-001 PHASE 0 — Infrastructure Bootstrap 0.4 API Gateway (`infrastructure/api-gateway/`) journal.md                                                                         |
| `cd53267` | 2026-08-20 | ARCH-001 PHASE 1 — Extract Auth Service 1.1 Service Setup                                                                                                                      |
| `a232e89` | 2026-08-20 | ARCH-001 PHASE 1 — Extract Auth Service 1.2 Database & Schema Flyway migration `V1__auth_schema.sql`                                                                           |
| `f1be408` | 2026-08-20 | ARCH-001 PHASE 1 — Extract Auth Service 1.2 Database & Schema `AppUser` entity                                                                                                 |
| `d98a840` | 2026-08-20 | ARCH-001 PHASE 1 — Extract Auth Service 1.2 Database & Schema `RefreshToken` entity                                                                                            |
| `09e2b57` | 2026-08-20 | ARCH-001 PHASE 1 — Extract Auth Service 1.2 Database & Schema `RefreshToken` entity                                                                                            |
| `28d7ec3` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.2 Database & Schema Integrate common-starter datasource                                                                              |
| `ea2757c` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.2 Database & Schema Add config for config server                                                                                     |
| `a4cd7ac` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.2 Database & Schema Force eureka uri in docker env variable create log folder                                                        |
| `1ddf9c3` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.2 Database & Schema fix spring boot env variables in docker-compose                                                                  |
| `75719d4` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.3 API Implementation                                                                                                                 |
| `ec9b7d7` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.3 API Implementation - Tests                                                                                                         |
| `b033137` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.3 API Implementation - Tests Integrate auth-service with api-gateway                                                                 |
| `f01e696` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.3 API Implementation - Tests Integrate auth-service with api-gateway : adapt filter in auth-service                                  |
| `d791741` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.3 API Implementation - Tests Fix unit tests for auth-service                                                                         |
| `e167560` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.3 API Implementation - Tests Add Constants and UserPrincipal in shared-module                                                        |
| `31874c1` | 2026-08-21 | ARCH-001 PHASE 1 — Extract Auth Service 1.4 Monolith Adaptation Phase 1 — Tests                                                                                                |
| `775831b` | 2026-08-23 | ARCH-001 PHASE 1 — Extract Auth Service 1.4 Monolith Adaptation Update config files for auth-service and api-gateway                                                           |
| `16c8aed` | 2026-08-23 | ARCH-001 PHASE 1 — Extract Auth Service 1.4 Monolith Adaptation Refactor api-gateway to use same jwt config                                                                    |
| `88bd5c2` | 2026-08-23 | ARCH-001 PHASE 1 — Extract Auth Service 1.4 Monolith Adaptation Refactor auth-service to use same jwt config                                                                   |
| `39c35ce` | 2026-08-23 | ARCH-001 PHASE 1 — Extract Auth Service 1.4 Monolith Adaptation Clean dashboard remove jwt utils                                                                               |
| `9cddf92` | 2026-08-23 | ARCH-001 PHASE 1 — Extract Auth Service 1.4 Monolith Adaptation Unit tests                                                                                                     |
| `c644517` | 2026-08-23 | ARCH-001 PHASE 1 — Extract Auth Service 1.4 Monolith Adaptation clockSkewSeconds between services                                                                              |
| `5a69bf4` | 2026-08-23 | ARCH-001 PHASE 1 — Extract Auth Service 1.4 Monolith Adaptation Refactor config files and env file for better readability                                                      |
| `c138149` | 2026-08-23 | ARCH-001 PHASE 1 — Extract Auth Service 1.4 Monolith Adaptation Fix unit tests                                                                                                 |
| `ea297ce` | 2026-08-24 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) 2.1 Service Setup                                                                                 |
| `8932600` | 2026-08-24 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) 2.2 Database & Schema                                                                             |
| `823240e` | 2026-08-24 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) 2.2 Database & Schema Move exception and common protein entries to common-starter                 |
| `bc92e0f` | 2026-08-24 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`)  2.3 API Implementation                                                                           |
| `c28bc13` | 2026-08-24 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) 2.5 Monolith Adaptation                                                                           |
| `9ff9230` | 2026-08-24 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) 2.3 API Implementation                                                                            |
| `91b8e5a` | 2026-08-24 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) add config server files and gateway route                                                         |
| `bde4a65` | 2026-08-24 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) add docker compose service for analytics                                                          |
| `37ab427` | 2026-08-24 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) Routing DataSource: analytics-service uses REPLICA exclusively                                    |
| `8a12d05` | 2026-08-25 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) Use constants to define schemas                                                                   |
| `c8e80ef` | 2026-08-25 | ARCH-001 PHASE 2 — Extract Analytics Service (`services/analytics-service/`) Documentation                                                                                     |
| `0161daf` | 2026-08-25 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 3.1 Service Setup                                                                                       |
| `14d86bd` | 2026-08-25 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Refactor move uniprotkb to common starter                                                               |
| `468157c` | 2026-08-25 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Refactor move uniprotkb to common starter : refactor dashboard                                          |
| `a61453e` | 2026-08-25 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Refactor move SavedFilterDto to common-starter                                                          |
| `9c4918b` | 2026-08-25 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 3.3 Batch Job Migration Refactor move all import batch related classes to import-service                |
| `399c5ac` | 2026-08-25 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 3.3 Batch Job Migration                                                                                 |
| `65b3c19` | 2026-08-26 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 3.3 Batch Job Migration Add cache dependency for import service                                         |
| `7c45803` | 2026-08-26 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 3.3 Batch Job Migration fix Unit test dashboard                                                         |
| `db27e49` | 2026-08-27 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Refactor : move cache to common-starter                                                                 |
| `b2d8868` | 2026-08-27 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Refactor : enable cache in analytics-service                                                            |
| `39674ed` | 2026-08-27 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Refactor : disable cache in auth-service                                                                |
| `cf880f8` | 2026-08-27 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Refactor : enable cache in dashboard                                                                    |
| `b56bdd6` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) import-service unit tests                                                                               |
| `18d8558` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Clean uniprot-api config                                                                                |
| `a6ac6e8` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Add config to import-service                                                                            |
| `02e6629` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 3.4 API Implementation                                                                                  |
| `f4c9620` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Docker Compose for import service                                                                       |
| `a25c867` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) add logs to api-gateway                                                                                 |
| `3196c23` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) clean dashboard service                                                                                 |
| `06ea253` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) fix flyway migration                                                                                    |
| `7b17b4b` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) fix api-gateway config                                                                                  |
| `5751080` | 2026-08-28 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) fix api-gateway config                                                                                  |
| `d6341e9` | 2026-08-29 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) fix configurations and enhance logging                                                                  |
| `a3663d6` | 2026-08-29 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) fix cors backend                                                                                        |
| `9b9c513` | 2026-08-30 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) fix auth frontend                                                                                       |
| `d5e317f` | 2026-08-30 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) fix SecurityFilterChain                                                                                 |
| `d2ecb11` | 2026-08-30 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Create @CurrentUser annotation for webmvc                                                               |
| `dbd5655` | 2026-08-30 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) fix unit tests                                                                                          |
| `9c656fb` | 2026-08-30 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) fix constants call and roles claim                                                                      |
| `1b22cfb` | 2026-08-31 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Fix import service chunck size property                                                                 |
| `03b6066` | 2026-08-31 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Fix schema names for relations                                                                          |
| `f6e6c63` | 2026-08-31 | change db replica config                                                                                                                                                       |
| `6671c43` | 2026-08-31 | ARCH-001 configure replica/primary db                                                                                                                                          |
| `1525c55` | 2026-08-31 | 0.6 Database — PostgreSQL Primary + Replica (`docker-compose.infra.yml`)                                                                                                       |
| `45f0a6b` | 2026-08-31 | 0.6 Database — PostgreSQL Primary + Replica (`docker-compose.infra.yml`)                                                                                                       |
| `ff14b8f` | 2026-09-02 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 3.5 Event Publishing                                                                                    |
| `a2d2c06` | 2026-09-02 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 2.4 Event Consumer in analytics-service                                                                 |
| `88ab6ac` | 2026-09-02 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 2.4 Event Consumer in analytics-service Clean dashboard monolithic                                      |
| `4714ab4` | 2026-09-02 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) 2.4 Event Consumer in analytics-service                                                                 |
| `f3e2fb9` | 2026-09-02 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) MaterializedViewRefreshService Add idempotency Service fix tests                                        |
| `21db9be` | 2026-09-03 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) MaterializedViewRefreshService Add idempotency Service fix tests                                        |
| `18c03a9` | 2026-09-04 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Reorder services and create script to run them sequentially                                             |
| `2f141e7` | 2026-09-05 | ARCH-001 PHASE 3 — Extract Import Service (`services/import-service/`) Fix refresh view bugs                                                                                   |
| `ffd0fc8` | 2026-09-05 | docs(ARCH-001): update journal with recent Phase 3 & infra commits (Aug29-Sep05)                                                                                               |
| `7f0c6ed` | 2026-09-05 | ARCH-001 PHASE 5 — Extract Export Service (`services/export-service/`)                                                                                                         |
| `8c0b14d` | 2026-09-06 | ARCH-001 PHASE 5 — Extract Export Service (`services/export-service/`) Add Application properties                                                                              |
| `868aa74` | 2026-09-06 | Merge pull request #24 from maaydi/implementation/ARCH-001                                                                                                                     |

</details>

---

#### [ARCHITECTURE SPEC] `ARCH-002` — Database-per-Service Architecture & Protein Catalog Ownership Spec

- **Dates Active:** [2026-09-29 to 2026-09-29]
- **Commits:** 3 commits (`5a07336..817c1cf`)
- **Merged PR:** #25, #26
- **Overview:** Formulated specifications and plan for Phase 2 microservices: establishing strict Database-per-Service
  isolation and formal Protein Catalog Domain Ownership.

**Backend Engineering:**

- Defined schema segregation strategy separating auth, catalog, analytics, import, and export databases.
- Outlined event-driven data synchronization contracts.

<details>
<summary><strong>Commit History (3 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                               |
|:---------:|:----------:|:---------------------------------------------------------------------------------------------|
| `5a07336` | 2026-09-29 | ARCH-002 Protein Catalog Ownership & Database-per-Service Enforcement Specification and plan |
| `4d1e39a` | 2026-09-29 | Merge pull request #25 from maaydi/documentation/arch-002                                    |
| `817c1cf` | 2026-09-29 | Merge pull request #26 from maaydi/documentation/update-design-and-agents                    |

</details>

---

#### [KUBERNETES / CLOUD-NATIVE] `OPS-002` — Kubernetes (Minikube) Cloud-Native Migration Specification

- **Dates Active:** [2026-09-30 to 2026-10-04]
- **Commits:** 9 commits (`a7571ab..79901d5`)
- **Merged PR:** #28, #29, #32
- **Overview:** Authored complete Kubernetes deployment specifications and Minikube local orchestration blueprints.

**DevOps & Infrastructure:**

- Kubernetes deployment manifests specifications: Deployments, Services, ConfigMaps, Secrets, Ingress.
- Minikube operational setup guides, resource allocation limits, and persistence volume claims.

<details>
<summary><strong>Commit History (9 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                                |
|:---------:|:----------:|:--------------------------------------------------------------------------------------------------------------|
| `79901d5` | 2026-10-26 | OPS-002 - Documentation                                                                                       |
| `93b2596` | 2026-10-26 | OPS-002 - frontend                                                                                            |
| `f4bd1d3` | 2026-10-02 | OPS-002 Migration from Docker Compose to Kubernetes (Minikube) Step 7 — Business services                     |
| `626cc86` | 2026-10-02 | OPS-002 Migration from Docker Compose to Kubernetes (Minikube) Step 6 — Platform services                     |
| `57d46cc` | 2026-10-02 | OPS-002 Migration from Docker Compose to Kubernetes (Minikube) Step 5 — Messaging & observability             |
| `9808ff7` | 2026-10-02 | OPS-002 Migration from Docker Compose to Kubernetes (Minikube) Step 4 — Data stores                           |
| `0772fe2` | 2026-09-30 | OPS-002 Migration from Docker Compose to Kubernetes (Minikube) Namespace + shared common configmap and secret |
| `59bf94b` | 2026-09-30 | OPS-002 Migration from Docker Compose to Kubernetes (Minikube) - Specification                                |
| `a7571ab` | 2026-09-30 | OPS-002 Migration from Docker Compose to Kubernetes (Minikube) - Specification                                |

</details>

---

### Epoch IV: Visual Export Pipeline, Batch Engine & Modern Store Architecture (September – October 2026)

#### [ADVANCED PIPELINE / HEAD]

`PIPE-001` — Visual Export Pipeline, Batch Export Engine & Modern Signal Store Refactoring

- **Dates Active:** [2026-08-12 to 2026-10-23]
- **Commits:** 109 commits (`a9f9440..b2b2b11`)
- **Merged PR:** #27 (Backend) & Ongoing HEAD
- **Overview:** Implemented asynchronous Visual Export Pipeline and Batch Export Engine, accompanied by major frontend
  refactorings adopting store-based signal architectures, layout optimizations, and remote UniProtKB enhancements.

**Backend Engineering:**

- Database schema migrations for `export_pipeline` and `export_job_execution` tables.
- Entities and repositories for asynchronous export pipelines and execution logs.
- Multi-format format writers: CSV, TSV, JSON, and FASTA sequence files.
- `ExportFileStorageService` managing generated export artifacts on disk with retention policies.
- Batch export execution engine with thread pool management and failure resilience.
- Fixed evidence level parsing bug in UniProtKB remote response mappers.

**Frontend & UX Engineering:**

- `ExportPipelineComponent` and visual step-by-step export workflow.
- `ExportPipelineCompletionMonitor` polling and alerting when large background exports complete.
- Refactored `ImportAdminStore` and `SavedFilterStore` to cleanly encapsulate state and eliminate redundant API calls.
- Fixed AG Grid table layout and column widths to fit cleanly on FHD (1080p) screens without awkward horizontal
  scrolling.
- Fixed double invocation bug of `searchGenes` during pagination updates; mapped grid row IDs strictly to protein
  accession numbers.
- Centralized toast notification messaging across analytics and dashboard views.

**DevOps & Infrastructure:**

- Flyway migrations for export pipeline tables in PostgreSQL.

**Quality Assurance & Testing:**

- Extensive backend unit and integration tests for `export-service`, format writers, and pipeline controllers.
- Frontend store service and component unit tests updated and passing.

<details>
<summary><strong>Commit History (109 commits)</strong></summary>

|   Hash    |    Date    | Commit Message                                                                                                                |
|:---------:|:----------:|:------------------------------------------------------------------------------------------------------------------------------|
| `a9f9440` | 2026-08-12 | PIPE-001 Visual Export Pipeline & Batch Export Engine                                                                         |
| `f4d1159` | 2026-09-06 | PIPE-001 Start Implementation - validate questions                                                                            |
| `9d01859` | 2026-09-06 | PIPE-001 - Create DB migration for `export_pipeline` and `export_job_execution` tables                                        |
| `ab49f37` | 2026-09-06 | PIPE-001 - Implement entity layer (`ExportPipeline`, `ExportJobExecution`)                                                    |
| `80cec75` | 2026-09-06 | PIPE-001 - Implement repository layer                                                                                         |
| `eccbd47` | 2026-09-06 | PIPE-001 - Implement DTOs and MapStruct mappers                                                                               |
| `2f5f22b` | 2026-09-06 | PIPE-001 - Implement `ExportFileStorageService`                                                                               |
| `2af745b` | 2026-09-11 | PIPE-001 - Backend — Format Writers                                                                                           |
| `422a59a` | 2026-09-11 | PIPE-001 - Backend — Format Writers Tests                                                                                     |
| `aacfc3b` | 2026-09-11 | PIPE-001 - Backend — Spring Batch Job Configuration Refactor gene controller add count                                        |
| `55ebc86` | 2026-09-11 | PIPE-001 - Backend — Spring Batch Job Configuration Add Gene feign client in export-service                                   |
| `f3c1773` | 2026-09-11 | PIPE-001 - Backend — Spring Batch Job Configuration Step 1: validateAndEstimateStep (Tasklet)                                 |
| `dd751f1` | 2026-09-13 | PIPE-001 - Backend — Spring Batch Job Configuration Refactor import-job                                                       |
| `4cdae61` | 2026-09-13 | PIPE-001 - Backend — Spring Batch Job Configuration Refactor import-job - fix unit tests                                      |
| `c94745c` | 2026-09-23 | PIPE-001 - Backend — Spring Batch Job Configuration Refactor ProteinDetailDto to support ExportFormatSerializable             |
| `f9cd80d` | 2026-09-24 | PIPE-001 - Backend — Spring Batch Job Configuration Remove CsvWriter and its tests                                            |
| `eaa8129` | 2026-09-24 | PIPE-001 - Backend — Spring Batch Job Configuration Move GeneMapper to common-starter                                         |
| `5cb9011` | 2026-09-24 | PIPE-001 - Backend — Spring Batch Job Configuration Step 2: `exportChunkStep` (chunk-oriented)                                |
| `6ecb06c` | 2026-09-24 | PIPE-001 - Backend — Spring Batch Job Configuration Step 3: `assembleAndFinalizeStep` (Tasklet)                               |
| `33f0d52` | 2026-09-24 | PIPE-001 - Backend — Spring Batch Job Configuration Job listener: `ExportJobListener` (implements `JobExecutionListener`)     |
| `7366728` | 2026-09-24 | PIPE-001 - Backend — Spring Batch Job Configuration Centralize repository access in ExportPipelineService                     |
| `bd605ec` | 2026-09-25 | PIPE-001 - Backend — Backend — Batch Components Readers and unit tests                                                        |
| `3e5e800` | 2026-09-25 | PIPE-001 - Backend — Backend — Batch Components ExportItemProcessor                                                           |
| `02cb152` | 2026-09-25 | PIPE-001 - Backend — Backend — Batch Components ExportItemWriter                                                              |
| `f736e33` | 2026-09-25 | PIPE-001 - Backend — Backend — Batch Components Refactor dashboard : remove duplicated providers define enum for DataProvider |
| `f963184` | 2026-09-25 | PIPE-001 - Backend — Backend — Batch Components ExportPipelineService Fix tests for export pipeline                           |
| `d73e763` | 2026-09-25 | PIPE-001 - Backend — Backend — Batch Components clean dashboard export-related tests                                          |
| `ee8c902` | 2026-09-25 | PIPE-001 - Backend — Backend — Batch Components remove duplicated PagedResponse                                               |
| `843351a` | 2026-09-25 | PIPE-001 - Backend — Service Layer                                                                                            |
| `9c6c4a4` | 2026-09-26 | PIPE-001 - Backend — Service Layer ExportJobStepTracker to get current step                                                   |
| `444316d` | 2026-09-26 | PIPE-001 - Backend — Service Layer downloadUrl, delete pipeline, retry pipeline                                               |
| `674e9af` | 2026-09-26 | PIPE-001 - Backend — Service Layer Returns all possible export fields with metadata for the field picker                      |
| `6147d83` | 2026-09-26 | PIPE-001 - Backend — Controller                                                                                               |
| `63e835e` | 2026-09-26 | PIPE-001 - Backend — Controller                                                                                               |
| `38ee3c0` | 2026-09-26 | PIPE-001 - Backend — Audit & Cleanup ExportCleanupJob                                                                         |
| `984410a` | 2026-09-26 | PIPE-001 - Backend — Docker compose                                                                                           |
| `94a8cfa` | 2026-09-26 | PIPE-001 - Backend — Docker compose Remove cycle in dependencies                                                              |
| `8406c8b` | 2026-09-26 | PIPE-001 - Backend — Docker compose Remove cycle in dependencies                                                              |
| `c9871e2` | 2026-09-27 | Merge remote-tracking branch 'origin/implementation/PIPE-001' into implementation/PIPE-001                                    |
| `f3a715e` | 2026-09-27 | PIPE-001 - Backend — Docker compose Remove cycle in dependencies                                                              |
| `c3ef187` | 2026-09-27 | PIPE-001 - Backend — Docker compose Configure api-gateway.yml with export-service                                             |
| `3f41ab1` | 2026-09-27 | PIPE-001 - Backend — Docker compose Add deserializer to ExportFormat                                                          |
| `fd75032` | 2026-09-27 | PIPE-001 - Documentation and logging for import-service                                                                       |
| `d8cf1f1` | 2026-09-27 | PIPE-001 - Documentation and logging for export-service                                                                       |
| `728aaa8` | 2026-09-27 | PIPE-001 - Documentation and logging for auth-service                                                                         |
| `14bd372` | 2026-09-27 | PIPE-001 - Documentation and logging for analytics-service                                                                    |
| `b07ec89` | 2026-09-27 | PIPE-001 - Documentation and logging for common-starter                                                                       |
| `736d2f3` | 2026-09-27 | PIPE-001 - Documentation and logging for dashboard                                                                            |
| `a660868` | 2026-09-27 | PIPE-001 - Fix transactional problem in step and decider                                                                      |
| `f8f9b24` | 2026-09-27 | PIPE-001 - fix data provider in call gene-service for correct count                                                           |
| `b34debb` | 2026-09-27 | PIPE-001 - Backend - Unit tests                                                                                               |
| `0aa3a00` | 2026-09-27 | PIPE-001 - Backend - Integration test                                                                                         |
| `6cc2b20` | 2026-09-27 | PIPE-001 - Backend - Integration tests                                                                                        |
| `1f5b4c7` | 2026-09-27 | backend maven run commands skill                                                                                              |
| `0d199e1` | 2026-09-27 | Java and Angular agents skills                                                                                                |
| `6f7da3b` | 2026-09-27 | PIPE-001 - Backend - Import-service tests                                                                                     |
| `d1f2b96` | 2026-09-27 | PIPE-001 - Backend - Auth-service tests                                                                                       |
| `a6cb2f2` | 2026-09-27 | PIPE-001 - Backend - Analytics-service tests                                                                                  |
| `c6a21a7` | 2026-09-27 | PIPE-001 - Backend - Optimize import                                                                                          |
| `31dca34` | 2026-09-29 | Update Design system quick reference                                                                                          |
| `3d08c98` | 2026-09-29 | Add agent for development java and angular                                                                                    |
| `fa319da` | 2026-09-29 | Add skill backend-maven-module to simplify maven command for the project development                                          |
| `24b824e` | 2026-09-29 | Add git-commit-instructions                                                                                                   |
| `ab14707` | 2026-09-29 | Merge branch 'refs/heads/main' into implementation/PIPE-001                                                                   |
| `ec28826` | 2026-09-29 | Documentation - Security owasp instructions                                                                                   |
| `9ee3fb1` | 2026-09-29 | PIPE-001 Update import and export directory for consistency                                                                   |
| `b4d3d36` | 2026-09-29 | PIPE-001 clean dashboard clean image                                                                                          |
| `2354b29` | 2026-09-29 | PIPE-001 fix format triple cote to one cote                                                                                   |
| `4a803a5` | 2026-09-29 | PIPE-001 refactor get list of pipelines                                                                                       |
| `dedac00` | 2026-09-29 | PIPE-001 Remove isExpired_exactlyNow_returnsTrue test                                                                         |
| `15ac774` | 2026-09-29 | PIPE-001 Finish backend implementation                                                                                        |
| `7dd0302` | 2026-09-29 | Merge pull request #27 from maaydi/implementation/PIPE-001 backend implementation                                             |
| `45c074c` | 2026-09-30 | PIPE-001 Add script to simplify build single service and restart it in docker compose                                         |
| `00e595d` | 2026-10-04 | Merge branch 'refs/heads/main' into implementation/PIPE-001                                                                   |
| `4d2db21` | 2026-10-06 | PIPE-001 - Frontend — Models                                                                                                  |
| `792dde7` | 2026-10-06 | PIPE-001 - Frontend — Service                                                                                                 |
| `18e4127` | 2026-10-08 | PIPE-001 - Frontend — Component Refactor chips for active filters, change export csv to export data button                    |
| `a137c3c` | 2026-10-08 | PIPE-001 - Frontend — Component Refactor chips for active filters : readonly chips                                            |
| `308dfc8` | 2026-10-09 | PIPE-001 - Frontend — Component Refactor chips for active filters : readonly chips                                            |
| `6a257a6` | 2026-10-09 | PIPE-001 - Frontend — Enhance table load and remove deprecated ag-grid styles                                                 |
| `b78be72` | 2026-10-09 | PIPE-001 - Frontend — ExportPipelineWizardComponent                                                                           |
| `c94238f` | 2026-10-09 | PIPE-001 - Backend - Fix import decider                                                                                       |
| `1c3c974` | 2026-10-16 | PIPE-001 - Frontend - Step 2 — Field Selection                                                                                |
| `90ccc6c` | 2026-10-16 | PIPE-001 - Frontend - Step 1 — Filter Review , add save filter                                                                |
| `3fab588` | 2026-10-16 | PIPE-001 - Frontend - Step 3 — Format Selection                                                                               |
| `7ae820c` | 2026-10-16 | PIPE-001 - Frontend - Step 4 — Summary & Confirm                                                                              |
| `dac4e01` | 2026-10-16 | PIPE-001 - Frontend - add provider header to export                                                                           |
| `d0e83fd` | 2026-10-16 | PIPE-001 - Frontend - Refactor analytics to centralize message for future I18n use                                            |
| `6061361` | 2026-10-16 | PIPE-001 - Frontend - Refactor dashboard to centralize message for future I18n use                                            |
| `82688f9` | 2026-10-16 | PIPE-001 - Frontend - Refactor auth to centralize message for future I18n use                                                 |
| `94f67fc` | 2026-10-16 | PIPE-001 - Frontend - Refactor gene-detail to centralize message for future I18n use                                          |
| `eb6493d` | 2026-10-16 | PIPE-001 - Frontend - Refactor gene filter and autocomplete to centralize message for future I18n use                         |
| `c404ce9` | 2026-10-16 | PIPE-001 - Frontend - Refactor import-admin to centralize message for future I18n use                                         |
| `27b523d` | 2026-10-16 | PIPE-001 - Frontend - Refactor saved-filters to centralize message for future I18n use                                        |
| `e766463` | 2026-10-16 | PIPE-001 - Frontend - Refactor shared components to centralize message for future I18n use                                    |
| `4ce53ab` | 2026-10-22 | PIPE-001 - Backend - Refactor UniProtPostgresItemReader for improved page handling and add unit tests                         |
| `e97e2a4` | 2026-10-22 | PIPE-001 - Frontend — ExportPipelineListComponent                                                                             |
| `e1827c2` | 2026-10-22 | PIPE-001 - Frontend — ExportPipelineListComponent Add to navbar                                                               |
| `f05bf69` | 2026-10-22 | PIPE-001 - Frontend — Refactor dashboard description for local DB                                                             |
| `d73ffdb` | 2026-10-22 | PIPE-001 - Frontend — Refactor Import admin - Create a store service to encapsulate history logic                             |
| `610fc70` | 2026-10-22 | PIPE-001 - Frontend — Refactor Import admin - Enhance style for better UX                                                     |
| `22b69b2` | 2026-10-22 | PIPE-001 - Frontend — Refactor Saved filters style for better UX                                                              |
| `d92303a` | 2026-10-22 | PIPE-001 - Frontend — Add ExportPipelineCompletionMonitor Fix export path and navbar name                                     |
| `ca4a755` | 2026-10-22 | PIPE-001 - Frontend — Fix Tests after refactoring - Finish implementation                                                     |
| `de3cbfa` | 2026-10-23 | PIPE-001 - Frontend — Fix Gene table layout to fit into FHD screen                                                            |
| `9df35cf` | 2026-10-23 | PIPE-001 - Backend - Tests add TODO for UniprotKb to enhance UX                                                               |
| `72a8b29` | 2026-10-23 | PIPE-001 - Frontend - Fix double call of searchGenes when updating pagination - Fix RowId to map to accession                 |
| `3b520df` | 2026-10-23 | PIPE-001 - Backend - Fix evidenceLevel bug in uniprotKb Mappers                                                               |
| `b2b2b11` | 2026-10-23 | PIPE-001 - Update Readme.md                                                                                                   |

</details>

---
