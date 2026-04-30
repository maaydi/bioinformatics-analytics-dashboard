# Bioinformatics Analytics Dashboard

A production-grade full-stack application for exploring, visualizing, and analyzing
**UniProt protein / gene data** — built with **Spring Boot 3**, **Angular**, and **PostgreSQL**.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Repository Structure](#3-repository-structure)
4. [Architecture](#4-architecture)
5. [Key Features](#5-key-features)
6. [Domain Model (Summary)](#6-domain-model-summary)
7. [REST API (Summary)](#7-rest-api-summary)
8. [Security Model](#8-security-model)
9. [Development Setup](#9-development-setup)
10. [Docker / Production Setup](#10-docker--production-setup)
11. [Spec-Driven Development Workflow](#11-spec-driven-development-workflow)
12. [MVP Roadmap](#12-mvp-roadmap)
13. [Non-Functional Requirements](#13-non-functional-requirements)
14. [Contributing](#14-contributing)

---

## 1. Project Overview

The Bioinformatics Analytics Dashboard allows scientists and researchers to:

- **Import** UniProt `.dat` / `.tsv` flat files (up to 2 GB) via an admin pipeline powered by Spring Batch.
- **Explore** the full UniProtKB dataset through a filterable, sortable, paginated table.
- **Visualize** protein distributions via histograms, pie charts, bar charts, and scatter plots.
- **Inspect** individual protein entries in detail (sequence, features, GO terms, cross-references, publications).
- **Export** filtered datasets as CSV files.
- **Save** and reload filter combinations across sessions.

> **Authoritative specification:** all functional requirements live in `documentation/`.  
> No code should diverge from those specs. See [§11](#11-spec-driven-development-workflow).

---

## 2. Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| **Frontend** | Angular (latest) | Standalone components, reactive forms |
| **UI Library** | Angular Material + AG Grid + ECharts | Tables, charts, layout |
| **Backend** | Spring Boot 3 | Java 21, layered architecture |
| **Import Pipeline** | Spring Batch | Chunk-oriented, async progress |
| **Database** | PostgreSQL 16 | JSONB, `tsvector`, materialized views, GIN indexes |
| **ORM** | Spring Data JPA + Hibernate | JPA Specifications for dynamic filters |
| **Migrations** | Flyway | Versioned SQL scripts |
| **Mapping** | MapStruct | Compile-time entity ↔ DTO mapping |
| **Auth** | Spring Security + JWT (HS256) | Access token 1 h, refresh token 24 h |
| **Containerization** | Docker + Docker Compose | Dev and prod profiles |
| **Build** | Maven (backend) · npm / Angular CLI (frontend) | |

---

## 3. Repository Structure

```text
bioinformatics-analytics-dashboard/
│
├── README.md                  ← You are here
├── .gitignore
├── .env.example               ← Copy to .env and fill in secrets
├── docker-compose.yml         ← Full stack: backend + frontend + postgres
│
├── documentation/             ← AUTHORITATIVE SPEC — read before coding
│   ├── overview.md            ← User stories, NFRs, authorization matrix, roadmap
│   ├── api-contract.md        ← REST endpoint contracts (schemas, status codes)
│   ├── domain-model.md        ← Database DDL, indexes, materialized views
│   ├── validation-rules.md    ← All DTO / import validation rules
│   ├── glossary.md            ← Domain term definitions
│   ├── data.md                ← UniProt data format reference
│   ├── project-structure.md   ← Folder conventions
│   ├── constitution.md        ← Coding standards and principles
│   └── implementation/        ← Per-ticket implementation journals (generated during dev)
│
├── backend/                   ← Spring Boot 3 application
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/bioinformatics/dashboard/
│       │   ├── DashboardApplication.java
│       │   ├── config/        ← Security, Web, JPA, Batch configs
│       │   ├── security/      ← JWT filter, UserDetailsService
│       │   ├── exception/     ← GlobalExceptionHandler, custom exceptions
│       │   ├── gene/          ← Feature: protein/gene CRUD + search
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── repository/
│       │   │   ├── specification/
│       │   │   ├── mapper/
│       │   │   ├── dto/
│       │   │   └── entity/
│       │   ├── analytics/     ← Feature: materialized-view chart endpoints
│       │   ├── admin/         ← Feature: import job management
│       │   ├── savedfilter/   ← Feature: saved filter sets
│       │   ├── auth/          ← Feature: login / refresh JWT
│       │   └── batch/         ← Spring Batch: UniProt import pipeline
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/  ← Flyway V1__, V2__ SQL scripts
│       └── test/
│
├── frontend/                  ← Angular application
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig.json
│   ├── proxy.conf.json        ← Dev proxy: /api → localhost:8080
│   └── src/
│       ├── app/
│       │   ├── core/          ← Auth service, HTTP interceptor, guards, global models
│       │   ├── shared/        ← Reusable components (spinner, error, empty-state)
│       │   ├── layout/        ← Shell: main layout, navbar, sidebar
│       │   ├── features/
│       │   │   ├── dashboard/      ← KPI cards + overview charts
│       │   │   ├── genes/          ← Gene Explorer table + filter panel
│       │   │   ├── gene-detail/    ← Protein detail tabs
│       │   │   ├── analytics/      ← Full analytics charts page
│       │   │   ├── import-admin/   ← Admin import form + progress monitor
│       │   │   ├── saved-filters/  ← Saved filter management
│       │   │   └── auth/           ← Login page
│       │   ├── app.config.ts
│       │   ├── app.routes.ts
│       │   └── app.component.ts
│       ├── environments/
│       └── assets/
│
└── devops/
    ├── docker/
    │   ├── backend/Dockerfile
    │   ├── frontend/Dockerfile
    │   └── nginx/nginx.conf
    └── scripts/
        ├── start-dev.sh       ← One-command local dev startup
        ├── build-all.sh       ← Full production build
        └── db-migrate.sh      ← Run Flyway migrations manually
```

---

## 4. Architecture

```text
┌─────────────────────────────────────────────────┐
│                 Angular Frontend                 │
│   Dashboard · Gene Explorer · Analytics          │
│   Import Admin · Saved Filters · Auth            │
└─────────────────────┬───────────────────────────┘
                      │  REST / JSON  (JWT Bearer)
                      │  Base URL: /api
┌─────────────────────▼───────────────────────────┐
│              Spring Boot 3 Backend               │
│                                                  │
│  Controllers → Services → Repositories           │
│  JPA Specifications (dynamic filters)            │
│  Spring Batch (import pipeline)                  │
│  Spring Security (JWT + roles)                   │
└─────────────────────┬───────────────────────────┘
                      │  JDBC / Hibernate
┌─────────────────────▼───────────────────────────┐
│                PostgreSQL 16                     │
│                                                  │
│  protein_entry   keyword   go_term               │
│  protein_feature cross_reference                 │
│  protein_comment protein_publication             │
│  host_organism   saved_filter   import_job       │
│  app_user                                        │
│                                                  │
│  Materialized Views (charts, KPIs)               │
│  tsvector full-text index                        │
│  GIN indexes on arrays + JSONB                   │
└─────────────────────────────────────────────────┘
```

---

## 5. Key Features

| Feature | Status | Epic |
|---|:---:|---|
| UniProt `.dat` / `.tsv` import (Spring Batch) | 🔲 Planned | Epic 1 |
| Import progress monitoring (poll every 5 s) | 🔲 Planned | Epic 1 |
| Gene Explorer table (sort, paginate, multi-filter) | 🔲 Planned | Epic 2 & 3 |
| Global full-text search (PostgreSQL tsvector) | 🔲 Planned | Epic 2 |
| Advanced filter panel (12+ filter fields) | 🔲 Planned | Epic 3 |
| Dashboard KPI cards | 🔲 Planned | Epic 4 |
| Protein length histogram | 🔲 Planned | Epic 4 |
| Evidence level pie chart | 🔲 Planned | Epic 4 |
| Proteins by organism bar chart | 🔲 Planned | Epic 4 |
| Reviewed / unreviewed ratio chart | 🔲 Planned | Epic 4 |
| Keyword frequency chart | 🔲 Planned | Epic 4 |
| Cross-chart filter drill-down | 🔲 Planned | Epic 4 |
| Protein detail page (7 tabs) | 🔲 Planned | Epic 5 |
| CSV export of filtered results | 🔲 Planned | Epic 6 |
| Saved filter sets | 🔲 Planned | Epic 7 |
| JWT authentication (login / refresh) | 🔲 Planned | Auth |
| ROLE_ADMIN / ROLE_USER access control | 🔲 Planned | Auth |

---

## 6. Domain Model (Summary)

The authoritative schema is in [`documentation/domain-model.md`](documentation/domain-model.md).

| Table | Purpose |
|---|---|
| `protein_entry` | One UniProt record per row; core entity |
| `keyword` | Shared keyword vocabulary (KW lines) |
| `protein_keyword` | M:N join — protein ↔ keyword |
| `go_term` | Gene Ontology terms (DR GO lines) |
| `protein_go_term` | M:N join — protein ↔ GO term + evidence code |
| `cross_reference` | External DB links (EMBL, RefSeq, Pfam, KEGG …) |
| `protein_feature` | Annotated sequence regions (CHAIN, DOMAIN, SIGNAL …) |
| `host_organism` | Virus host taxa (OH lines) |
| `protein_comment` | Functional comments (CC lines) |
| `protein_publication` | Literature references (RN/RP/RX/RA/RT/RL lines) |
| `app_user` | Application accounts with roles |
| `saved_filter` | Persisted filter sets per user |
| `import_job` | Spring Batch job tracking |

**Materialized views:** `mv_length_histogram`, `mv_organism_counts`, `mv_reviewed_ratio`,
`mv_evidence_distribution`, `mv_keyword_frequency`, `mv_dashboard_kpis` — power all chart
endpoints without full-table scans.

---

## 7. REST API (Summary)

The full contract lives in [`documentation/api-contract.md`](documentation/api-contract.md).

| Method | Path | Description | Auth |
|---|---|---|---|
| `POST` | `/api/auth/login` | Obtain JWT pair | Public |
| `POST` | `/api/auth/refresh` | Refresh access token | Public |
| `GET` | `/api/genes` | Paginated protein list | USER |
| `POST` | `/api/genes/search` | Search + multi-filter | USER |
| `GET` | `/api/genes/{id}` | Full protein detail | USER |
| `POST` | `/api/genes/export-csv` | Export filtered CSV | USER |
| `GET` | `/api/analytics/dashboard-kpis` | KPI aggregates | USER |
| `GET` | `/api/analytics/length-histogram` | Length buckets | USER |
| `GET` | `/api/analytics/by-organism` | Top organisms | USER |
| `GET` | `/api/analytics/reviewed-ratio` | Reviewed counts | USER |
| `GET` | `/api/analytics/evidence-levels` | Evidence distribution | USER |
| `GET` | `/api/analytics/keyword-frequency` | Top keywords | USER |
| `GET` | `/api/saved-filters` | List saved filters | USER |
| `POST` | `/api/saved-filters` | Create saved filter | USER |
| `DELETE` | `/api/saved-filters/{id}` | Delete saved filter | USER (own) |
| `POST` | `/api/admin/import/uniprot` | Trigger import job | ADMIN |
| `GET` | `/api/admin/import/status` | List import jobs | ADMIN |
| `GET` | `/api/admin/import/status/{jobId}` | Job progress | ADMIN |

---

## 8. Security Model

| Aspect | Detail |
|---|---|
| Auth mechanism | JWT Bearer tokens (HS256) |
| Access token expiry | 1 hour |
| Refresh token expiry | 24 hours |
| Password hashing | bcrypt, cost factor ≥ 12 |
| Roles | `ROLE_USER`, `ROLE_ADMIN` |
| Admin guard | All `/api/admin/**` require `ROLE_ADMIN` |
| HTTPS | TLS 1.2+ required in production |
| File upload guard | `.dat` / `.tsv` only; content validated in batch processor |
| OWASP | Top 10 (2021) addressed — SQL injection via JPA, XSS via Angular sanitization, CSRF disabled (stateless JWT) |

---

## 9. Development Setup

### Prerequisites

| Tool | Version |
|---|---|
| Java (JDK) | 21+ |
| Maven | 3.9+ |
| Node.js | 20+ |
| Angular CLI | 18+ |
| Docker + Docker Compose | Latest |
| PostgreSQL | 16 (or run via Docker) |

### Quick start (local)

```bash
# 1. Clone repository
git clone <repo-url>
cd bioinformatics-analytics-dashboard

# 2. Configure environment
cp .env.example .env
# Edit .env: DB credentials, JWT secret, etc.

# 3. Start PostgreSQL (via Docker)
docker compose up postgres -d

# 4. Run backend (Flyway migrations run automatically)
cd backend
./mvnw spring-boot:run

# 5. Run frontend
cd ../frontend
npm install
ng serve --proxy-config proxy.conf.json
```

Then open **http://localhost:4200**. The frontend proxies `/api` calls to the backend on port 8080.

### Running tests

```bash
# Backend unit + integration tests
cd backend && ./mvnw test

# Frontend unit tests
cd frontend && ng test
```

---

## 10. Docker / Production Setup

```bash
# Build and start all services
docker compose up --build

# Services:
#   postgres   → port 5432
#   backend    → port 8080
#   frontend   → port 80  (nginx serves Angular + proxies /api)
```

See [`devops/`](devops/) for individual Dockerfiles and nginx configuration.

---

## 11. Spec-Driven Development Workflow

This project follows **SDD (Spec-Driven Development)**. Every feature is backed by a specification
document *before* any code is written.

### Spec Documents (read these first)

| Document | Purpose |
|---|---|
| [`overview.md`](documentation/overview.md) | User stories + acceptance criteria, NFRs, auth matrix, roadmap |
| [`api-contract.md`](documentation/api-contract.md) | REST endpoint schemas — the frontend/backend contract |
| [`domain-model.md`](documentation/domain-model.md) | Authoritative DDL — never alter schema without updating this |
| [`validation-rules.md`](documentation/validation-rules.md) | All validation rules for DTOs and import |
| [`glossary.md`](documentation/glossary.md) | Domain term definitions — use consistently |
| [`constitution.md`](documentation/constitution.md) | Coding standards, architectural principles |

### Ticket Workflow

When implementing a ticket:

1. Create `documentation/implementation/<Ticket-ID>/overview.md` — description + acceptance criteria.
2. Create `documentation/implementation/<Ticket-ID>/journal.md` — chronological log.
3. If ambiguities exist, add `documentation/implementation/<Ticket-ID>/analyse.md` and resolve before coding.
4. Add `documentation/implementation/<Ticket-ID>/plan.md` — task breakdown with status tracking.
5. Implement code, updating `plan.md` and `journal.md` as tasks complete.

---

## 12. MVP Roadmap

| Phase | Scope |
|---|---|
| **Phase 1** | DB schema + Flyway migrations, Spring Boot project setup, JWT auth |
| **Phase 2** | Spring Batch import pipeline (`.dat` parser + Overwrite strategy) |
| **Phase 3** | Gene Explorer API (`GET /genes`, `POST /genes/search`, `GET /genes/{id}`) |
| **Phase 4** | Analytics API (materialized view endpoints) |
| **Phase 5** | Angular: Auth, layout shell, Dashboard page |
| **Phase 6** | Angular: Gene Explorer table + filter panel |
| **Phase 7** | Angular: Gene Detail page (all tabs) |
| **Phase 8** | Angular: Analytics charts page |
| **Phase 9** | Angular: Import Admin page + progress monitor |
| **Phase 10** | Saved filters, CSV export, final polish |

---

## 13. Non-Functional Requirements

| Requirement | Target |
|---|---|
| Gene list initial load | ≤ 1 s (p95) |
| Search with filters | ≤ 2 s (p95) |
| Dashboard KPI cards | ≤ 500 ms |
| Analytics chart endpoints | ≤ 500 ms |
| Gene detail page | ≤ 1 s (p95) |
| CSV export (≤ 10,000 rows) | ≤ 5 s |
| Import pipeline | Full Swiss-Prot ~570,000 entries without timeout |
| Concurrent users | 50 simultaneous |
| DB scalability | 2,000,000 rows in `protein_entry` |

---

## 14. Test Credentials

The following accounts are seeded for local testing (see `Untitled-1` seed script).  
**Do not use these in any non-local environment.**

| Username | Password | Role |
|---|---|---|
| `user_test` | `password` | `ROLE_USER` |
| `admin_test` | `admin123` | `ROLE_ADMIN` |

Passwords are BCrypt-hashed (cost 10). To re-seed, run the INSERT script against the local database:

```sql
INSERT INTO public.app_user (username, password, role)
VALUES
  ('user_test',  '$2b$10$sd7Wth3x55Z/0F/iZ9qyzu5g0Ndz25F3Beez6qBPAMHQY7C.88Bsu', 'ROLE_USER'),
  ('admin_test', '$2b$10$oFip6L2K1z7zDJHFvehoy.axDZHiFVuMZK4Xx8G9pHRkoGqewgSQa', 'ROLE_ADMIN')
ON CONFLICT (username) DO NOTHING;
```

---

## 15. Contributing

1. Read [`documentation/constitution.md`](documentation/constitution.md) before writing any code.
2. Follow the Ticket Workflow described in §11.
3. Every PR must have passing tests and must not break the API contract.
4. Migrations are append-only — never edit an existing Flyway script.
