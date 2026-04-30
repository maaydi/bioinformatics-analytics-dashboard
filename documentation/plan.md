# Implementation Plan — Epics & User Stories

This document is the **authoritative backlog** for the Bioinformatics Analytics
Dashboard. Each user story below uses the **Given / When / Then** format and
constitutes the definition of done for that story.

It expands the original epics with:

- **Implementation hints** per story (entities, endpoints, DTOs, components,
  services, validation, tests).
- **Ticket mapping** to the IDs declared in
  [implementation/README.md](implementation/README.md).
- **Additional user stories** (US-22 …) that close gaps the original spec left
  open (auth, filters by keyword/GO/weight, admin job history, accessibility,
  rate-limiting, etc.).

Cross-references:

- Domain schema → [domain-model.md](domain-model.md)
- REST contract → [api-contract.md](api-contract.md)
- Validation rules → [validation-rules.md](validation-rules.md)
- NFRs / Authorization matrix / Import spec → [overview.md](overview.md)

---

## Ticket Map

| Ticket ID | Scope | Stories |
|---|---|---|
| `AUTH-001` | DB schema (V1) + JWT login/refresh/logout + role guards | US-22, US-23, US-24, US-43 |
| `IMPORT-001` | Spring Batch import pipeline + ImportController | US-1, US-2, US-3, US-25, US-26, US-31 |
| `GENE-001` | GeneController + GeneService + GeneSpecification | US-4, US-5, US-6, US-10 |
| `GENE-002` | GeneFilter Angular component + reactive form | US-7, US-8, US-9, US-10, US-27, US-28, US-29, US-30 |
| `GENE-003` | GenesTable AG Grid component | US-4, US-5, US-6, US-36 |
| `DETAIL-001` | Gene Detail page (all tabs) | US-15, US-16, US-17 |
| `ANALYTICS-001` | Analytics endpoints + materialized views | US-11, US-12, US-33, US-34, US-35 |
| `DASH-001` | Dashboard page (KPI cards + charts) | US-32 |
| `FILTER-001` | Saved filter feature (backend + frontend) | US-20, US-21 |
| `EXPORT-001` | CSV / chart-image export | US-18, US-19 |
| `COMPARE-001` | Compare-mode analytics | US-13, US-14 |
| `OPS-001` | Health check, audit log, rate limiting | US-38, US-39, US-40 |
| `A11Y-001` | Accessibility & UX polish | US-36, US-37, US-41, US-42 |

---

# Epic 1: Import UniProt Data

> Tickets: `IMPORT-001`, `AUTH-001` (admin role required)

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
  and no partial data is committed to the database (all-or-nothing per chunk)
```

**Implementation notes**
- **Backend:** `ImportController#uploadUniprot(MultipartFile)`,
  `ImportService`, `UniprotDatItemReader`, `UniprotTsvItemReader`,
  `ProteinEntryItemProcessor`, `ProteinEntryItemWriter`, chunk size = 500.
- **Validation:** MIME + extension check; magic-bytes sniff for `.dat`;
  `multipart.max-file-size = 2GB` in `application.yml`.
- **DTO:** `ImportJobResponse { jobId, status, startedAt }`.
- **Errors:** `UnsupportedMediaTypeException → 422`,
  `MaxUploadSizeExceededException → 413`,
  `MalformedUniprotFileException → job FAILED`.
- **Tests:** unit tests on parsers (small fixtures in
  `backend/src/test/resources/fixtures/`), integration test with Testcontainers
  PostgreSQL.

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

**Implementation notes**
- **Backend:** `GET /api/admin/import/status/{jobId}` returning
  `ImportJobStatus { id, status, progressPct, processed, total, insertedCount, skippedCount, durationMs, errorMessage, failedStep }`.
- **Frontend:** `import-admin/import-status.component`, polling every 5 s with
  `interval(5000)` + `switchMap`; cancel poll when status is terminal.
- Use Angular signals for status state; `computed()` for derived progress label.

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

