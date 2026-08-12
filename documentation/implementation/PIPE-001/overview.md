# PIPE-001 — Visual Export Pipeline & Batch Export Engine

## Description

Replace direct synchronous export calls with an asynchronous **Export Pipeline** system backed by Spring Batch. Users
configure export jobs through a visual pipeline builder or a dedicated export wizard, selecting data source (filtered
gene set, saved filter, or compare result), output format (CSV, TSV, JSON, Excel), and field schema (all fields or
custom subset). The system queues the job, processes it in chunked Spring Batch steps, persists the output file, and
surfaces a secure download link upon completion.

This eliminates the timeout and memory-pressure risks inherent in streaming large result sets directly through the HTTP
response thread, while providing users with visibility into export progress, history, and retry capabilities.

---

## Scope

| Layer                    | Artifact                                                                                                                     | Description                                                                                                                                       |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| **Backend — Entity**     | `ExportPipeline`                                                                                                             | Pipeline definition: name, filter snapshot, format, field selection, status                                                                       |
| **Backend — Entity**     | `ExportJobExecution`                                                                                                         | Tracks each Spring Batch execution tied to a pipeline                                                                                             |
| **Backend — Repository** | `ExportPipelineRepository`, `ExportJobExecutionRepository`                                                                   | CRUD + query by user + status                                                                                                                     |
| **Backend — DTOs**       | `ExportPipelineCreateRequest`, `ExportPipelineResponse`, `ExportJobStatusResponse`, `ExportFieldSchemaDto`, `DownloadUrlDto` | API contracts                                                                                                                                     |
| **Backend — Service**    | `ExportPipelineService`                                                                                                      | Orchestrates pipeline CRUD, validation, and job launch                                                                                            |
| **Backend — Batch**      | `ExportJobConfig`, `ExportItemReader`, `ExportItemProcessor`, `ExportItemWriter`                                             | Spring Batch job for all formats                                                                                                                  |
| **Backend — Writer**     | `CsvExportWriter`, `TsvExportWriter`, `JsonExportWriter`, `ExcelExportWriter`                                                | Format-specific file writers                                                                                                                      |
| **Backend — Storage**    | `ExportFileStorageService`                                                                                                   | Writes to `APP_DIR/exports/{userId}/{pipelineId}/`                                                                                                |
| **Backend — Controller** | `ExportPipelineController`                                                                                                   | `POST /api/exports/pipelines`, `GET /api/exports/pipelines`, `GET /api/exports/pipelines/{id}/status`, `GET /api/exports/pipelines/{id}/download` |
| **Frontend — Component** | `export-pipeline-wizard.component`                                                                                           | Stepper wizard: filter → format → fields → confirm                                                                                                |
| **Frontend — Component** | `export-pipeline-list.component`                                                                                             | Pipeline history table with status, progress, download links                                                                                      |
| **Frontend — Component** | `field-picker.component`                                                                                                     | Dual-list field selector for custom schemas                                                                                                       |
| **Frontend — Service**   | `export-pipeline.service.ts`                                                                                                 | API calls + polling logic                                                                                                                         |
| **Frontend — Model**     | `export-pipeline.model.ts`                                                                                                   | TypeScript interfaces                                                                                                                             |

---

## Acceptance Criteria

### AC-1 — Pipeline Creation Wizard

```
Given the user is on the Gene Explorer with active filters applied
When they click "Export via Pipeline"
Then the Export Pipeline Wizard opens as a stepper dialog
And Step 1 shows the current filter summary (read-only) with option to save as named filter
And Step 2 lets the user select format: CSV, TSV, JSON, Excel
And Step 3 lets the user choose "All Fields" or "Custom Fields"
And if Custom Fields is selected, a dual-list picker shows available fields
  with "ProteinSummary" fields on the left and selected fields on the right
And Step 4 shows a summary: estimated row count, format, fields, filename
And the user can name the pipeline (optional, defaults to "Export_{timestamp}")
When they click "Create & Run"
Then the pipeline is persisted with status = QUEUED
And a Spring Batch job is launched asynchronously
And the wizard closes with a toast: "Export pipeline '{name}' queued."
```

### AC-2 — Supported Export Formats

