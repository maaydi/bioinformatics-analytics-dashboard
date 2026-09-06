# PIPE-001 — Implementation Journal

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

**Next Step:** Implement repository layer with query methods for common access patterns.

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
