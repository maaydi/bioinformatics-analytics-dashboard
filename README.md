# Bioinformatics Analytics Dashboard

> **v2.2** — Explore, visualize, and analyze **UniProt protein/gene data** from both **remote UniProt.org API** (150M+
> entries)
> and **local database** (570K+ entries). Filter, import, visualize, and export — all in one unified platform.
> Built with **Angular 21**, **Spring Boot 4**, and **PostgreSQL 16**.
---

## ✨ What It Does

|                                                                                                |                                                                                              |
|------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| 🌐 **Remote Explore** 150+ million UniProt proteins directly from UniProt.org API              | 📊 **Visualize** protein distributions via histograms, bar charts, pie charts, and KPI cards |
| 🔬 **Local Explore** 570K+ imported UniProt entries with advanced multi-field search & filters | 📥 **Import** remote data into local PostgreSQL with live progress tracking                  |
| 💾 **Export** filtered datasets through asynchronous pipelines as CSV, TSV, JSON, or Excel     | 🔖 **Save** and reload filter combinations across sessions                                   |
| 🌙 **Dark Mode** Full dark theme support for comfortable extended exploration                  | 🔐 **Update Password** Secure user profile management with password reset                    |
| 📋 **Inspect** full protein detail: sequence, GO terms, features, cross-references             | ⚡ **Filter Remote Data** Apply 15+ filter dimensions to 150M UniProt entries in real-time   |

---

## 📸 Screenshots

### Login

![Login Screen](documentation/implementation/AUTH-001/login-screen.png)

### Dashboard

![Dashboard-01](documentation/implementation/DASH-001/dashboard-screen-01.png)
![Dashboard-02](documentation/implementation/DASH-001/dashboard-screen-02.png)

### Gene Explorer — Search & Browse

![Gene Explorer](documentation/implementation/GENE-003/explore-gene-screen.png)

### Advanced Filters

![Advanced Filters](documentation/implementation/GENE-002/apply-filters-screen.png)

### Protein Detail

![Protein Detail](documentation/implementation/DETAIL-001/gene-detail-summary.png)

### Analytics

![Analytics-01](documentation/implementation/ANALYTICS-001/analytics-1.png)
![Analytics-02](documentation/implementation/ANALYTICS-001/analytics-2.png)

### Compare

![Compare-01](documentation/implementation/COMPARE-001/compare-01.png)
![Compare-02](documentation/implementation/COMPARE-001/compare-02.png)

### Saved Filters

![Saved-Filters](documentation/implementation/FILTER-001/saved-filters.png)

### Export Data Via Pipeline

![Export-01](documentation/implementation/PIPE-001/export-steps.jpg)
![Export-02](documentation/implementation/PIPE-001/export-table.png)

### Import Admin — Progress Monitoring

![Import Admin](documentation/implementation/IMPORT-001/import-admin-screen.png)

### Account Settings

![Account Screen](documentation/implementation/AUTH-001/account-settings.png)

---
## 🏗️ Architecture

The platform provides two complementary data-access modes through a unified interface:

```text
                              Angular 21
                                  │
                                  ▼
                         Unified Application API
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
              🌐 Remote Mode              💾 Local Mode
                    │                           │
                    ▼                           ▼
              UniProt.org                 PostgreSQL
                    │                           │
              150M+ entries                 570K+ entries
                    │                           │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    │                           │
              🔎 Explore & Filter          📥 Import
              📤 Export                     │
                                            ▼
                                      📊 Analytics
                                      📈 Visualization
                                      │
                              ┌────────┴────────┐
                              │                 │
                         Histograms         Bar / Pie /
                         KPI metrics        Other plots
                              │                 │
                              └────────┬────────┘
                                       ▼
                                📤 Export Visualization
```

### Remote Mode — UniProt.org

The remote mode provides direct, read-only access to the live UniProt dataset without requiring local storage.

- **150M+ protein entries** available through the UniProt.org API
- **Advanced filtering** across accession, organism, length, GO terms, reviewed status, and other dimensions
- **Pagination and sorting** for large result sets
- **Export** filtered remote results without importing the complete dataset
- **Redis caching** to optimize repeated remote queries

### Local Mode — PostgreSQL

The local mode provides persistent access to data imported from UniProt (API or Files).

- **Selective import** of filtered remote results/ Files
- **> 570K+ locally stored entries** for intensive exploration and analysis
- **Full-text and multi-field search**
- **Materialized views** for pre-aggregated metrics and dashboard KPIs
- **Spring Batch** processing for large-scale imports

### Analytics & Visualization

Analytics that require aggregation across the dataset are performed on **locally imported PostgreSQL data**.

Imported data can be transformed into:

- **Histograms** — protein length and other numerical distributions
- **Bar charts** — organism, evidence level, keywords, and other categorical distributions
- **Pie charts** — reviewed/unreviewed and other proportional distributions
- **KPI cards** — totals, averages, minimum/maximum values, and other aggregate metrics

The resulting visualizations provide an interactive way to explore the characteristics of the imported dataset, while the underlying aggregated data can also be **exported for further analysis**.

### Remote-to-Local Workflow

Remote exploration and local analytics work together as a single workflow:

```text
UniProt.org
     │
     │  Search / Filter
     ▼
Remote Results
     │
     │  Select & Import
     ▼
PostgreSQL
     │
     ├── Explore / Filter
     │
     ├── Analytics
     │      ├── Histograms
     │      ├── Bar charts
     │      ├── Pie charts
     │      └── KPI metrics
     │
     └── Export
            ├── CSV
            ├── TSV
            ├── JSON
            └── Excel
```

This separation allows the application to combine **live access to the global UniProt dataset** with the **performance and analytical capabilities of a local database**.