**Implementation notes**
- **Concurrency guard:** unique partial index
  `CREATE UNIQUE INDEX ux_import_running ON import_job((status)) WHERE status='RUNNING';`
- **Stale flag:** `protein_entry.stale BOOLEAN DEFAULT FALSE`; set with a single
  `UPDATE ... WHERE last_seen_import_id <> :currentJobId`.
- **MV refresh:** call
  `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_*` in a final job step.

---

# Epic 2: Explore Genes

> Tickets: `GENE-001`, `GENE-003`

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

**Implementation notes**
- **Endpoint:** `GET /api/genes?page=0&size=50&sort=id,asc`.
- **DTO:** `PagedResponse<GeneSummaryDto>` (envelope already in
  `frontend/src/app/core/models/paged-response.model.ts`).
- **Page size cap:** max `size = 200` (validated server-side, see US-40).
- **Component:** `features/genes/genes-table.component` (AG Grid, `OnPush`).

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

**Implementation notes**
- Whitelist sortable properties in `GeneController` to prevent injection via
  `sort=` parameter (`accession`, `geneNamePrimary`, `proteinFullName`,
  `organismName`, `length`, `reviewed`, `evidenceLevel`).

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

**Implementation notes**
- **Index:** `CREATE INDEX ix_protein_search ON protein_entry USING GIN(search_tsv);`
  where `search_tsv` is a generated column.
- **Frontend:** `debounceTime(400) + distinctUntilChanged()`.

---

# Epic 3: Filtering

> Tickets: `GENE-001`, `GENE-002`

### US-7 — Filter by organism

```
Given the filter panel is open
When the user types a partial organism name (e.g. "Human") in the Organism field
Then the table shows only entries where organism_name ILIKE '%Human%'
  and the active filter chip "Organism: Human" is shown above the table

Given the user removes the organism filter chip
When the chip is dismissed
Then the organism filter is cleared and all organisms are included again
```

**Implementation notes**
- `GeneSpecification.organismLike(String)` using
  `cb.like(cb.lower(...), "%" + value.toLowerCase() + "%")`.
- Trigram index: `CREATE INDEX ix_protein_organism_trgm ON protein_entry USING GIN (organism_name gin_trgm_ops);`

---

### US-8 — Filter by protein length range

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

**Implementation notes**
- Reactive form cross-field validator `lengthRangeValidator`.
- Server: `@AssertTrue` on `GeneSearchRequest` (`isLengthRangeValid()`).

---

### US-9 — Filter reviewed only

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

**Implementation notes**
- Compose `Specification`s with `.and()`; null-check each predicate.
- Persist active filters as a signal store
  (`features/genes/state/filters.store.ts`) used by both filter panel and table.

---

# Epic 4: Visual Analytics

> Tickets: `ANALYTICS-001`, `COMPARE-001`

### US-11 — Protein length histogram

```
Given the Analytics page is loaded
When the Length Histogram chart renders
Then it shows 100-AA-width buckets from 0 to 10,000
  with bar heights representing the count of proteins in each bucket
  sourced from mv_length_histogram

When the user hovers a bar
Then a tooltip shows: range (e.g. "200–299 AA"), count, and percentage of total
```

**Implementation notes**
- `GET /api/analytics/length-histogram` →
  `[{ rangeStart, rangeEnd, count }]`.
- Compute percentage client-side from total returned in the same payload.

---

### US-12 — Evidence levels pie chart

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

```
Given the Analytics page is open and the Gene Explorer table is visible
When the user clicks a segment on the Evidence Levels chart (e.g. "Predicted")
Then the Gene Explorer table is filtered to show only entries with evidence_level = 4
  and an active filter chip "Evidence: Predicted" appears above the table

When the user clicks the same segment again
Then the filter is toggled off and all evidence levels are included
```

**Implementation notes**
- Chart `output()` event → push value into shared `filters.store`.

---

### US-14 — Compare two filtered populations

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

