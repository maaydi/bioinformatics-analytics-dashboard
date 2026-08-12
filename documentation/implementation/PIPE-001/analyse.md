# PIPE-001 — Ambiguities & Analysis

## Status: Resolved — implementation can proceed

---

## Resolved Decisions

### 1. Synchronous vs Asynchronous Export Architecture

**Decision:** Asynchronous Spring Batch jobs with polling.

**Rationale:**

| Approach                                       | Pros                                                                                          | Cons                                                                             |
|------------------------------------------------|-----------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| **Synchronous streaming (current EXPORT-001)** | Simple; immediate download                                                                    | HTTP thread blocked; timeout risk >30s; memory pressure for large sets; no retry |
| **Async with messaging (RabbitMQ/Kafka)**      | True decoupling; horizontal scaling                                                           | New infrastructure; operational complexity; overkill for current scale           |
| **Async Spring Batch (chosen)**                | Reuses existing batch infra; built-in retry/skip; chunk processing; job repository for status | Requires polling from frontend; slightly more complex than sync                  |

- The existing Spring Batch `JobRepository` (from IMPORT-001) already tracks job executions, making status polling
  trivial.
- Chunk-oriented processing (500 rows/chunk) keeps heap usage bounded regardless of result set size.
- The HTTP response returns in < 500 ms with a pipeline ID; the user polls for completion.

---

### 2. File Assembly: Concatenation vs Single Writer

**Decision:** Segment files per chunk, concatenated in a final assembly step.

**Rationale:**

| Approach                              | Pros                                                                                                   | Cons                                                                                              |
|---------------------------------------|--------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| **Single writer across all chunks**   | No assembly step; simpler                                                                              | Writer must be thread-safe; harder to resume on failure; Excel cannot append to existing workbook |
| **Segment files + assembly (chosen)** | Each chunk is independent; easy to retry failed chunks; Excel segments can be merged via sheet copying | Requires assembly step; more disk I/O                                                             |

- For CSV/TSV/JSON: assembly is a simple stream concatenation.
- For Excel: Apache POI SXSSF cannot append to an existing workbook. The assembly step opens each segment workbook and
  copies rows into a master workbook. This is memory-intensive but acceptable for the typical export size (< 500K rows).
- If a chunk fails, only that segment needs reprocessing (future enhancement: chunk-level retry).

---

### 3. Excel Library: Apache POI SXSSF vs EasyExcel

**Decision:** Apache POI SXSSF.

**Rationale:**

| Library                 | Maturity  | Streaming             | Memory              | Spring Integration                                               |
|-------------------------|-----------|-----------------------|---------------------|------------------------------------------------------------------|
| **Apache POI SXSSF**    | Very high | Yes (window size 100) | ~10MB per 100K rows | Native Java, no issues                                           |
| **EasyExcel (Alibaba)** | High      | Yes                   | Very low            | Java, but Chinese docs; less community in Western bioinformatics |
| **FastExcel**           | Moderate  | Yes                   | Low                 | Newer library, less battle-tested                                |

- SXSSF is the de-facto standard for large Excel generation in Java enterprise apps.
- The "window size" (default 100 rows kept in memory) ensures OOM safety.
- Auto-sizing columns requires a two-pass approach (write data, then auto-size), which is handled in the assembly step.

---

### 4. Custom Field Selection: Flat vs Nested

**Decision:** Flat field list with nested collection flattening.

**Rationale:**

- Users select from a flat list of field names (e.g., `keywords`, `goTerms`, `features`).
- For CSV/TSV: nested collections are serialized as comma-separated strings within the cell (e.g., `keywords` →
  `"Kinase,Activator,Transcription"`).
- For JSON: nested collections remain as JSON arrays (e.g., `"keywords": ["Kinase", "Activator"]`).
- For Excel: same as CSV (comma-separated within cell).
- This avoids requiring users to understand nested schemas while preserving data in all formats.
- Future v1.1: allow "Expand nested" option that creates additional columns (e.g., `goTerm_1`, `goTerm_2`).

---

### 5. Provider Compatibility: Postgres vs UniProt REST Export

**Decision:** Single `ExportItemReader` interface with two implementations.

**Rationale:**

- Postgres export uses `JpaPagingItemReader<ProteinEntry>` with `GeneSpecification`.
- UniProt REST export requires cursor-based pagination (same pattern as `UniProtApiItemReader` in RDF-001).
- The `ExportJobConfig` selects the reader implementation based on a job parameter `dataProvider` (default: current
  provider from `ProviderContextHolder`).
- Both readers produce `Map<String, Object>` rows via the same `ExportItemProcessor`, so the writer is
  provider-agnostic.
- This maintains the pluggable provider architecture without duplicating batch logic.

---

### 6. Retention & Cleanup: Hard vs Soft Delete

**Decision:** Soft delete for 30 days, then hard delete via scheduled job.

**Rationale:**

- Soft delete allows users to "undo" accidental deletions within a window.
- Prevents immediate data loss if a user deletes a pipeline they still need.
- The scheduled cleanup job (Sunday 2 AM) mirrors the maintenance window defined in overview.md §12.5.
- Disk space is monitored; if > 80 % full, an alert is logged and cleanup can be triggered manually.

---

### 7. Download URL: Direct vs Presigned

**Decision:** Direct download via controller endpoint with ownership check.

**Rationale:**

| Approach                     | Pros                                            | Cons                                                                     |
|------------------------------|-------------------------------------------------|--------------------------------------------------------------------------|
| **Direct controller stream** | Full auth control; no URL expiration complexity | HTTP thread occupied during download (acceptable for file sizes < 100MB) |
| **Presigned URL (S3/minio)** | Offloads bandwidth; scalable                    | Requires object storage infrastructure; not in current stack             |
| **Static file serving**      | Fast; CDN-friendly                              | No auth control; path traversal risk                                     |

- For the current scale (local file system, single-node or small cluster), direct controller streaming is sufficient.
- The controller verifies JWT + pipeline ownership before streaming bytes via `InputStreamResource`.
- If the deployment scales to multiple nodes, a shared volume or object storage migration is a future ops task, not a
  code change.

---

## Open Questions (non-blocking)

| Question                                                                 | Owner   | Priority | Resolution Path                                                              |
|--------------------------------------------------------------------------|---------|----------|------------------------------------------------------------------------------|
| Should we support compressed exports (.zip, .gz) for large files?        | Product | Low      | Defer to v1.1; add `compressed: boolean` to `ExportPipelineCreateRequest`    |
| Should completed pipelines be emailable with the file attached?          | Product | Low      | Defer; requires email service integration; file size may exceed email limits |
| Should we support scheduled/recurring exports (e.g., weekly)?            | Product | Low      | Defer to v1.1; requires Quartz integration and cron expression UI            |
| Should the wizard show a preview of the first 10 rows before confirming? | UX      | Low      | Defer; adds complexity; current filter chips provide sufficient context      |

---

**Last Updated:** 2026-08-12
