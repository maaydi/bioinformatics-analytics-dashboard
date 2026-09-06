# PIPE-001 — Implementation Journal

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