**Implementation notes**
- `POST /api/analytics/compare` body `{ setA: GeneSearchRequest, setB: GeneSearchRequest }`
  → `{ a: AnalyticsSubset, b: AnalyticsSubset }`.

---

# Epic 5: Gene Details

> Tickets: `DETAIL-001`

### US-15 — Open gene detail page

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

**Implementation notes**
- Lazy-load tab payloads (one HTTP call per tab on first activation).

---

### US-17 — Open external UniProt links

```
Given the Gene Detail page is open and the "Cross References" tab is selected
When the list of cross-references renders
Then each entry shows: Source (e.g. RefSeq, EMBL, KEGG), Identifier, and a link icon

When the user clicks a cross-reference link
Then the link opens in a new browser tab pointing to the correct external resource URL
  and the application does not navigate away from the current page
```

**Implementation notes**
- Use `target="_blank" rel="noopener noreferrer"` (security).

---

# Epic 6: Export

> Tickets: `EXPORT-001`

### US-18 — Export filtered rows to CSV

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

**Implementation notes**
- Stream with `StreamingResponseBody` to avoid loading all rows in memory.
- Hard cap = 100,000 rows; above that, return HTTP 413 with guidance to refine.

---

### US-19 — Export chart image

```
Given a chart is visible on the Analytics page
When the user clicks the "Export PNG" icon on that chart
Then a PNG file is downloaded at minimum 1200×600 px
  with the chart title as the filename (e.g. length_histogram.png)
  and the image includes axes, legend, and title
```

---

# Epic 7: Saved Work

> Tickets: `FILTER-001`

### US-20 — Save a filter set

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

**Implementation notes**
- Table `saved_filter (id, user_id, name, filter_json, created_at)` with
  unique `(user_id, name)`.

---

### US-21 — Reload saved searches

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

# Epic 8: Authentication & Authorization

> Tickets: `AUTH-001`

These stories are implied by the NFRs (§12.4) and the authorization matrix
(§13) of [overview.md](overview.md), but were not previously expressed as
backlog items.

### US-22 — Log in with email and password

```
Given an inactive visitor on the login page
When the visitor submits valid credentials
Then the server returns HTTP 200 with { accessToken, refreshToken, user }
  and the access token expires in 1 hour, the refresh token in 24 hours
  and the user is redirected to the Dashboard

Given invalid credentials
When the visitor submits the form
Then the server returns HTTP 401 with body { error: "Invalid credentials" }
  and no token is issued

Given 5 consecutive failed attempts within 10 minutes for the same email
When the 6th attempt is submitted
Then the server returns HTTP 423 (Locked)
  and the account is locked for 15 minutes
```

**Implementation notes**
- `POST /api/auth/login` → `LoginRequest { email, password }` →
  `AuthResponse { accessToken, refreshToken, user: UserDto }`.
- Passwords stored with BCrypt cost ≥ 12.
- Lockout state in `app_user (failed_attempts, locked_until)`.

---

### US-23 — Refresh access token

```
Given the user holds a valid refresh token
When the client calls POST /api/auth/refresh with that token
Then a new access token is returned (TTL 1 h) and the same refresh token is reused

Given an expired or revoked refresh token
When the client calls /api/auth/refresh
Then HTTP 401 is returned and the client redirects to login
```

**Implementation notes**
- Refresh tokens stored hashed in `refresh_token` table; revoked on logout.

---

### US-24 — Log out

```
Given an authenticated user
When the user clicks "Log out"
Then the refresh token is revoked server-side
  and tokens are cleared client-side
  and the user is redirected to /login
```

---

### US-43 — Role-based access enforcement

```
Given an authenticated user with role USER
When the user calls any /api/admin/** endpoint
Then HTTP 403 is returned with body { error: "Insufficient privileges" }

Given an unauthenticated request to a protected endpoint
When the request reaches the API
Then HTTP 401 is returned with body { error: "Authentication required" }
```

