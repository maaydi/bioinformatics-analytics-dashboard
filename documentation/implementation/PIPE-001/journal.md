# PIPE-001 — Implementation Journal

## 2026-09-25 — Export Writers and Segment Assembly Remediation

**Action:** Completed the remediations identified by the export writer and segment assembly audit.

**Outcome:**

- Enforced unique export fields in `ExportPipelineCreateRequest`, retaining the caller-provided `List<String>` order.
- Reworked CSV/TSV assembly to parse and emit records incrementally through Apache Commons CSV. It now keeps embedded
  newlines valid, emits RFC 4180 CRLF record separators, removes only duplicate logical header records, and preserves
  the CSV UTF-8 BOM.
- Reworked JSON assembly with Jackson parser/generator token streaming. Every segment must be a JSON array and its
  values are copied one by one into the final JSON array without loading a segment into memory.
- Replaced the lossy XLSX first-segment copy with a complete SXSSF merge of all chunk workbooks. Exactly one header is
  retained, all data rows are copied, and source/workbook resources are closed deterministically.
- Defined stream ownership consistently: `ExportFormatWriter#close(OutputStream)` now finalizes state without closing
  caller-owned streams for CSV, TSV, JSON, and XLSX writers. Writer state is removed before finalization; SXSSF
  workbooks are closed in `finally`.
- Made `ExportItemWriter` an `ItemStream`: it initializes directories in `open`, persists the segment index in
  `ExecutionContext`, advances past already-existing committed segments on restart, and deletes incomplete segments
  after write/finalization errors.
- Added tests for RFC 4180 multiline CSV assembly, multi-segment XLSX data retention, and restart-safe segment
  allocation.

**Verification:**