```
Given a pipeline is configured with format = CSV
When the batch job executes
Then the output file is RFC 4180 compliant with UTF-8 BOM
And headers match the selected field names in camelCase
And string fields containing commas or quotes are properly escaped

Given format = TSV
When the batch job executes
Then fields are tab-separated with LF line endings
And no escaping is applied beyond tab replacement

Given format = JSON
When the batch job executes
Then the output is a JSON array of objects, one per protein
And each object contains only the selected fields
And the file is minified (no pretty-print) to reduce size

Given format = Excel
When the batch job executes
Then an .xlsx file is generated with one sheet named "Proteins"
And the header row is bold with a freeze pane
And columns are auto-sized to content
And the file supports up to 1,048,576 rows (Excel limit)
```

### AC-3 — Custom Field Selection

```
Given the user selects "Custom Fields" in the wizard
When they move fields from Available to Selected
Then the Available list updates to exclude selected fields
And the Selected list supports drag-and-drop reordering
And the order in Selected determines column order in the output
And the user can select from the following field pool:
  id, accession, entryName, proteinFullName, proteinShortName,
  geneNamePrimary, geneNameSynonyms, geneOrfNames,
  organismName, organismCommonName, taxid, lineage,
  reviewed, length, molecularWeight, evidenceLevel,
  keywords, goTerms, features, crossReferences,
  proteinEcNumber, sequenceVersion, entryVersion,
  integratedDate, sequenceDate, updatedDate,
  sequence, sequenceChecksum, comments, publications, hostOrganisms
And at minimum 1 field must be selected (validation error if 0)
And "All Fields" selects the full pool automatically
```

### AC-4 — Asynchronous Batch Execution

```
Given a pipeline with estimated row count = 150,000
When the job starts
Then the status transitions: QUEUED → RUNNING → COMPLETED
And the batch job uses chunk size = 500 (reuses existing import chunk config)
And each chunk writes to a temporary file segment
And on COMPLETED, segments are concatenated into the final file
And the file is moved to APP_DIR/exports/{userId}/{pipelineId}/export.{ext}
And the pipeline record is updated with:
  filePath, fileSizeBytes, rowCountActual, completedAt, durationMs
And the user sees a download link in the pipeline list
```

### AC-5 — Pipeline Status Polling

```
Given a pipeline is in status = RUNNING
When the user views the Export Pipelines page
Then a progress bar shows: (chunksProcessed / totalChunks) × 100
And the status badge color follows:
  QUEUED  → gray
  RUNNING → blue (animated pulse)
  COMPLETED → green
  FAILED  → red
  CANCELLED → orange
And the page auto-refreshes every 3 seconds via polling
And the user can click "Refresh Now" to force an update
```

### AC-6 — Download on Completion

```
Given a pipeline has status = COMPLETED
When the user clicks the "Download" button
Then a presigned or direct download URL is returned
And the file downloads with Content-Disposition: attachment
And the filename is: {pipelineName}_{yyyy-MM-dd_HH-mm}.{ext}
And if the file exceeds 50 MB, the user sees a warning:
  "File is large ({size}). Download may take a while."
And if multiple formats were requested (future: zip bundle),
  the download is a .zip containing all format files

Given the pipeline is not COMPLETED
When the user clicks Download
Then the button is disabled with tooltip:
  "Export is still processing. Download will be available when complete."
```

### AC-7 — Timeout & Large-Set Protection

```
Given a filter matches > 500,000 rows
When the user attempts to create a pipeline
Then the backend returns HTTP 202 (accepted) immediately — no timeout
And the Spring Batch job processes the full set in background chunks
And the HTTP thread is released within 500 ms
And the user can close the browser and return later to download

Given a filter matches 0 rows
When the user attempts to create a pipeline
Then the wizard shows a validation error before submission:
  "Current filter yields 0 results. Nothing to export."
And the "Create & Run" button is disabled
```

### AC-8 — Pipeline History & Management

```
Given the user navigates to /exports
When the page loads
Then a paginated table shows all their pipelines with columns:
  Name | Created | Format | Rows | Size | Status | Actions (Download / Retry / Delete)
And pipelines are sorted by createdAt DESC
And the user can filter by status (All / Completed / Failed / Running)
And clicking "Retry" on a FAILED pipeline re-queues the same job
And clicking "Delete" removes the pipeline record and its file from disk
And deleted pipelines are soft-deleted (deletedAt timestamp) for 30 days
```

### AC-9 — Error Handling & Notification

