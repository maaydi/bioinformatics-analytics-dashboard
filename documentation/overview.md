# Bioinformatics Analytics Dashboard

A project using **Spring Boot + Angular + PostgreSQL** to explore **UniProt genes/proteins** with **tables, diagrams,
histograms, and filters**.

Users can:

* import UniProt data
* search/filter genes
* visualize distributions
* inspect entries
* compare subsets
* export results

---

# 1. High-Level Architecture

```text
Angular Frontend
   |
REST API
(Spring Boot)
   |
Service Layer
   |
PostgreSQL
   |
ETL / Import Pipeline
(UniProt .dat / TSV / XML)
```

---

# 2. Recommended Tech Stack

## Frontend (Angular)

Recommended libraries:

* Angular latest
* Angular Material
* AG Grid (tables)
* ngx-charts / ApexCharts / ECharts
* RxJS
* Angular Router

## Backend (Spring Boot)

Recommended libraries:

* Spring Boot 3+
* Spring Web
* Spring Data JPA
* Spring Security
* Spring Batch (imports)
* PostgreSQL driver
* Lombok
* MapStruct

## Database

PostgreSQL

Recommended features:

* JSONB
* Full text search
* indexes
* materialized views

---

# 3. Database Schema

The authoritative domain model is defined in [domain-model.md](domain-model.md).

The main entity is `protein_entry` (one row per UniProt record) with related tables:
`keyword`, `protein_keyword`, `go_term`, `protein_go_term`, `cross_reference`,
`protein_feature`, `host_organism`, `protein_comment`, and `protein_publication`.

Materialized views (`mv_length_histogram`, `mv_organism_counts`, `mv_reviewed_ratio`,
`mv_evidence_distribution`, `mv_keyword_frequency`, `mv_dashboard_kpis`) power all
Dashboard and Analytics charts without full-table scans.

---

# 4. Angular UI Modules

```text
App
 ├── Dashboard
 ├── Gene Explorer
 ├── Gene Details
 ├── Charts Analytics
 ├── Import Admin
 ├── Saved Filters
 └── Settings
```

---

# 5. Main Pages

---

## A. Dashboard

KPIs:

* total proteins
* reviewed count
* organisms count
* avg length
* top taxa

Charts:

* reviewed vs unreviewed
* proteins by organism
* length histogram
* evidence level pie chart

---

## B. Gene Explorer Table

Advanced data table:

Columns:

* accession
* gene name
* protein name
* organism
* length
* reviewed
* evidence
* keywords

Features:

* sorting
* pagination
* multi-filter
* global search
* column hide/show
* export CSV

---

## C. Gene Detail Page

For one UniProt entry:

Tabs:

* Summary
* Sequence
* Features
* Cross References
* Taxonomy
* Publications
* Similar Proteins

---

## D. Analytics Page

Interactive charts:

* histogram of protein lengths
* bar chart by organism
* evidence distribution
* reviewed ratio
* keyword frequency
* scatter: length vs weight

---

# 6. Filtering System (Very Important)

Users should filter by:

* accession
* gene name
* protein name
* reviewed
* organism
* taxid
* length range
* molecular weight range
* keyword contains
* evidence level
* feature exists
* has GO term

---

# 7. REST API Design

## Gene Search

```http
GET /api/genes?page=0&size=50
POST /api/genes/search
```

Body:

```json
{
  "geneName": "ABC",
  "reviewed": true,
  "organism": "Human",
  "lengthMin": 100,
  "lengthMax": 900
}
```

---

## Charts

```http
GET /api/analytics/length-histogram
GET /api/analytics/by-organism
GET /api/analytics/reviewed-ratio
GET /api/analytics/evidence-levels
```

---

## Detail

```http
GET /api/genes/{id}
```

---

## Import

```http
POST /api/admin/import/uniprot
```

---

# 8. Spring Boot Layered Architecture

```text
controller/
service/
repository/
dto/
entity/
mapper/
specification/
security/
batch/
```

---

# 9. Smart Query Strategy

Use **JPA Specifications** for dynamic filters.

Example:

```java
GeneSpecification.byReviewed(true)
.

and(byOrganism("Human"))
        .

and(lengthBetween(100,500));
```

Well-suited for dynamic Angular filter combinations.

---

# 10. Histograms / Charts Backend

Instead of returning all rows, backend returns aggregated buckets.

