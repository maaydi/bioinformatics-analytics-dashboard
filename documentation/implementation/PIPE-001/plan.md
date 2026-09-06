# PIPE-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Resolve ambiguities (file storage, Excel library, segment assembly, provider compatibility)
3. Create DB migration for `export_pipeline` and `export_job_execution` tables
4. Implement entity layer (`ExportPipeline`, `ExportJobExecution`)
5. Implement repository layer
6. Implement DTOs and MapStruct mappers
7. Implement `ExportFileStorageService`
8. Implement format writers (CSV, TSV, JSON, Excel)
9. Implement Spring Batch job config (`ExportJobConfig`)
10. Implement batch components (`ExportItemReader`, `ExportItemProcessor`, `ExportItemWriter`, `ExportJobListener`)
11. Implement `ExportPipelineService`
12. Implement `ExportPipelineController`
13. Add audit hooks for pipeline events
14. Implement scheduled cleanup job for old exports
15. Implement Angular models
16. Implement `ExportPipelineService` (frontend)
17. Implement `ExportPipelineWizardComponent`
18. Implement `FieldPickerComponent`
19. Implement `ExportPipelineListComponent`
20. Write backend unit tests
21. Write frontend unit tests
22. Write integration tests
23. Update documentation and journal

## Status

- [x] Requirements analyzed
- [x] Ambiguities resolved (see analyse.md)
- [x] DB migration created
- [x] Entities implemented
- [x] Repositories implemented
- [x] DTOs and mappers implemented
- [x] ExportFileStorageService implemented
- [ ] Format writers implemented
- [ ] Spring Batch job config implemented
- [ ] Batch components implemented
- [ ] ExportPipelineService implemented
- [ ] ExportPipelineController implemented
- [ ] Audit hooks wired
- [ ] Scheduled cleanup job implemented
- [ ] Angular models defined
- [ ] Frontend service implemented
- [ ] ExportPipelineWizardComponent implemented
- [ ] FieldPickerComponent implemented
- [ ] ExportPipelineListComponent implemented
- [ ] Backend unit tests written
- [ ] Frontend unit tests written
- [ ] Integration tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80 %

---

## Detailed Checklist

### Database Migration (`V1__export_pipeline.sql`)

- [x] `export_pipeline` table created with all required columns and indexes
- [x] `export_job_execution` table created for chunk progress tracking
- [x] Migration file: `backend/services/export-service/src/main/resources/db/migration/V1__export_pipeline.sql`

### Backend — Entity Layer

- [x] `ExportPipeline` entity:
    - [x] `id: Long` (auto-generated), `userId: String` (username), `name: String`, `description: String`
    - [x] `filterJson: JsonNode` (`@JdbcTypeCode(SqlTypes.JSON)`) — serialized GeneSearchRequest
    - [x] `format: ExportFormat` (enum: CSV, TSV, JSON, EXCEL)
    - [x] `fieldSchema: JsonNode` (`@JdbcTypeCode(SqlTypes.JSON)`) — ordered field names as JSONB array
    - [x] `status: ExportStatus` (enum: QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED)
    - [x] `estimatedRows: Long`, `actualRows: Long`
    - [x] `filePath: String`, `fileSizeBytes: Long`
    - [x] `errorMessage: String`, `jobExecutionId: Long`
    - [x] `createdAt`, `startedAt`, `completedAt`, `deletedAt`, `durationMs` (Instant)
    - [x] Helper methods: `isTerminal()`, `isDeleted()`
    - [x] `@PrePersist` lifecycle hook for `createdAt`
- [x] `ExportJobExecution` entity (tracks chunk progress):
    - [x] `id: Long` (auto-generated)
    - [x] `pipeline: ExportPipeline` (ManyToOne, LAZY fetch, cascade delete)
    - [x] `jobExecutionId: Long` (unique, reference to Spring Batch job execution)
    - [x] `chunksTotal: Integer`, `chunksProcessed: Integer`
    - [x] `updatedAt: Instant`
    - [x] Helper method: `getProgressPercent()`
    - [x] `@PrePersist` and `@PreUpdate` lifecycle hooks

### Backend — Repository Layer