```
Given a pipeline fails during batch execution (e.g., disk full)
When the job transitions to FAILED
Then the pipeline status = FAILED
And the errorMessage field is populated from Spring Batch exit description
And the user sees a red error state in the pipeline list
And a toast notification appears if the user is on the site:
  "Export '{name}' failed: {errorMessage}"
And the partial output file is cleaned up automatically
```

### AC-10 — Security & Authorization

```
Given an authenticated user creates a pipeline
When the file is written to disk
Then it is stored under APP_DIR/exports/{userId}/
And another user cannot access files outside their own userId directory
And the download endpoint verifies the requesting user owns the pipeline
And ADMIN users can view all pipelines but still cannot download another user's file

Given an unauthenticated request to POST /api/exports/pipelines
When it reaches the controller
Then HTTP 401 is returned
```

---

## Key Design Decisions

### Spring Batch for Export Processing

- **Rationale:** The application already has a mature Spring Batch infrastructure (IMPORT-001, RDF-001). Reusing it for
  export avoids introducing a second job framework.
- **Job name:** `exportPipelineJob`
- **Steps:**
    1. `validateAndEstimateStep` — validates filter, counts rows, rejects 0-row pipelines
    2. `exportChunkStep` — chunk-oriented read → process → write to temp segment files
    3. `assembleAndFinalizeStep` — concatenates segments, moves to final path, updates pipeline record
- **Reader:** `JpaPagingItemReader<ProteinEntry>` with `GeneSpecification` applied
- **Processor:** `ExportFieldExtractor` — maps `ProteinEntry` to `Map<String, Object>` based on selected fields
- **Writer:** `MultiResourceItemWriter` or custom `SegmentFileItemWriter` per format

### File Storage Strategy

- **Base path:** `${APP_DIR}/exports/{userId}/{pipelineId}/`
- **Temp segments:** `{base}/segments/chunk_{chunkNumber}.{ext}`
- **Final file:** `{base}/export.{ext}`
- **Cleanup:** On job failure, `JobExecutionListener` deletes partial segments. On success, segments are deleted after
  assembly.
- **Retention:** Soft-delete for 30 days; scheduled cleanup job (Spring `@Scheduled`) purges old files.

### Format Writers

| Format | Library                              | Notes                                       |
|--------|--------------------------------------|---------------------------------------------|
| CSV    | Apache Commons CSV                   | RFC 4180 compliant, streaming               |
| TSV    | Apache Commons CSV (with TSV format) | Tab delimiter, no quote handling needed     |
| JSON   | Jackson `SequenceWriter`             | Streaming array, low memory                 |
| Excel  | Apache POI SXSSF                     | Streaming XML, handles 1M+ rows without OOM |

### Pipeline vs Direct Export Coexistence

- The existing `POST /api/genes/export-csv` endpoint (EXPORT-001) remains for small, synchronous exports (< 10,000 rows,
  CSV only).
- PIPE-001 is the **recommended path** for all exports > 10,000 rows or requiring non-CSV formats.
- Future: EXPORT-001 may be deprecated or redirected to PIPE-001 internally.

### Provider Architecture Compatibility

- Export pipelines work with both `postgres` and `uniprotKb` providers because they consume `GeneService.search()`
  through the existing dispatcher.
- For `uniprotKb` provider, chunk reading is adapted: the reader fetches pages from the UniProt REST API instead of the
  local database. This is handled by a `UniProtApiExportItemReader` that implements `ItemReader<ProteinSummaryDto>`.

---

## References

- `documentation/api-contract.md` §1 — `POST /api/genes/export-csv` (existing sync export)
- `documentation/implementation/EXPORT-001/overview.md` — existing CSV export service
- `documentation/implementation/IMPORT-001/overview.md` — Spring Batch job patterns
- `documentation/implementation/RDF-001/plan.md` — `APP_DIR` property and file storage conventions
- `documentation/implementation/OPS-001/overview.md` — audit logging for pipeline events
- `documentation/implementation/REFACTOR-001/overview.md` — provider dispatcher pattern (for UniProt API export reader)
- Spring Batch Documentation: https://docs.spring.io/spring-batch/reference/
- Apache POI SXSSF: https://poi.apache.org/components/spreadsheet/

---

**Ticket Created**: 2026-08-12  
**Target Release**: Phase 6 (post core features stable)  
**Estimated Effort**: XL (8–10 weeks)