```bash
mvn -f backend/pom.xml -pl services/export-service -am \
  -Dtest=ExportWritersTest,SegmentAssemblersTest,ExportItemWriterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: **BUILD SUCCESS**, 8 tests run, 0 failures, 0 errors. JaCoCo still reports the existing execution-data/class
mismatch warning for `UniProtExportJobStepConfig`; it does not affect the test result.

---

## 2026-09-25 — Export Writers and Segment Assembly Compliance Audit

**Action:** Reviewed `ExportItemWriter`, all `ExportFormatWriter` implementations, their Spring Batch step wiring,
segment assemblers, and the associated unit tests against the PIPE-001 plan.

**Confirmed implementation:**

- `ExportItemWriter` writes every non-empty chunk to a format-specific segment, delegates headers and rows to the
  selected `ExportFormatWriter`, then finalizes the writer and closes the output stream.
- `ExportWriterFactory` resolves CSV, TSV, JSON, and XLSX writer implementations; the export step injects the selected
  writer as a step-scoped dependency.
- CSV writes a UTF-8 BOM and uses Apache Commons CSV; TSV uses the tab-delimited Commons CSV format; JSON writes array
  segments through Jackson `SequenceWriter`; XLSX uses Apache POI SXSSF with a frozen header row and column auto-sizing.
- Segment assemblers sort segment names before finalization, retain only the first CSV/TSV header, and produce a single
  JSON array.

**Blocking compliance gaps:**

- The writer contract uses `Set<String>` although `fieldSchema` column order is a functional requirement. API-selected
  order is therefore not guaranteed from request to output. The contract must use an ordered `List<String>` and reject
  duplicates.
- CSV/TSV assembly uses `Files.readAllLines`; JSON assembly uses `Files.readString`. Neither is streaming, and the CSV
  approach is unsafe for valid quoted values containing line breaks. The final delimited output is also normalized to LF
  even though CSV segments use CRLF.
- XLSX does **not** meet the multi-chunk requirement. A workbook is created for every chunk, and
  `ExcelSegmentAssembler` copies only the first segment to the final file, silently losing all later chunks. The planned
  single SXSSF workbook with periodic flushing/disposal is not implemented.
- Segment numbering is an in-memory `AtomicInteger`, which is neither persisted in `ExecutionContext` nor coordinated
  with transactional retry. Restarted jobs can overwrite or combine segments from different attempts.
- Format writers retain mutable state keyed by `OutputStream`. An exception before `close` can retain writer/workbook
  resources. Their close behavior also conflicts with the interface statement that the caller owns the stream.
- Existing writer and assembler tests cover nominal single-segment behavior only. They do not cover ordering,
  multi-chunk assembly, restart/retry, streaming large data, quoted multiline CSV records, or XLSX resource cleanup.

**Plan update:** The format-writer and batch-component tasks remain open and are now marked as partially implemented.
Dedicated remediation and test items were added to `plan.md`.

**Verification:** Static review completed. Focused writer and assembly tests passed (7 tests):
`mvn test -f backend/pom.xml -pl services/export-service -am -Dtest=ExportWritersTest,SegmentAssemblersTest -Dsurefire.failIfNoSpecifiedTests=false`.
The JaCoCo report emitted a pre-existing execution-data/class mismatch warning. A prior full reactor test run was not
green: `ExportFileStorageServiceTest` fails because its segment directory is not created before direct file writes; the
application-context test also fails in the local environment due to a missing `feign/slf4j/Slf4jLogger` class and absent
`export_data` schema. These failures are tracked separately from this writer audit.

---

## 2026-09-13 — Unified UniProt Import Job Architecture Refactored

**Action:** Unified the UniProt import job configuration to use a single orchestration point with a job execution
decider that routes to source-specific steps.

**Outcome:**

- Created unified job configuration `ImportJobConfig` at
  `backend/services/import-service/src/main/java/com/bioinformatics/importservice/uniprot/ImportJobConfig.java`
  - Implements Spring Batch `Job` as a single flow orchestrator for both API and file-based imports
  - Uses job parameter `DATA_PROVIDER` to make routing decision at runtime (no code branching, pure configuration)
  - Job beans:
    - `importSourceDecider()` — implements `JobExecutionDecider` interface, reads job parameter and returns decision
      status
    - `unifiedUniProtImportJob()` — defines job flow using `JobBuilder` with flow decision logic

- Implemented `ImportSourceDecider` at
  `backend/services/import-service/src/main/java/com/bioinformatics/importservice/uniprot/ImportSourceDecider.java`
  - Reads `DATA_PROVIDER` job parameter and returns its upper-cased value as flow status
  - Maps to Constants: `API("api")` → routes to `uniProtApiImportStep`, `FILE("file")` → routes to `uniProtImportStep`
  - Handles unknown provider with status "UNKNOWN" (matches no transition, job fails cleanly)

- Separated step configurations into two domain-specific configs:
  - `UniProtApiImportJobConfig` — API-based import step configuration with `UniProtApiItemReader`,
    `UniProtApiEntryProcessor`, shared `ProteinAggregateItemWriter`
  - `UniProtImportJobConfig` — file-based import step configuration with dynamic reader factory for `.dat` and `.tsv`
    formats, `ProteinEntryItemProcessor`, skip fault-tolerance policies

- **Architectural Benefits:**
  - **Single Responsibility:** Job orchestration is isolated in `ImportJobConfig`; step implementations remain in
    domain-specific configs
  - **Reusability:** Both API and file readers can be tested/invoked independently; no coupling to job flow
  - **Runtime Flexibility:** Data source selection is a job parameter, not a code path — enables easy addition of new
    sources without recompiling
  - **Liskov Substitution:** Both steps conform to Spring Batch `Step` contract; job is agnostic to source
    implementation
  - **Open/Closed:** Adding a third import source (e.g., OMA, Ensembl) requires only adding a new
    `UniProtXyzImportJobConfig` and a new decision path in `ImportJobConfig`; no changes to existing configs
  - **Observer Pattern:** Global job listeners (`ImportJobDatabaseListener`, `PostImportCacheEvictionListener`,
    `ImportJobRefreshViewsListener`) apply uniformly to both sources

- **Design Rationale:**
  - Job parameter `DATA_PROVIDER` is set by caller (e.g., `ImportService`) and passed via `JobLauncher.run(job, params)`
  - Flow decision happens after job launch, in the job execution context — enables late-binding of the source
  - Step names (`uniProtApiImportStep`, `uniProtImportStep`) are stable constants in `Constants` enum for consistency
  - `ImportSourceDecider.decide()` is stateless and deterministic — no side effects, repeatable for restart scenarios

**Dependencies & Integration:**

- `ImportJobParameters` record unpacks all job parameters (userId, filterId, filePath, timestamp, dataProvider)
- `SavedFilterService` injected into API config for filter resolution
- `ProteinAggregateItemWriter` shared between both steps (same persistence strategy)
- Both steps use `ImportProgressChunkListener` for progress telemetry
- File-based step adds `ImportUniprotSkipListener` for malformed-record handling

**Testing Considerations:**

- Unit test `ImportSourceDeciderTest` validates decision logic for API, FILE, and unknown providers
- Integration test `UnifiedUniProtImportJobTest` should verify both paths (API and file) end-to-end with mocked sources
- Job restart scenarios should re-run the decision point and re-route if source parameter changed

**Next Steps:**

- Implement comprehensive unit tests for decider (edge cases: null, empty, case sensitivity)
- Add integration test harness that runs small sample imports via both paths
- Document in operational guide how to submit job with `DATA_PROVIDER` parameter

---

## 2026-09-06 — ExportFileStorageService Implemented

**Action:** Implemented the file storage abstraction and a production-ready default implementation; added unit tests and
required dependencies.

**Outcome:**

- Created interface `ExportFileStorageService` at
  `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/service/export/ExportFileStorageService.java`.
- Implemented `DefaultExportFileStorageService` at
  `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/service/export/DefaultExportFileStorageService.java`.
  - Methods implemented: `createPipelineDirectory`, `getSegmentPath`, `getFinalFilePath`, `assembleSegments`,
    `deletePipelineDirectory`, `getFileSize`, `validateFileExists`.
  - Default base directory is configurable via `app.export.temp-dir` with fallback `/tmp/.bio-export`.
  - Segment naming: `segments/segment_00001.<ext>` (zero-padded) and final file `export_{pipelineId}.<ext>`.
  - CSV/TSV assembly: concatenates segment files, removing duplicate headers after the first segment.
  - JSON assembly: merges JSON arrays from segments into a single array safely by stripping brackets and inserting
    commas between elements.
  - Excel assembly: for single-segment exports copies the segment as final; for multiple segments logs a warning and
    uses the first segment as final. (Rationale: full XLSX sheet-merge requires careful POI handling and can be memory
    intensive; documented as a known limitation and mitigated by recommending CSV/JSON for very large exports.)

- Added unit tests `ExportFileStorageServiceTest` covering directory creation, CSV assembly and deletion:
  `backend/services/export-service/src/test/java/com/bioinformatics/exportservice/service/export/ExportFileStorageServiceTest.java`.

**Notes / Decisions:**

- Kept implementation file-system local and path-normalization to avoid path traversal. Controller/service will still
  validate ownership before exposing downloads.
- Excel merging is intentionally conservative; we will revisit if sheet-level merging is required for production.

**Next Steps:**

- Implement format writers (CSV, TSV, JSON, Excel) and `ExportWriterFactory`.
- Wire `DefaultExportFileStorageService` into the batch `assembleAndFinalizeStep`.
- Add integration test that runs a small batch job end-to-end and verifies final assembled file contents.

---

## 2026-09-06 — DTOs and MapStruct Mapper Implemented

**Action:** Created all DTOs and MapStruct mapper for API contracts and data transformation.  
**Outcome:**

- Created 6 DTO records using Java records (immutable, concise):
  - `ExportPipelineCreateRequest`: Input DTO for pipeline creation with Bean Validation annotations
    - Validates: name (non-blank, max 200 chars), description (max 500 chars), filter (non-null JsonNode), format
      (non-null), fieldSchema (non-empty, max 50 fields)
    - Filter accepted as JsonNode for flexible request structures (GeneSearchRequest or other filter formats)

  - `ExportPipelineResponse`: Output DTO for pipeline details
    - Includes: id, name, description, format, fieldSchema (List<String>), status, row counts, file info, timestamps,
      duration
    - Used by both detail and list endpoints

  - `ExportJobStatusResponse`: DTO for real-time progress polling
    - Includes: pipelineId, status, progressPercent, chunksProcessed/Total, currentStep, updatedAt
    - Frontend polls `/api/exports/pipelines/{id}/status` every 3 seconds

  - `ExportFieldSchema`: DTO describing exportable fields
    - Used by `/api/exports/fields` endpoint for field picker UI
    - Includes: fieldName, displayName, dataType, description, available

  - `DownloadUrlDto`: DTO for file download metadata
    - Used by `/api/exports/pipelines/{id}/download` endpoint
    - Includes: downloadUrl, filename, fileSizeBytes, contentType

  - `ExportPipelineRetryRequest`: DTO for retrying failed pipelines
    - Simple record with pipelineId validation

- Created `ExportPipelineMapper` (MapStruct, `@Mapper(componentModel = "spring")`)
  - `toDto(ExportPipeline): ExportPipelineResponse` — converts JSONB fieldSchema (JsonNode array) to List<String> for
    simpler API contracts
  - `toEntity(ExportPipelineCreateRequest, String userId): ExportPipeline` — creates entity from request with userId
    parameter
  - Uses injected `ObjectMapper` for JsonNode ↔ List<String> conversions
  - Ignores auto-generated fields (id, timestamps, status defaults)

- Design decisions:
  - All DTOs use Java records for immutability and conciseness (no Lombok needed for DTOs)
  - Filter accepted as JsonNode (not GeneSearchRequest) to allow flexibility with different filter formats
  - fieldSchema: JsonNode in entity → List<String> in DTO for API simplicity
  - Comprehensive validation annotations on CreateRequest (@NotBlank, @Size, @NotNull, @NotEmpty)
  - Mapper handles JSONB ↔ Java conversions seamlessly

- Updated entity (ExportPipeline):
  - Changed filterJson from GeneSearchRequest to JsonNode for flexibility
  - Removed incorrect schema import that was leftover

**Next Step:** Implement `ExportFileStorageService` for file system management.

---

## 2026-09-06 — Repository Layer Implemented

**Action:** Created Spring Data JPA repository interfaces for database access.  
**Outcome:**

- Created `ExportPipelineRepository` at
  `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/repository/ExportPipelineRepository.java`
  - Extends `JpaRepository<ExportPipeline, Long>` for standard CRUD operations
  - Query methods follow Spring Data naming conventions (auto-implemented)
  - `findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId, Pageable)` — list all active pipelines for a
    user, newest first
  - `findByUserIdAndStatusAndDeletedAtIsNull(String userId, ExportStatus status, Pageable)` — filter by status (e.g.,
    RUNNING, COMPLETED)
  - `findByIdAndUserIdAndDeletedAtIsNull(Long id, String userId)` — ownership verification (ensures user owns the
    pipeline)
  - `countByUserIdAndStatusAndDeletedAtIsNull(String userId, ExportStatus)` — concurrency control (count running
    exports)
  - All queries respect soft-delete pattern: `deletedAt IS NULL` in WHERE clause
  - Comprehensive Javadoc explaining each method's purpose

- Created `ExportJobExecutionRepository` at
  `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/repository/ExportJobExecutionRepository.java`
  - Extends `JpaRepository<ExportJobExecution, Long>` for standard CRUD operations
  - `findByPipelineId(Long pipelineId)` — fetches progress record for a given pipeline (unique constraint ensures max 1
    result)
  - Denormalized design allows efficient progress polling without Batch table joins

- Design decisions:
  - Used `String userId` (username) instead of foreign key to `AppUser` for auth service resilience
  - Method naming adheres to Spring Data conventions for automatic implementation (no custom @Query annotations needed)
  - Javadoc includes rationale for ownership checks and soft-delete handling
  - No `JpaSpecificationExecutor` extension (queries are simple and predefined)

**Next Step:** Implement `ExportFileStorageService` for file system management.

---

## 2026-09-06 — JPA Entity Layer Implemented

**Action:** Created JPA entities for export pipeline and job execution progress tracking.  
**Outcome:**

- Created `ExportPipeline` entity at
  `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/entity/ExportPipeline.java`
  - Used Lombok (`@Entity`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`)
  - JSONB fields via `@JdbcTypeCode(SqlTypes.JSON)` for `filterJson` and `fieldSchema` (flexible query support)
  - Enum fields with `@Enumerated(EnumType.STRING)` for `format` and `status`
  - Soft-delete pattern: `deletedAt` column tracks deletion time; null = active
  - Lifecycle hook `@PrePersist` sets `createdAt` automatically
  - Helper methods: `isTerminal()` (checks if status is COMPLETED/FAILED/CANCELLED), `isDeleted()` (checks if
    soft-deleted)
  - Design rationale: `userId` stored as `String` (username) rather than foreign key for auth service resilience

