# Bioinformatics Analytics Dashboard
A project using **Spring Boot + Angular + PostgreSQL** to explore **UniProt genes/proteins** with **tables, diagrams, histograms, and filters**.

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
.and(byOrganism("Human"))
.and(lengthBetween(100,500));
```

Well-suited for dynamic Angular filter combinations.

---

# 10. Histograms / Charts Backend

Instead of returning all rows, backend returns aggregated buckets.

Example:

```json
[
 { "range":"0-100", "count":1200 },
 { "range":"101-200", "count":5400 }
]
```

This approach scales significantly better.

---

# 11. User Stories (Spec)

Each story follows the **Given / When / Then** format. Every condition is testable and constitutes the definition of done for that story.

---

# Epic 1: Import UniProt Data

### US-1 — Upload UniProt file

_As an admin, I can upload UniProt `.dat` or `.tsv` files so data becomes searchable._

**Acceptance Criteria:**

```
Given a valid uniprot_sprot.dat or .tsv file (≤ 2 GB) is selected in the import form
When the admin clicks Submit
Then a Spring Batch import job is created with status = RUNNING
  and a progress indicator is displayed
  and on successful completion all parsed entries are queryable via GET /api/genes
  and the import job record shows status = COMPLETED with entry_count filled

Given the uploaded file has an unsupported MIME type (not text/plain, text/tab-separated-values,
  application/octet-stream for .dat)
When the admin clicks Submit
Then the server rejects the request with HTTP 422
  and the error message states the accepted formats
  and no job is created

Given a file exceeding 2 GB is selected
When the admin clicks Submit
Then the server rejects the request with HTTP 413
  and no job is created