- [x] `ExportPipelineRepository extends JpaRepository<ExportPipeline, Long>`:
    - [x] `findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId, Pageable pageable): Page<ExportPipeline>`
    - [x] 
      `findByUserIdAndStatusAndDeletedAtIsNull(String userId, ExportStatus status, Pageable pageable): Page<ExportPipeline>`
    - [x] `findByIdAndUserIdAndDeletedAtIsNull(Long id, String userId): Optional<ExportPipeline>`
    - [x] `countByUserIdAndStatusAndDeletedAtIsNull(String userId, ExportStatus status): long`
    - [x] Repository file:
      `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/repository/ExportPipelineRepository.java`
- [x] `ExportJobExecutionRepository extends JpaRepository<ExportJobExecution, Long>`:
    - [x] `findByPipelineId(Long pipelineId): Optional<ExportJobExecution>`
    - [x] Repository file:
      `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/repository/ExportJobExecutionRepository.java`

### Backend — DTOs

- [x] `ExportPipelineCreateRequest`:
    - [x] `@NotBlank @Size(max=200) String name`
    - [x] `@Size(max=500) String description`
    - [x] `@NotNull JsonNode filter` — filter criteria (flexible JSON structure)
    - [x] `@NotNull ExportFormat format`
    - [x] `@NotEmpty @Size(max=50) List<@NotBlank String> fieldSchema`
    - [x] DTO file:
      `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/dto/ExportPipelineCreateRequest.java`
- [x] `ExportPipelineResponse`:
    - [x] `Long id`, `String name`, `String description`, `ExportFormat format`, `List<String> fieldSchema`
    - [x] `ExportStatus status`, `Long estimatedRows`, `Long actualRows`
    - [x] `String filePath`, `Long fileSizeBytes`, `String errorMessage`
    - [x] `Instant createdAt`, `Instant startedAt`, `Instant completedAt`, `Long durationMs`
    - [x] DTO file:
      `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/dto/ExportPipelineResponse.java`
- [x] `ExportJobStatusResponse`:
    - [x] `Long pipelineId`, `ExportStatus status`, `Integer progressPercent`
    - [x] `Integer chunksProcessed`, `Integer chunksTotal`, `String currentStep`
    - [x] `Instant updatedAt`
    - [x] DTO file:
      `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/dto/ExportJobStatusResponse.java`
- [x] `ExportFieldSchemaDto`:
    - [x] `String fieldName`, `String displayName`, `String dataType` (STRING, NUMBER, BOOLEAN, DATE, ARRAY)
    - [x] `String description`, `boolean available`
    - [x] DTO file:
      `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/dto/ExportFieldSchemaDto.java`
- [x] `DownloadUrlDto`:
    - [x] `String downloadUrl`, `String filename`, `Long fileSizeBytes`, `String contentType`
    - [x] DTO file:
      `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/dto/DownloadUrlDto.java`
- [x] `ExportPipelineRetryRequest`:
    - [x] `@NotNull Long pipelineId` (re-run existing pipeline with same config)
    - [x] DTO file:
      `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/dto/ExportPipelineRetryRequest.java`

### Backend — Mappers

- [x] `ExportPipelineMapper` (MapStruct):
    - [x] `toDto(ExportPipeline): ExportPipelineResponse` — converts JSONB fieldSchema to List<String>
    - [x] `toEntity(ExportPipelineCreateRequest, String userId): ExportPipeline` — creates entity from request
    - [x] Mapper file:
      `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/mapper/ExportPipelineMapper.java`

### Backend — File Storage Service

- [x] `ExportFileStorageService` (`service/export/`):
    - [x] `createPipelineDirectory(Long userId, Long pipelineId): Path`
        - [x] Use APP_EXPORT_TEMP_DIR=/tmp/.bio-export
        - [x] Creates `${APP_EXPORT_TEMP_DIR}/{userId}/{pipelineId}/`
        - [x] Creates `segments/` subdirectory
    - [x] `getSegmentPath(Long userId, Long pipelineId, int chunkNumber, ExportFormat format): Path`
    - [x] `getFinalFilePath(Long userId, Long pipelineId, ExportFormat format): Path`
    - [x] `assembleSegments(Long userId, Long pipelineId, ExportFormat format): Path`
        - [x] Reads all segment files in order
        - [x] For CSV/TSV/JSON: streams segments into final file (concatenation)
        - [x] For Excel: opens each segment workbook, copies sheets into master workbook (see note)
    - [x] `deletePipelineDirectory(Long userId, Long pipelineId): void`
    - [x] `getFileSize(Long userId, Long pipelineId, ExportFormat format): Long`
    - [x] `validateFileExists(Long userId, Long pipelineId, ExportFormat format): boolean`