## 🚀 Quick Start

```bash
# Clone and configure
git clone <repo-url>
cd bioinformatics-analytics-dashboard
cp .env.example .env   # fill in DB credentials + JWT secret + UniProt API base URL
# Start everything with Docker
docker compose up --build
# → Frontend: http://localhost
# → Backend API: http://localhost/api
```

**Local development** (without Docker):

```bash
# Start PostgreSQL, then:
cd backend/dashboard  && ./mvnw spring-boot:run          # http://localhost:8080
cd frontend && npm install && npm start        # http://localhost:4200
```

**Test accounts** (local only):
| Username | Password | Role |
|---|---|---|
| `user_test` | `password` | User |
| `admin_test` | `admin123` | Admin |

---

## 🔄 Dual-Mode Data Exploration

### Remote Mode (UniProt.org API)

- **150M+ live protein entries** from UniProt.org
- **Real-time filtering** on 15+ dimensions (accession, organism, GO terms, length, etc.)
- **No local storage required** — query remote API directly
- **Fast read-only access** for discovery and analysis

### Local Mode (PostgreSQL)

- **Import remote data** into local database for persistence
- **Materialized views** for instant analytics and KPI dashboards
- **Full-text search** on sequence, description, and keywords
- **Batch processing** with 570K+ entries for heavy lifting

---

## 🛠 Tech Stack

| Layer    | Technology                                                               |
|----------|--------------------------------------------------------------------------|
| Frontend | Angular 21 — signals, standalone components, AG Grid, ECharts, dark mode |
| Backend  | Spring Boot 4, Java 25 — layered REST API, Spring Batch, UniProt proxy   |
| Database | PostgreSQL 16 — materialized views, GIN indexes, full-text search        |
| APIs     | UniProt REST API integration for live 150M+ protein dataset              |
| Auth     | Spring Security + JWT (HS256), password management                       |
| Infra    | Docker Compose, Flyway migrations, nginx, Redis caching, Kubernetes      |

---

## 📁 Project Structure

```
backend/dashboard       → Spring Boot (controller → service → repository → dto)
backend/services/export-service/ → Export microservice: pipelines, batch jobs, CSV/TSV/JSON/Excel writers
frontend/               → Angular features: dashboard, genes, analytics, import-admin, auth
documentation/          → Specs: api-contract, domain-model, validation-rules, overview
devops/                 → Dockerfiles, Kubernetes files, nginx config, shell scripts
```

Full details in [`documentation/`](documentation).
---

## ⚡ Performance Targets

| Operation                      | Target      |
|--------------------------------|-------------|
| Remote UniProt API search      | ≤ 3 s (p95) |
| Local gene list load           | ≤ 1 s (p95) |
| Local filtered search          | ≤ 2 s (p95) |
| Dashboard KPIs                 | ≤ 500 ms    |
| CSV export (up to 100K rows)   | ≤ 5 s       |
| Remote data import to local DB | Streaming   |
| Full Swiss-Prot import         | No timeout  |

---

## 🎯 v2.2 Key Features

### Deployment on Kubernetes

- **Dual deployment support** — run the application seamlessly in both **Docker Compose** for local development and
  **Kubernetes** for containerized, scalable deployments
- **Kubernetes-ready architecture** — deploy application services, PostgreSQL, Redis, and supporting infrastructure as
  Kubernetes workloads
- **Containerized services** — package all microservices and frontend components as independent Docker containers for
  consistent deployments across environments
- **Scalable deployment** — leverage Kubernetes to scale application services independently based on workload
  requirements
- **Environment-based configuration** — manage service configuration, secrets, and deployment settings through
  Kubernetes resources

### Remote Data Access

- **Live UniProt.org integration** — access 150+ million protein entries without local storage
- **Advanced filtering** — search by accession, organism, protein length, GO terms, reviewed status, and more
- **Pagination & sorting** — handle large result sets efficiently
- **Caching layer** — optimized API queries with Redis

### Local Import & Visualization

- **Selective import** — cherry-pick filtered results from UniProt and import into local PostgreSQL
- **Batch processing** — handle millions of records with Spring Batch
- **Real-time progress** — live import tracking with cancel support
- **Materialized views** — pre-aggregated analytics for instant dashboard KPIs

### Export Data Microservice

The dedicated **Export Service** (`backend/services/export-service`) manages
asynchronous data exports through configurable pipelines, powered by Spring Batch.

- **Multiple formats** — export datasets as CSV, TSV, JSON, or Excel.
- **Configurable pipelines** — define dataset filters and select fields to include.
- **Background processing** — generate export files without blocking interactive requests.
- **Progress monitoring** — track job status, completion percentage, and batch progress.
- **Pipeline management** — browse export history, inspect details, retry failed or
  cancelled exports, and delete pipelines.
- **Authenticated downloads** — download completed files through endpoints requiring
  a User or Admin role.

**API base path:** `/api/v1/exports`

### User Experience

- **🌙 Dark Mode** — full UI theme toggle for comfortable nighttime browsing
- **🔐 Password Management** — secure profile updates and password reset workflows
- **💾 Data Export Via Pipeline** — download filtered datasets in multiple formats : CSV, TSV, JSON & Excel
- **📌 Saved Filters** — persist and reload custom filter combinations for repeated queries
- **Responsive design** — works seamlessly on desktop, tablet, and mobile devices

---

## 🤝 Contributing

1. Read [`documentation/constitution.md`](documentation/constitution.md) for coding standards.
2. Check [`documentation/api-contract.md`](documentation/api-contract.md) before touching any endpoint.
3. Every PR needs passing tests (`./mvnw test` + `npm test`).
4. Database changes go in a new Flyway migration — never edit existing scripts.