**Implementation notes**
- `SecurityFilterChain` with `.requestMatchers("/api/admin/**").hasRole("ADMIN")`.
- `@PreAuthorize("hasRole('ADMIN')")` on `ImportController` methods.
- Frontend route guards: `authGuard`, `adminGuard` (already in
  `frontend/src/app/core/guards/`).

---

# Epic 9: Admin Operations (Imports)

> Tickets: `IMPORT-001`, `OPS-001`

### US-25 — View import job history

```
Given the admin opens the Import Admin page
When the page loads
Then a paginated list of past jobs is shown with columns:
  Started, Finished, Status, Triggered By, Entries (inserted/skipped), Duration
  ordered by Started DESC

When the admin clicks a job row
Then the job detail panel opens showing the same fields as the live status view
  plus the original filename and SHA-256 checksum
```

**Implementation notes**
- `GET /api/admin/import/jobs?page=&size=` → `PagedResponse<ImportJobSummaryDto>`.

---

### US-26 — Cancel a running import

```
Given an import job is in status = RUNNING
When the admin clicks "Cancel" and confirms
Then the Spring Batch job receives a stop signal
  and within ≤ 30 seconds the job status becomes CANCELLED
  and chunks already committed are retained
  and a new import can be started immediately
```

**Implementation notes**
- `POST /api/admin/import/{jobId}/cancel` →
  `JobOperator.stop(executionId)`.

---

# Epic 10: Additional Filters

> Tickets: `GENE-002`

### US-27 — Filter by keyword

```
Given the filter panel is open
When the user adds keyword "Phosphoprotein"
Then the table shows only entries linked to that keyword via protein_keyword
```

### US-28 — Filter by GO term

```
Given the filter panel is open
When the user enters a GO ID (e.g. "GO:0005524") or GO name fragment
Then the table shows only entries linked to a matching go_term via protein_go_term
```

### US-29 — Filter by molecular weight range

```
Given the filter panel is open
When the user sets Mass Min = 10000 and Mass Max = 50000 (Da)
Then the table shows only entries with 10000 ≤ molecular_weight ≤ 50000

Given Mass Min > Mass Max
When the filter is applied
Then an inline validation error is shown and no API request is made
```

### US-30 — Filter by evidence level

```
Given the filter panel is open
When the user selects one or more evidence levels (1..5) in a multi-select
Then the table shows only entries whose evidence_level is in the selected set
```

---

# Epic 11: Data Lifecycle

> Tickets: `IMPORT-001`, `GENE-001`

### US-31 — Indicate stale entries

```
Given an entry was flagged stale=TRUE by the most recent re-import
When the entry appears in the Gene Explorer table
Then a "Stale" badge is rendered next to the accession
  and a tooltip explains: "This entry was not present in the latest UniProt import"

Given the user enables the "Hide stale entries" filter
When the table reloads
Then no entries with stale=TRUE are returned
```

---

# Epic 12: Dashboard

> Tickets: `DASH-001`, `ANALYTICS-001`

### US-32 — Dashboard KPIs

```
Given an authenticated user opens the Dashboard
When the page loads
Then the following KPI cards are displayed, sourced from mv_dashboard_kpis:
  Total proteins, Reviewed count, Distinct organisms, Average length, Top 5 organisms
  and each card response time is ≤ 500 ms (NFR §12.1)

When the data backing mv_dashboard_kpis was refreshed less than 1 hour ago
Then a "Last updated: <timestamp>" footer is shown on each card
```

### US-33 — Keyword frequency chart

```
Given the Analytics page is loaded
When the Keyword Frequency chart renders
Then it shows the top 20 keywords by protein count, sourced from mv_keyword_frequency
  rendered as a horizontal bar chart with counts on hover
```

### US-34 — Reviewed ratio chart

```
Given the Analytics page is loaded
When the Reviewed Ratio donut chart renders
Then it shows two segments — Reviewed and Unreviewed — sourced from mv_reviewed_ratio
  with counts and percentages in tooltips
```

### US-35 — Length-vs-weight scatter