### Backend — Format Writers

- [ ] `ExportFormatWriter` interface:
    - [ ] `void writeHeader(List<String> fields, OutputStream out) throws IOException`
    - [ ] `void writeRow(Map<String, Object> row, List<String> fields, OutputStream out) throws IOException`
    - [ ] `void close(OutputStream out) throws IOException`
    - [ ] `String getFileExtension()`
    - [ ] `String getContentType()`
- [ ] `CsvExportWriter` — Apache Commons CSV, RFC 4180, UTF-8 BOM
- [ ] `TsvExportWriter` — Apache Commons CSV with TSV format, tab delimiter
- [ ] `JsonExportWriter` — Jackson `SequenceWriter`, writes `[` then rows as objects, then `]`
- [ ] `ExcelExportWriter` — Apache POI SXSSF (streaming), auto-size columns, freeze pane
- [ ] `ExportWriterFactory` — `getWriter(ExportFormat): ExportFormatWriter`

### Backend — Spring Batch Job Configuration

- [ ] `ExportJobConfig` (`batch/export/`):
    - [ ] Job name: `exportPipelineJob`
    - [ ] Step 1: `validateAndEstimateStep` (Tasklet)
        - [ ] Reads `filterJson` from job parameters
        - [ ] Calls `GeneService.count()` with specification to get estimated rows
        - [ ] If estimated rows == 0: fail job with exit code `NO_DATA`
        - [ ] If estimated rows > 1,000,000: log warning but continue
        - [ ] Updates `ExportPipeline.estimatedRows` and `status = RUNNING`
    - [ ] Step 2: `exportChunkStep` (chunk-oriented)
        - [ ] Chunk size: 500 (configurable via `app.export.chunk-size`)
        - [ ] Reader: `ExportItemReader` (see below)
        - [ ] Processor: `ExportItemProcessor` — `ProteinEntry` → `Map<String, Object>`
        - [ ] Writer: `ExportItemWriter` — writes to segment files
        - [ ] Listener: `ChunkListener` updates `ExportJobExecution.chunksProcessed`
    - [ ] Step 3: `assembleAndFinalizeStep` (Tasklet)
        - [ ] Calls `ExportFileStorageService.assembleSegments()`
        - [ ] Updates `ExportPipeline` with `filePath`, `fileSizeBytes`, `actualRows`, `status = COMPLETED`
        - [ ] Cleans up segment files
    - [ ] Job listener: `ExportJobListener` (implements `JobExecutionListener`)
        - [ ] `beforeJob`: set `startedAt = NOW()`
        - [ ] `afterJob`: if FAILED, set `status = FAILED`, populate `errorMessage`, cleanup segments
- [ ] Job parameters:
    - [ ] `pipelineId` (Long)
    - [ ] `userId` (Long)
    - [ ] `format` (String)
    - [ ] `fieldSchema` (JSON string)
    - [ ] `filterJson` (JSON string)

### Backend — Batch Components

- [ ] `ExportItemReader` (`batch/export/`):
    - [ ] Extends `JpaPagingItemReader<ProteinEntry>` for Postgres provider
    - [ ] For UniProt provider: implements `ItemReader<ProteinSummaryDto>` with cursor-based pagination via
      `UniprotKbRestService`
    - [ ] Applies `GeneSpecification` from deserialized `filterJson`
    - [ ] Page size = chunk size
    - [ ] `read()` returns null when no more data
- [ ] `ExportItemProcessor` (`batch/export/`):
    - [ ] `process(ProteinEntry protein): Map<String, Object>`
    - [ ] Extracts only the fields listed in `fieldSchema` from the entity
    - [ ] Handles nested collections:
        - [ ] `keywords` → comma-separated string or JSON array (depending on format)
        - [ ] `goTerms` → list of `goId`
        - [ ] `features` → count or list of `featureType`
        - [ ] `crossReferences` → count or list of `source:identifier`
        - [ ] `comments` → list of `commentType: text`
        - [ ] `publications` → count or list of `pubmedId`
        - [ ] `hostOrganisms` → count or list of `name`
    - [ ] Null-safe: missing fields render as empty string/0/null
- [ ] `ExportItemWriter` (`batch/export/`):
    - [ ] `write(Chunk<? extends Map<String, Object>> chunk)`
    - [ ] Opens segment file for the current chunk number
    - [ ] Delegates to `ExportFormatWriter` for each row
    - [ ] Closes file after chunk
    - [ ] For Excel: maintains a single SXSSF workbook across chunks (not segments); flushes rows periodically