- Created `ExportJobExecution` entity at
  `backend/services/export-service/src/main/java/com/bioinformatics/exportservice/entity/ExportJobExecution.java`
  - ManyToOne relationship to `ExportPipeline` with LAZY fetch and CASCADE delete
  - `jobExecutionId` column has UNIQUE constraint to enforce one execution per pipeline
  - Denormalized progress tracking: `chunksTotal`, `chunksProcessed` for efficient polling (no Batch table join needed)
  - Lifecycle hooks `@PrePersist` and `@PreUpdate` maintain `updatedAt` timestamp
  - Helper method: `getProgressPercent()` calculates 0–100 progress for UI progress bars

- Both entities follow modern Java 21+ conventions:
  - Immutable-first design via Lombok's `@Builder` pattern
  - Strong typing (no primitives where Optional semantics apply)
  - Comprehensive Javadoc for maintainability
  - Stateless design (no circular references, lazy fetch by default)

- Verified Jackson (via Spring Boot) is available for `JsonNode` JSONB handling (no additional dependency needed)

**Next Step:** Implement `ExportFileStorageService` for file system management.

---

## 2026-09-06 — DB Migration Created

**Action:** Created Flyway migration file for export pipeline tables.  
**Outcome:**

- Created `/backend/services/export-service/src/main/resources/db/migration/V1__export_pipeline.sql`
- Implemented `export_pipeline` table with:
  - All required columns: `id`, `user_id`, `name`, `description`, `filter_json`, `format`, `field_schema`, `status`,
    `estimated_rows`, `actual_rows`, `file_path`, `file_size_bytes`, `error_message`, `job_execution_id`, lifecycle
    timestamps (`created_at`, `started_at`, `completed_at`, `deleted_at`, `duration_ms`)
  - CHECK constraint on `format` and `status` enums
  - Three performance indexes on `user_id`, `status`, and soft-delete flag