```
Given the Analytics page is loaded
When the user opens the "Length vs Weight" tab
Then a scatter plot renders (server-paginated, max 5000 points)
  with length on the X axis (AA) and molecular weight on the Y axis (Da)
  and points are color-coded by reviewed status
```

---

# Epic 13: UX & Accessibility

> Tickets: `A11Y-001`, `GENE-003`

### US-36 — Loading, error, and empty states

```
For every list/table/chart view
Given the data is being fetched
Then a skeleton or spinner is rendered (loading state)

Given the request fails
Then an inline error block is rendered with retry button (error state)

Given the request returns an empty result
Then a clear empty-state message is rendered (empty state)
```

**Implementation notes**
- Reuse a shared `<app-state-host>` component that takes a discriminated union
  `{ status: 'loading'|'error'|'empty'|'ready', data?, error? }`.

---

### US-37 — Accessibility (WCAG AA)

```
Given any page is rendered
Then it passes axe-core checks with 0 violations of severity ≥ "serious"
  and all interactive elements are reachable via keyboard
  and color contrast meets WCAG AA (≥ 4.5:1 for normal text)
  and form fields have associated <label> elements or aria-label
  and dynamic content updates use aria-live where appropriate
```

**Implementation notes**
- Add `@axe-core/playwright` to e2e suite; fail the build on serious issues.

---

### US-41 — Change own password

```
Given an authenticated user opens the Profile page
When the user submits current password + new password (≥ 12 chars, mixed case + digit)
Then the password is updated, all refresh tokens are revoked
  and the user is asked to log in again

Given the new password fails complexity rules
When Submit is clicked
Then an inline validation error lists the failed rules and no API call is made
```

---

### US-42 — Theme toggle (light/dark)

```
Given the user clicks the theme toggle in the navbar
When the toggle changes
Then the application switches between light and dark themes
  and the choice persists in localStorage and is restored on next visit
  and contrast still meets WCAG AA in both themes
```

---

# Epic 14: Operations & Hardening

> Tickets: `OPS-001`

### US-38 — Audit log

```
Given an admin performs an action (login, import upload, import cancel,
  saved-filter delete-other-user)
When the action completes (success or failure)
Then a row is inserted in audit_log
  with: actor_user_id, action, target_type, target_id, status, ip_address, created_at
```

### US-39 — Health & readiness endpoints

```
Given the application is running
When a load balancer calls GET /actuator/health/liveness
Then HTTP 200 with body { status: "UP" } is returned

When a load balancer calls GET /actuator/health/readiness
Then HTTP 200 is returned only if the database is reachable
  and the most recent Flyway migration is up-to-date
```

### US-40 — Pagination & rate limiting

```
Given a client calls any list endpoint with size > 200
Then HTTP 400 is returned with error "Page size must not exceed 200"

Given a client makes more than 60 requests/minute to /api/genes/search
  from the same IP
Then the 61st request returns HTTP 429 with Retry-After header
```

**Implementation notes**
- Use Bucket4j with in-memory bucket per IP for MVP (Redis-backed when scaled).

---

# Definition of Done (every story)

A story is **only** considered done when:

1. Backend code respects the layered architecture
   (`controller → service → repository → dto → mapper → entity`).
2. DTO validation annotations match [validation-rules.md](validation-rules.md).
3. Endpoints conform to [api-contract.md](api-contract.md) (status codes,
   pagination envelope, error shape).
4. **Unit tests** cover the new service logic and Angular component logic.
5. **Integration tests** cover the new endpoint paths (happy + error).
6. **Coverage ≥ 80 %** measured by JaCoCo (backend) and Jest/Karma (frontend).
7. Authorization matches the matrix in [overview.md §13](overview.md).
8. Any new database object is added via a new Flyway migration
   (`backend/src/main/resources/db/migration/V<n>__<desc>.sql`).
9. Frontend states (loading / error / empty) are implemented (US-36).
10. Accessibility checks (US-37) pass on changed pages.
11. `documentation/implementation/<TICKET>/journal.md` is updated with
    coverage numbers and any deviations from this plan.