### Backend — Service Layer

- [ ] `ExportPipelineService` (`service/export/`):
    - [ ] `createPipeline(ExportPipelineCreateRequest request, AppUser user): ExportPipelineResponse`
        - [ ] Validates filter yields > 0 rows (pre-check via `GeneService.count()`)
        - [ ] Persists pipeline with status = QUEUED
        - [ ] Launches Spring Batch job asynchronously via `JobLauncher.run()`
        - [ ] Returns response immediately (HTTP 202)
    - [ ] `listPipelines(ExportStatus status, Pageable pageable, AppUser user): Page<ExportPipelineResponse>`
        - [ ] Filters by user + status (optional) + not deleted
    - [ ] `getPipelineStatus(Long pipelineId, AppUser user): ExportJobStatusResponse`
        - [ ] Reads `ExportPipeline` + `ExportJobExecution` for progress
        - [ ] Calculates `progressPercent = (chunksProcessed / chunksTotal) * 100`
        - [ ] If COMPLETED/FAILED, returns final state
    - [ ] `getDownloadUrl(Long pipelineId, AppUser user): DownloadUrlDto`
        - [ ] Verifies ownership
        - [ ] Verifies status = COMPLETED
        - [ ] Returns direct download URL: `/api/exports/pipelines/{id}/download-file` (streamed)
    - [ ] `retryPipeline(Long pipelineId, AppUser user): ExportPipelineResponse`
        - [ ] Clones existing pipeline config, resets status to QUEUED, launches new job
    - [ ] `deletePipeline(Long pipelineId, AppUser user): void`
        - [ ] Soft delete: sets `deletedAt = NOW()`
        - [ ] If job is RUNNING, calls `JobOperator.stop()` first
        - [ ] Schedules physical file deletion after 30 days
    - [ ] `getAvailableFields(): List<ExportFieldSchemaDto>`
        - [ ] Returns all possible export fields with metadata for the field picker

### Backend — Controller

- [ ] `ExportPipelineController` (`controller/`):
    - [ ] `POST /api/exports/pipelines` → `201 Created` with `ExportPipelineResponse`
        - [ ] `@Valid @RequestBody ExportPipelineCreateRequest`
        - [ ] Returns immediately (async); body includes pipelineId for polling
    - [ ] `GET /api/exports/pipelines` → `200 OK` with `PagedResponse<ExportPipelineResponse>`
        - [ ] Query param: `status` (optional filter)
        - [ ] Query param: `page`, `size` (max 50)
    - [ ] `GET /api/exports/pipelines/{id}` → `200 OK` with `ExportPipelineResponse`
        - [ ] Returns full pipeline details
    - [ ] `GET /api/exports/pipelines/{id}/status` → `200 OK` with `ExportJobStatusResponse`
        - [ ] Frontend polls this every 3 seconds
    - [ ] `GET /api/exports/pipelines/{id}/download` → `200 OK` with `DownloadUrlDto`
        - [ ] Returns metadata + presigned/direct URL
    - [ ] `GET /api/exports/pipelines/{id}/download-file` → streams file bytes
        - [ ] `Content-Type` from `ExportFormatWriter.getContentType()`
        - [ ] `Content-Disposition: attachment; filename="..."`
        - [ ] Streams via `InputStreamResource` to avoid loading file in memory
    - [ ] `POST /api/exports/pipelines/{id}/retry` → `202 Accepted`
    - [ ] `DELETE /api/exports/pipelines/{id}` → `204 No Content`
    - [ ] `GET /api/exports/fields` → `200 OK` with `List<ExportFieldSchemaDto>`
        - [ ] Returns available fields for the field picker
    - [ ] Error responses:
        - [ ] `400` — validation failure, 0-row filter
        - [ ] `401` — missing JWT
        - [ ] `403` — pipeline belongs to another user
        - [ ] `404` — pipeline not found
        - [ ] `409` — pipeline not in a retryable state
        - [ ] `410` — file expired (deleted after retention)

### Backend — Audit & Cleanup

- [ ] `ExportPipelineAuditListener`:
    - [ ] Records `EXPORT_PIPELINE_CREATED`, `EXPORT_PIPELINE_COMPLETED`, `EXPORT_PIPELINE_FAILED` in `audit_log`
      (reuses OPS-001)