Example:

```json
[
  {
    "range": "0-100",
    "count": 1200
  },
  {
    "range": "101-200",
    "count": 5400
  }
]
```

This approach scales significantly better.

---

# 11. User Stories (Spec)

The full backlog of epics and user stories — including detailed acceptance
criteria (Given / When / Then), implementation hints, and ticket mapping —
lives in [plan.md](plan.md).

Each story there is the **definition of done** for its scope and is linked to
a ticket ID under [implementation/](implementation/README.md).

---

# 12. Non-Functional Requirements (NFRs)

These requirements are binding — they inform indexing decisions, caching strategy, pagination defaults, and
infrastructure sizing.

## 12.1 Performance

| Requirement                                          | Target                                     |
|------------------------------------------------------|--------------------------------------------|
| Gene Explorer table initial load (no filter)         | ≤ 1 s (p95) for up to 500,000 entries      |
| `POST /api/genes/search` with any filter combination | ≤ 2 s (p95)                                |
| Dashboard KPI cards                                  | ≤ 500 ms (served from `mv_dashboard_kpis`) |
| Analytics chart endpoints                            | ≤ 500 ms (served from materialized views)  |
| Gene Detail page load                                | ≤ 1 s (p95)                                |
| CSV export (up to 10,000 rows)                       | ≤ 5 s                                      |

## 12.2 Scalability

| Requirement      | Target                                                                                      |
|------------------|---------------------------------------------------------------------------------------------|
| Import pipeline  | Must handle the full Swiss-Prot file (~570,000 entries, ~3 GB uncompressed) without timeout |
| Concurrent users | Application must remain responsive with 50 simultaneous users                               |
| Database size    | Schema must remain functional with 2,000,000 rows in `protein_entry`                        |

## 12.3 Browser Support

| Browser         | Minimum Version |
|-----------------|-----------------|
| Google Chrome   | Latest - 2      |
| Mozilla Firefox | Latest - 2      |
| Microsoft Edge  | Latest - 2      |
| Safari          | Latest - 1      |

Mobile/touch support is not required for MVP.

## 12.4 Security

| Requirement      | Detail                                                                                  |
|------------------|-----------------------------------------------------------------------------------------|
| Authentication   | JWT (Bearer token); token expiry = 1 hour; refresh token expiry = 24 hours              |
| Password storage | bcrypt with cost factor ≥ 12                                                            |
| OWASP compliance | OWASP Top 10 (2021) must be addressed — SQL injection, XSS, CSRF, broken access control |
| HTTPS            | All traffic must be served over TLS 1.2+                                                |
| File upload      | Only `.dat` and `.tsv` MIME types accepted; content validated server-side               |
| Admin endpoints  | All `/api/admin/**` routes require `ROLE_ADMIN`                                         |

## 12.5 Availability

| Requirement                  | Target                                   |
|------------------------------|------------------------------------------|
| Planned uptime               | 99.5 % (excluding scheduled maintenance) |
| Scheduled maintenance window | Sundays 02:00–04:00 UTC                  |

---

# 13. Authorization Matrix

Roles: **ANONYMOUS** (unauthenticated), **USER** (authenticated, standard), **ADMIN** (authenticated, elevated).

| Endpoint                               | ANONYMOUS |  USER   | ADMIN |
|----------------------------------------|:---------:|:-------:|:-----:|
| `GET /api/genes`                       |     —     |    ✓    |   ✓   |
| `POST /api/genes/search`               |     —     |    ✓    |   ✓   |
| `GET /api/genes/{id}`                  |     —     |    ✓    |   ✓   |
| `GET /api/analytics/length-histogram`  |     —     |    ✓    |   ✓   |
| `GET /api/analytics/by-organism`       |     —     |    ✓    |   ✓   |
| `GET /api/analytics/reviewed-ratio`    |     —     |    ✓    |   ✓   |
| `GET /api/analytics/evidence-levels`   |     —     |    ✓    |   ✓   |
| `GET /api/analytics/keyword-frequency` |     —     |    ✓    |   ✓   |
| `GET /api/analytics/dashboard-kpis`    |     —     |    ✓    |   ✓   |
| `GET /api/genes/{id}/export-csv`       |     —     |    ✓    |   ✓   |
| `POST /api/genes/export-csv`           |     —     |    ✓    |   ✓   |
| `GET /api/saved-filters`               |     —     |    ✓    |   ✓   |
| `POST /api/saved-filters`              |     —     |    ✓    |   ✓   |
| `DELETE /api/saved-filters/{id}`       |     —     | ✓ (own) |   ✓   |
| `POST /api/admin/import/uniprot`       |     —     |    —    |   ✓   |
| `GET /api/admin/import/status`         |     —     |    —    |   ✓   |
| `GET /api/admin/import/status/{jobId}` |     —     |    —    |   ✓   |
| `POST /api/auth/login`                 |     ✓     |    ✓    |   ✓   |
| `POST /api/auth/refresh`               |     ✓     |    ✓    |   ✓   |

