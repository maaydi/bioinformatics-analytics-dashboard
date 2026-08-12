# PIPE-001 — Implementation Journal

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