- [ ] `ExportCleanupJob` (`@Scheduled(cron = "0 0 2 * * SUN")`):
    - [ ] Finds pipelines with `deletedAt < NOW() - INTERVAL '30 days'`
    - [ ] Deletes physical files via `ExportFileStorageService`
    - [ ] Hard-deletes DB records
    - [ ] Logs count of cleaned records

### Frontend — Models (`core/models/export-pipeline.model.ts`)

- [ ] `ExportPipeline`:
    - [ ] `id: number`, `name: string`, `description?: string`, `format: ExportFormat`
    - [ ] `fieldSchema: string[]`, `status: ExportStatus`, `estimatedRows?: number`, `actualRows?: number`
    - [ ] `fileSizeBytes?: number`, `errorMessage?: string`, `createdAt: string`, `completedAt?: string`
- [ ] `ExportPipelineCreateRequest`:
    - [ ] `name: string`, `description?: string`, `filter: GeneSearchRequest`, `format: ExportFormat`,
      `fieldSchema: string[]`
- [ ] `ExportJobStatus`:
    - [ ] `pipelineId: number`, `status: ExportStatus`, `progressPercent: number`, `chunksProcessed?: number`,
      `chunksTotal?: number`, `currentStep?: string`
- [ ] `ExportFieldSchema`:
    - [ ] `fieldName: string`, `displayName: string`, `dataType: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE' | 'ARRAY'`,
      `description: string`
- [ ] `DownloadUrl`:
    - [ ] `downloadUrl: string`, `filename: string`, `fileSizeBytes: number`, `contentType: string`
- [ ] `ExportFormat` — enum: `'CSV' | 'TSV' | 'JSON' | 'EXCEL'`
- [ ] `ExportStatus` — enum: `'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'`

### Frontend — Service (`features/export/export-pipeline.service.ts`)

- [ ] `createPipeline(request: ExportPipelineCreateRequest): Observable<ExportPipeline>`
- [ ] `listPipelines(status?: ExportStatus, page?: number, size?: number): Observable<PagedResponse<ExportPipeline>>`
- [ ] `getPipeline(id: number): Observable<ExportPipeline>`
- [ ] `getStatus(id: number): Observable<ExportJobStatus>`
- [ ] `getDownloadUrl(id: number): Observable<DownloadUrl>`
- [ ] `downloadFile(id: number): Observable<Blob>` — calls `/download-file`
- [ ] `retryPipeline(id: number): Observable<ExportPipeline>`
- [ ] `deletePipeline(id: number): Observable<void>`
- [ ] `getAvailableFields(): Observable<ExportFieldSchema[]>`
- [ ] `pollStatus(id: number, intervalMs = 3000): Observable<ExportJobStatus>` — wraps `getStatus` with `interval()` +
  `takeWhile(status !== 'COMPLETED' && status !== 'FAILED')`

### Frontend — `ExportPipelineWizardComponent` (`features/export/export-pipeline-wizard/`)

- [ ] `export-pipeline-wizard.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone, `MatDialog` or route-based
- [ ] Stepper (Angular Material `mat-stepper`):
    - [ ] **Step 1 — Filter Review:**
        - [ ] Displays current `filtersStore` state as read-only chips
        - [ ] Button: "Save this filter" → opens `SaveFilterDialog` (reuses FILTER-001)
        - [ ] Shows estimated row count (calls `GeneService.count()` with current filters)
        - [ ] Validation: if count == 0, disable next step
    - [ ] **Step 2 — Format Selection:**
        - [ ] Radio group: CSV, TSV, JSON, Excel
        - [ ] Each option shows icon + brief description + estimated file size hint
        - [ ] Default: CSV
    - [ ] **Step 3 — Field Selection:**
        - [ ] Toggle: "All Fields" / "Custom Fields"
        - [ ] If Custom: `<app-field-picker>` dual-list component
        - [ ] If All: all fields pre-selected, picker hidden
    - [ ] **Step 4 — Summary & Confirm:**
        - [ ] Name input (default: `Export_${date}`)
        - [ ] Description textarea (optional)
        - [ ] Summary card: filter count, format, field count, estimated rows, estimated size
        - [ ] "Create & Run" button (primary) + "Cancel" button
- [ ] Outputs:
    - [ ] `pipelineCreated = output<ExportPipeline>()`

### Frontend — `FieldPickerComponent` (`shared/components/field-picker/`)

- [ ] `field-picker.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone, reusable
- [ ] `field-picker.component.html` — external template
- [ ] Inputs:
    - [ ] `availableFields: ExportFieldSchema[]`
    - [ ] `selectedFields: string[]` (two-way binding via `selectedFieldsChange`)