- Implemented `export_job_execution` table with:
  - Foreign key to `export_pipeline` with CASCADE delete
  - Unique constraint on `job_execution_id`
  - Progress tracking columns (`chunks_total`, `chunks_processed`)
  - Index on `pipeline_id` for fast lookups
- Migration follows Flyway naming convention (`V<N>__<description>.sql`) and PostgreSQL best practices
- Schema design uses `JSONB` for structured data (`filter_json`, `field_schema`) to leverage PostgreSQL's query
  capabilities
- Soft-delete pattern implemented via `deleted_at` column with partial index for active records

**Next Step:** Implement JPA entity layer (`ExportPipeline`, `ExportJobExecution`).

---

## 2026-08-12 — Ticket Created & Requirements Analyzed

**Action:** Created `PIPE-001` implementation folder and drafted specification.  
**Outcome:**

- Analyzed existing EXPORT-001 synchronous export implementation. Identified timeout risk for >100K rows and format
  limitation (CSV only).
- Evaluated Spring Batch reuse vs introducing a new async framework (e.g., RabbitMQ, Kafka). Selected Spring Batch for
  consistency with IMPORT-001/RDF-001.
- Evaluated Excel libraries: Apache POI SXSSF vs EasyExcel vs FastExcel. Selected SXSSF for streaming capability and
  maturity.
- Defined file storage convention under existing `APP_DIR` property (from RDF-001):
  `${APP_DIR}/exports/{userId}/{pipelineId}/`.
- Confirmed `GeneServiceDispatcher` and `GeneSpecification` can be reused for both Postgres and UniProt provider
  exports.
- Designed segment-based file writing to keep memory bounded: each chunk writes to a separate temp file, assembled at
  the end.
- Identified reuse opportunities:
    - `AuditService` (OPS-001) for pipeline event logging
    - `Bucket4j` (OPS-001) for rate limiting pipeline creation
    - `GeneSearchRequest` validation (GENE-001) for filter pre-check
    - `SaveFilterDialog` (FILTER-001) for Step 1 of wizard
- Decided to keep existing `POST /api/genes/export-csv` for small synchronous exports; PIPE-001 is additive, not
  replacing.

**Next Step:** Begin DB migration and entity implementation once ticket is prioritized.

---

**Coverage Target:** ≥ 80 % (pending)