Given the file is structurally malformed (e.g. missing // delimiters)
When the import runs
Then the job status becomes FAILED with a descriptive error message
  and no partial data is committed to the database (all-or-nothing)
```

---

### US-2 — Monitor import progress

_As an admin, I can monitor import progress._

**Acceptance Criteria:**

```
Given an import job is in status = RUNNING
When the admin is on the Import Status page
Then a progress bar shows percentage complete (records processed / total estimated)
  and the display refreshes every 5 seconds without a full page reload
  and elapsed time is shown

Given an import job completes
When the admin is on the Import Status page
Then status changes to COMPLETED, percentage shows 100 %,
  and a summary is displayed: total entries processed, entries inserted, entries skipped, duration

Given an import job fails
When the admin is on the Import Status page
Then status shows FAILED with the error message and the step at which failure occurred
```

---

### US-3 — Re-import newer version

_As an admin, I can re-import newer versions._

**Acceptance Criteria:**

```
Given a newer UniProt .dat file is uploaded while no other import is running
When the import completes
Then existing protein_entry rows whose accession matches an entry in the new file
  are updated (overwrite strategy) with all new field values and updated_date refreshed
And entries present only in the old data and absent in the new file are flagged
  with a stale = TRUE marker (not deleted)
And all materialized views are refreshed after the job completes

Given an import is already in status = RUNNING
When the admin tries to submit a new import
Then the server rejects the request with HTTP 409
  and the error message states that an import is already in progress
```

---

# Epic 2: Explore Genes

### US-4 — Browse genes in paginated table

_As a user, I can browse genes in a paginated table._

**Acceptance Criteria:**

```
Given the Gene Explorer page is loaded with no filters applied
When the table renders
Then the first page shows 50 rows by default
  with columns: Accession, Gene Name, Protein Name, Organism, Length, Reviewed, Evidence, Keywords
  and a pagination control shows: current page, total pages, total result count

Given the user changes the page size to 100
When the table reloads
Then 100 rows are shown and pagination recalculates

Given there are no entries in the database
When the table loads
Then an empty state message "No proteins found" is shown instead of an empty table
```

---

### US-5 — Sort by any visible column

_As a user, I can sort by any visible column._

**Acceptance Criteria:**

```
Given the Gene Explorer table is visible
When the user clicks a sortable column header
Then the table reloads sorted ascending by that column
  and an ascending-sort indicator appears on that header

When the user clicks the same header again
Then the table reloads sorted descending
  and a descending-sort indicator appears

When the user clicks a third time
Then sorting reverts to the default order (id ASC)
```

---

### US-6 — Global search

_As a user, I can search by accession, gene name, or protein name._

**Acceptance Criteria:**

```
Given the Gene Explorer page is loaded
When the user types at least 2 characters in the global search box
Then results are filtered to entries where accession, gene_name_primary,
  or protein_full_name contains the input (case-insensitive, uses PostgreSQL tsvector)
  and results update within 400 ms of the user stopping typing (debounce)

Given the user clears the search box
When the input is empty
Then all entries are restored (no search filter active)

Given a search string that matches no entries
When the results load
Then an empty state message "No results for '<query>'" is shown
```

---

# Epic 3: Filtering

### US-7 — Filter by organism

_As a user, I can filter by organism._

**Acceptance Criteria:**

```
Given the filter panel is open
When the user types a partial organism name (e.g. "Human") in the Organism field
Then the table shows only entries where organism_name ILIKE '%Human%'
  and the active filter chip "Organism: Human" is shown above the table

Given the user removes the organism filter chip
When the chip is dismissed
Then the organism filter is cleared and all organisms are included again
```

---

### US-8 — Filter by protein length range

_As a user, I can filter by protein length range._

**Acceptance Criteria:**

```
Given the filter panel is open
When the user sets Length Min = 100 and Length Max = 500
Then the table shows only entries with 100 ≤ length ≤ 500

Given the user sets Length Min > Length Max
When the filter is applied
Then the UI shows an inline validation error
  "Minimum length must be less than or equal to maximum length"
  and no API request is made
```

---

### US-9 — Filter reviewed only

_As a user, I can filter reviewed entries only._

**Acceptance Criteria:**

```
Given the filter panel is open
When the user enables the "Reviewed only" toggle
Then only entries with reviewed = TRUE are shown in the table
  and the KPI bar updates to reflect the filtered count

When the user disables the toggle
Then all entries (reviewed and unreviewed) are shown again
```

---

### US-10 — Combine multiple filters

_As a user, I can combine multiple filters._

**Acceptance Criteria:**

```
Given the user has set: Organism = "Human", Reviewed = TRUE, Length Min = 200
When the table loads
Then only entries satisfying ALL three conditions simultaneously are shown (AND logic)

Given four or more filters are active
When any one filter is modified
Then the table re-queries with all active filters still applied

Given the user clicks "Clear All Filters"
When the action completes
Then all filter values reset to their defaults and the full dataset is shown
```

---

# Epic 4: Visual Analytics

### US-11 — Protein length histogram

_As a user, I can view histogram of protein lengths._

**Acceptance Criteria:**

```
Given the Analytics page is loaded
When the Length Histogram chart renders
Then it shows 100-AA-width buckets from 0 to 10,000
  with bar heights representing the count of proteins in each bucket
  sourced from mv_length_histogram

When the user hovers a bar
Then a tooltip shows: range (e.g. "200–299 AA"), count, and percentage of total
```

---

### US-12 — Evidence levels pie chart

_As a user, I can view pie chart of evidence levels._

**Acceptance Criteria:**

```
Given the Analytics page is loaded
When the Evidence Levels pie chart renders
Then it shows 5 slices labelled: Protein level, Transcript level, Homology, Predicted, Uncertain
  with counts sourced from mv_evidence_distribution

When the user hovers a slice
Then a tooltip shows: level number, label, count, and percentage
```

---

### US-13 — Click chart segment to filter table

_As a user, I can click a chart segment to filter the table._

**Acceptance Criteria:**

```
Given the Analytics page is open and the Gene Explorer table is visible
When the user clicks a segment on the Evidence Levels chart (e.g. "Predicted")
Then the Gene Explorer table is filtered to show only entries with evidence_level = 4
  and an active filter chip "Evidence: Predicted" appears above the table

When the user clicks the same segment again
Then the filter is toggled off and all evidence levels are included
```

---

### US-14 — Compare two filtered populations

_As a user, I can compare two filtered populations._

**Acceptance Criteria:**

```
Given the Analytics page is open
When the user defines Filter Set A (e.g. Organism = "Human") and Filter Set B
  (e.g. Organism = "Mouse") and clicks Compare
Then two side-by-side charts render: length distribution and evidence distribution
  for each subset
  and a KPI row shows for each subset: count, avg length, reviewed count, reviewed ratio

Given Filter Set A and Filter Set B are identical
When Compare is clicked
Then a warning is shown: "Filter sets are identical — comparison is not meaningful"
```

---

# Epic 5: Gene Details

### US-15 — Open gene detail page

_As a user, I can open a gene detail page._

**Acceptance Criteria:**

```
Given the Gene Explorer table is visible
When the user clicks a row
Then the browser navigates to /genes/{id}
  and the detail page loads with: Accession, Entry Name, Protein Full Name, Organism,
    Reviewed badge, Evidence level badge, Length, Molecular Weight

Given the user navigates directly to /genes/{id} with an invalid id
When the page loads
Then HTTP 404 is returned and a "Protein not found" message is shown
```

---

### US-16 — View sequence and annotations

_As a user, I can view sequence and annotations._

**Acceptance Criteria:**

```
Given the Gene Detail page is open
When the user clicks the "Sequence" tab
Then the full amino acid sequence is displayed in FASTA-like monospace format
  with annotated feature positions highlighted
  and sequence length is shown (e.g. "256 AA")

When the user clicks the "Features" tab
Then a table of annotated features is shown with columns:
  Type, Start, End, Note, Feature ID
  sourced from protein_feature
```

---

### US-17 — Open external UniProt links

_As a user, I can open external UniProt links._

**Acceptance Criteria:**

```
Given the Gene Detail page is open and the "Cross References" tab is selected
When the list of cross-references renders
Then each entry shows: Source (e.g. RefSeq, EMBL, KEGG), Identifier, and a link icon

When the user clicks a cross-reference link
Then the link opens in a new browser tab pointing to the correct external resource URL
  and the application does not navigate away from the current page
```

---

# Epic 6: Export

### US-18 — Export filtered rows to CSV

_As a user, I can export filtered rows to CSV._

**Acceptance Criteria:**

```
Given the Gene Explorer table has an active filter producing N results
When the user clicks "Export CSV"
Then a CSV file is downloaded containing all N filtered rows (not just the current page)
  with headers matching the visible column names
  and the filename is proteins_YYYY-MM-DD.csv

Given the filter returns 0 results
When the user clicks "Export CSV"
Then the button is disabled and a tooltip reads "No data to export"
```

---

### US-19 — Export chart image

_As a user, I can export chart image._

**Acceptance Criteria:**

```
Given a chart is visible on the Analytics page
When the user clicks the "Export PNG" icon on that chart
Then a PNG file is downloaded at minimum 1200×600 px
  with the chart title as the filename (e.g. length_histogram.png)
  and the image includes axes, legend, and title
```

---

# Epic 7: Saved Work

### US-20 — Save a filter set

_As a user, I can save a filter set._

**Acceptance Criteria:**

```
Given the user has applied one or more filters and is authenticated
When the user clicks "Save Filters", enters a name, and confirms
Then the filter combination is persisted and visible in the Saved Filters module
  with the name entered, the filter summary, and the creation date

Given the user tries to save with a blank name
When Submit is clicked
Then an inline validation error "Name is required" is shown
  and no filter set is saved

Given the user tries to save a filter set with a name that already exists
When Submit is clicked
Then a confirmation prompt asks "Overwrite existing '<name>'?" before proceeding
```

---

### US-21 — Reload saved searches

_As a user, I can reload saved searches._

**Acceptance Criteria:**

```
Given the user has one or more saved filter sets
When the user opens the Saved Filters module
Then a list of saved sets is shown, each with: name, creation date, filter summary

When the user clicks a saved set
Then the Gene Explorer table loads with that exact filter combination applied
  and the active filter chips reflect all filters from the saved set

When the user deletes a saved set
Then it is removed from the list immediately
  and the Gene Explorer table is not affected if it was not the active set
```

---

# 12. Non-Functional Requirements (NFRs)

These requirements are binding — they inform indexing decisions, caching strategy, pagination defaults, and infrastructure sizing.

## 12.1 Performance

| Requirement | Target |
|---|---|
| Gene Explorer table initial load (no filter) | ≤ 1 s (p95) for up to 500,000 entries |
| `POST /api/genes/search` with any filter combination | ≤ 2 s (p95) |
| Dashboard KPI cards | ≤ 500 ms (served from `mv_dashboard_kpis`) |
| Analytics chart endpoints | ≤ 500 ms (served from materialized views) |
| Gene Detail page load | ≤ 1 s (p95) |
| CSV export (up to 10,000 rows) | ≤ 5 s |

## 12.2 Scalability

| Requirement | Target |
|---|---|
| Import pipeline | Must handle the full Swiss-Prot file (~570,000 entries, ~3 GB uncompressed) without timeout |
| Concurrent users | Application must remain responsive with 50 simultaneous users |
| Database size | Schema must remain functional with 2,000,000 rows in `protein_entry` |

## 12.3 Browser Support

| Browser | Minimum Version |
|---|---|
| Google Chrome | Latest - 2 |
| Mozilla Firefox | Latest - 2 |
| Microsoft Edge | Latest - 2 |
| Safari | Latest - 1 |

Mobile/touch support is not required for MVP.

## 12.4 Security

| Requirement | Detail |
|---|---|
| Authentication | JWT (Bearer token); token expiry = 1 hour; refresh token expiry = 24 hours |
| Password storage | bcrypt with cost factor ≥ 12 |
| OWASP compliance | OWASP Top 10 (2021) must be addressed — SQL injection, XSS, CSRF, broken access control |
| HTTPS | All traffic must be served over TLS 1.2+ |
| File upload | Only `.dat` and `.tsv` MIME types accepted; content validated server-side |
| Admin endpoints | All `/api/admin/**` routes require `ROLE_ADMIN` |

## 12.5 Availability

| Requirement | Target |
|---|---|
| Planned uptime | 99.5 % (excluding scheduled maintenance) |
| Scheduled maintenance window | Sundays 02:00–04:00 UTC |

---

# 13. Authorization Matrix

Roles: **ANONYMOUS** (unauthenticated), **USER** (authenticated, standard), **ADMIN** (authenticated, elevated).

| Endpoint | ANONYMOUS | USER | ADMIN |
|---|:---:|:---:|:---:|
| `GET /api/genes` | — | ✓ | ✓ |
| `POST /api/genes/search` | — | ✓ | ✓ |
| `GET /api/genes/{id}` | — | ✓ | ✓ |
| `GET /api/analytics/length-histogram` | — | ✓ | ✓ |
| `GET /api/analytics/by-organism` | — | ✓ | ✓ |
| `GET /api/analytics/reviewed-ratio` | — | ✓ | ✓ |
| `GET /api/analytics/evidence-levels` | — | ✓ | ✓ |
| `GET /api/analytics/keyword-frequency` | — | ✓ | ✓ |
| `GET /api/analytics/dashboard-kpis` | — | ✓ | ✓ |
| `GET /api/genes/{id}/export-csv` | — | ✓ | ✓ |
| `POST /api/genes/export-csv` | — | ✓ | ✓ |
| `GET /api/saved-filters` | — | ✓ | ✓ |
| `POST /api/saved-filters` | — | ✓ | ✓ |
| `DELETE /api/saved-filters/{id}` | — | ✓ (own) | ✓ |
| `POST /api/admin/import/uniprot` | — | — | ✓ |
| `GET /api/admin/import/status` | — | — | ✓ |
| `GET /api/admin/import/status/{jobId}` | — | — | ✓ |
| `POST /api/auth/login` | ✓ | ✓ | ✓ |
| `POST /api/auth/refresh` | ✓ | ✓ | ✓ |

Unauthenticated requests to protected endpoints receive HTTP 401.  
Authenticated requests to insufficiently privileged endpoints receive HTTP 403.

---

# 14. Import Technical Specification

## 14.1 Accepted Inputs

| Parameter | Constraint |
|---|---|
| Formats | `.dat` (UniProt flat file) and `.tsv` (UniProt tab-separated) |
| Maximum file size | 2 GB |
| MIME types | `text/plain`, `text/tab-separated-values`, `application/octet-stream` (for .dat) |
| Encoding | UTF-8 |

## 14.2 Duplicate Handling Strategy

**Overwrite**: if a row with the same `accession` already exists, all fields are overwritten with the new values. The `created_at` timestamp is preserved; `updated_at` is set to `NOW()`.

## 14.3 Transaction Boundary

**All-or-nothing per chunk** (Spring Batch chunk size = 500 records):
- If a chunk fails, that chunk is rolled back.
- Previous successful chunks are committed and retained.
- The job status is set to `FAILED` with the error and the chunk number where failure occurred.
- A `FAILED` import does not roll back already-committed chunks; the admin must inspect the error and decide whether to re-import.

## 14.4 Concurrency Control

Only **one import job** may be in `RUNNING` state at any given time.  
A second import request while one is running returns HTTP 409 with body:
```json
{ "error": "An import job is already running", "runningJobId": "<id>" }
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