Unauthenticated requests to protected endpoints receive HTTP 401.  
Authenticated requests to insufficiently privileged endpoints receive HTTP 403.

---

# 14. Import Technical Specification

## 14.1 Accepted Inputs

| Parameter         | Constraint                                                                       |
|-------------------|----------------------------------------------------------------------------------|
| Formats           | `.dat` (UniProt flat file) and `.tsv` (UniProt tab-separated)                    |
| Maximum file size | 2 GB                                                                             |
| MIME types        | `text/plain`, `text/tab-separated-values`, `application/octet-stream` (for .dat) |
| Encoding          | UTF-8                                                                            |

## 14.2 Duplicate Handling Strategy

**Overwrite**: if a row with the same `accession` already exists, all fields are overwritten with the new values. The
`created_at` timestamp is preserved; `updated_at` is set to `NOW()`.

## 14.3 Transaction Boundary

**All-or-nothing per chunk** (Spring Batch chunk size = 500 records):

- If a chunk fails, that chunk is rolled back.
- Previous successful chunks are committed and retained.
- The job status is set to `FAILED` with the error and the chunk number where failure occurred.
- A `FAILED` import does not roll back already-committed chunks; the admin must inspect the error and decide whether to
  re-import.

## 14.4 Concurrency Control

Only **one import job** may be in `RUNNING` state at any given time.  
A second import request while one is running returns HTTP 409 with body:

```json
{
  "error": "An import job is already running",
  "runningJobId": "<id>"
}
```

## 14.5 Progress Reporting

Progress is reported as a percentage: `(records_processed / total_estimated_records) × 100`.  
For `.dat` files, `total_estimated_records` is estimated from file size at job start.  
Progress is polled by the frontend via `GET /api/admin/import/status/{jobId}` every 5 seconds.

## 14.6 Post-Import Actions

After a successful import completes, the Spring Batch job executes the following in order:

1. Refresh all materialized views (`REFRESH MATERIALIZED VIEW CONCURRENTLY ...`)
2. Update the import job record: `status = COMPLETED`, `entry_count`, `duration_ms`
3. Log completion at INFO level

---

# 15. Advanced Features (Excellent for Portfolio)

* BLAST-like sequence search
* Similarity clustering
* Heatmaps
* Gene compare mode
* Favorites
* Role-based access
* Scheduled UniProt sync

---

# 16. Performance Tips

## PostgreSQL indexes

```sql
accession
gene_name
organism_name
reviewed
length
GIN(metadata_jsonb)
GIN(to_tsvector(...))
```

---

# 17. Best MVP Roadmap

## Phase 1 (2 weeks)

* import TSV
* table page
* filters
* details page

## Phase 2

* charts
* histograms
* saved filters

## Phase 3

* auth
* admin import
* compare mode

---

# 17. Test Credentials

The following accounts are seeded for local testing  
**Do not use these in any non-local environment.**

| Username     | Password   | Role         |
|--------------|------------|--------------|
| `user_test`  | `password` | `ROLE_USER`  |
| `admin_test` | `admin123` | `ROLE_ADMIN` |

Passwords are BCrypt-hashed (cost 10). To re-seed, run the INSERT script against the local database:

```sql
INSERT INTO public.app_user (username, password, role)
VALUES ('user_test', '$2b$10$sd7Wth3x55Z/0F/iZ9qyzu5g0Ndz25F3Beez6qBPAMHQY7C.88Bsu', 'ROLE_USER'),
       ('admin_test', '$2b$10$oFip6L2K1z7zDJHFvehoy.axDZHiFVuMZK4Xx8G9pHRkoGqewgSQa',
        'ROLE_ADMIN') ON CONFLICT (username) DO NOTHING;
```