- [ ] Layout:
  ```
  ┌─ Available Fields ──────┬─ Selected Fields ───────┐
  │ [🔍 Search...]          │ [🔍 Search...]          │
  │                         │                         │
  │ □ accession             │ ☰ accession        [×]  │
  │ □ entryName             │ ☰ geneNamePrimary  [×]  │
  │ □ proteinFullName       │ ☰ organismName     [×]  │
  │ ...                     │ ...                     │
  │                         │                         │
  │ [► Add Selected]        │ [◄ Remove Selected]     │
  └─────────────────────────┴─────────────────────────┘
  ```
- [ ] Features:
    - [ ] Checkbox multi-select in Available list
    - [ ] Drag-and-drop reordering in Selected list (Angular CDK `drag-drop`)
    - [ ] Search filters both lists
    - [ ] "Add All" / "Remove All" buttons
    - [ ] Selected count badge: "23 fields selected"
    - [ ] Validation: minimum 1 field selected
- [ ] Accessibility:
    - [ ] `aria-label` on each list
    - [ ] Keyboard: arrow keys navigate, Space toggles selection, Enter moves item

### Frontend — `ExportPipelineListComponent` (`features/export/export-pipeline-list/`)

- [ ] `export-pipeline-list.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `export-pipeline-list.component.html` — external template
- [ ] `export-pipeline-list.component.scss` — design system tokens
- [ ] Layout:
    - [ ] Page header: "Export Pipelines" + "New Export" button
    - [ ] Filter bar: status chips (All / Queued / Running / Completed / Failed)
    - [ ] Table columns: Name | Format | Rows | Size | Status | Created | Actions
    - [ ] Status column: colored badge with icon
        - [ ] QUEUED → `hourglass_empty` icon, gray
        - [ ] RUNNING → `sync` animated icon, blue
        - [ ] COMPLETED → `check_circle` icon, green
        - [ ] FAILED → `error` icon, red
        - [ ] CANCELLED → `cancel` icon, orange
    - [ ] Actions column:
        - [ ] COMPLETED → Download button (primary icon)
        - [ ] FAILED → Retry button (warn icon) + error tooltip
        - [ ] All → Delete button (trash icon, confirmation dialog)
    - [ ] Progress bar for RUNNING pipelines (indeterminate or determinate)
- [ ] Polling:
    - [ ] `effect()` subscribes to `pollStatus()` for all RUNNING pipelines
    - [ ] Updates signals when status changes
    - [ ] Auto-stops polling when no RUNNING pipelines remain
- [ ] Empty state: "No export pipelines yet. Create your first export from the Gene Explorer."
- [ ] Error state: toast on failed retry or delete

### Frontend — Integration Points

- [ ] Add "Export via Pipeline" button to `GenesPageComponent` toolbar (next to existing "Export CSV")
    - [ ] Opens `ExportPipelineWizardComponent` with current filters pre-populated
- [ ] Add "Export Pipelines" route `/exports` to main navigation (behind `authGuard`)
- [ ] Add "Export History" quick link in user profile dropdown
- [ ] When pipeline completes, show toast notification (if user is on any page)
    - [ ] Uses `MatSnackBar` with action button "View"

### Tests — Backend

- [x] `ExportFileStorageServiceTest`:
    - [x] `createPipelineDirectory_createsExpectedStructure`
    - [x] `assembleSegments_concatenatesCsvFiles`
    - [x] `deletePipelineDirectory_removesAllFiles`
- [ ] `CsvExportWriterTest`:
    - [ ] `writeHeader_outputsCorrectColumns`
    - [ ] `writeRow_escapesCommasAndQuotes`
    - [ ] `writeRow_outputsUtf8Bom`
- [ ] `ExcelExportWriterTest`:
    - [ ] `writeRow_createsValidXlsx`
    - [ ] `close_finalizesWorkbook`
- [ ] `ExportItemProcessorTest`:
    - [ ] `process_extractsSelectedFields`
    - [ ] `process_handlesNullCollections`
    - [ ] `process_mapsNestedObjects`
- [ ] `ExportPipelineServiceTest`:
    - [ ] `createPipeline_validRequest_returnsQueuedPipeline`
    - [ ] `createPipeline_zeroRows_throws`
    - [ ] `getDownloadUrl_completedPipeline_returnsUrl`
    - [ ] `getDownloadUrl_incompletePipeline_throws`
    - [ ] `retryPipeline_failedPipeline_requeues`
    - [ ] `deletePipeline_softDeletesAndStopsJob`
- [ ] `ExportPipelineControllerIntegrationTest`:
    - [ ] `POST /api/exports/pipelines` → `201`
    - [ ] `GET /api/exports/pipelines` → `200` paginated
    - [ ] `GET /api/exports/pipelines/{id}/status` → `200` with progress
    - [ ] `GET /api/exports/pipelines/{id}/download-file` → streams file
    - [ ] `DELETE /api/exports/pipelines/{id}` → `204`
    - [ ] `POST /api/exports/pipelines` with 0-row filter → `400`

### Tests — Frontend

- [ ] `ExportPipelineWizardComponent` unit tests:
    - [ ] Stepper advances through all 4 steps
    - [ ] Step 1 disables next if row count is 0
    - [ ] Step 3 shows field picker only for Custom Fields
    - [ ] Step 4 summary reflects selections
    - [ ] Emits `pipelineCreated` on confirm
- [ ] `FieldPickerComponent` unit tests:
    - [ ] Moving field updates selected list
    - [ ] Drag-and-drop reorders selected fields
    - [ ] Search filters available fields
    - [ ] Validation fails with 0 selected fields
- [ ] `ExportPipelineListComponent` unit tests:
    - [ ] Lists pipelines from service
    - [ ] Shows correct status badge per state
    - [ ] Polls status for running pipelines
    - [ ] Download button enabled only for COMPLETED
    - [ ] Retry button calls service on FAILED
- [ ] `ExportPipelineService` unit tests (HttpClientTestingModule):
    - [ ] `createPipeline` sends POST with correct body
    - [ ] `pollStatus` emits at interval until terminal state
    - [ ] `downloadFile` calls correct endpoint and returns Blob

### General

- [ ] No `ngClass` / `ngStyle` — `class` / `style` bindings only
- [ ] Native control flow (`@if`, `@for`, `@defer`)
- [ ] `ChangeDetectionStrategy.OnPush` on all new components
- [ ] AXE checks pass (stepper has `aria-label`, field picker has `aria-live` for selection changes)
- [ ] Code reviewed
- [ ] Coverage ≥ 80 % (JaCoCo + Jest)

---

## Risk Register

| ID | Risk                                                     | Probability | Mitigation                                                                                                            |
|----|----------------------------------------------------------|-------------|-----------------------------------------------------------------------------------------------------------------------|
| R1 | Excel SXSSF memory leak on very large files (>500K rows) | Medium      | Cap Excel exports at 500K rows; recommend CSV/JSON for larger sets; document limit                                    |
| R2 | Disk space exhaustion on APP_DIR                         | Medium      | Scheduled cleanup job; monitor disk usage; configurable max retention                                                 |
| R3 | Concurrent pipeline jobs overwhelm database              | Low         | Spring Batch job concurrency limited to 2 export jobs via `SimpleJobLauncher` thread pool; queue remaining            |
| R4 | User deletes pipeline while job is RUNNING               | Low         | `deletePipeline` calls `JobOperator.stop()`; listener handles interrupted state gracefully                            |
| R5 | UniProt API export reader hits rate limits               | Medium      | Add retry with backoff in `UniProtApiExportItemReader`; fallback to error state                                       |
| R6 | File download exposes path traversal                     | Low         | `ExportFileStorageService` validates all paths are within `${APP_DIR}/exports/{userId}/`; controller checks ownership |
| R7 | JSON export of nested arrays produces unwieldy files     | Low         | For JSON format, nested collections are serialized as compact arrays of strings; document schema                      |

---

## Commands

```bash
# Run backend tests
cd backend
./mvnw -Dtest=com.bioinformatics.dashboard.export.*Test test

# Run frontend tests
cd frontend
ng test --include='**/export-*/*'

# Verify export directory structure
ls -la ${APP_DIR}/exports/

# Trigger manual cleanup (for testing)
curl -X POST http://localhost:8080/api/admin/exports/cleanup   -H "Authorization: Bearer $ADMIN_TOKEN"
```
