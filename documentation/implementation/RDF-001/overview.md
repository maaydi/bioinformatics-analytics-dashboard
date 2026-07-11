# RDF-001 — Import Job Tracking via Spring Batch JobRepository

## Description

Replace the custom `import_job` table and `ImportJobRepository` with Spring Batch's native
`JobRepository` as the single source of truth for import job tracking.

The remote UniProt API import is already functional (`ImportService.triggerRemoteImport()`).
This ticket cleans up the redundant persistence layer that duplicated what Spring Batch already
tracks, and wires `ImportService` to read job state directly from `JobRepository` instead.

**Zero frontend impact** — `ImportJobSummary` and `ImportJobProgress` DTOs are kept as-is;
only the data source that populates them changes.

## Motivation

| Problem                                                                                                | Impact                                        |
|--------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `import_job` table duplicates Spring Batch meta-tables (`BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`) | Dual write — inconsistency risk               |
| Listeners manually update `ImportJob` entity every chunk                                               | Extra DB round-trips inside Batch transaction |
| `ImportJobRepository` is a parallel job-tracking mechanism                                             | Dead code once Batch meta-tables are used     |
| Flyway migration required to keep schema in sync with entity                                           | Maintenance overhead                          |

## Scope

| Layer            | Artifact / Responsibility                                                                                                                            |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DB migration** | `VN__drop_import_job_table.sql` — drop `import_job` and related indexes                                                                              |
| **Deleted**      | `ImportJob.java` entity, `ImportJobRepository.java`, current `ImportJobMapper.java`                                                                  |
| **New mapper**   | `BatchJobExecutionMapper.java` — `JobExecution` → `ImportJobSummary` / `ImportJobProgress`                                                           |
| **New service**  | `BatchImportQueryService.java` — wraps `JobRepository` to list, page, and get job status                                                             |
| **Updated**      | `ImportService.java` — replace `ImportJobRepository` calls with `BatchImportQueryService`                                                            |
| **Updated**      | Batch listeners — remove any listener that wrote to `import_job`; keep only listeners with valid side-effects (materialized view refresh, analytics) |
| **Constants**    | `Constants.java` — review job parameter keys; `IMPORT_JOB_ID` key may be removed or repurposed                                                       |
| **Tests**        | Unit tests for `BatchJobExecutionMapper`, `BatchImportQueryService`; integration test for `ImportService`                                            |

## Acceptance Criteria

- [ ] `import_job` table removed via a Flyway migration (Spring Batch tables hold all state).
- [ ] `ImportJob.java` entity deleted; no compilation errors remain.
- [ ] `ImportJobRepository.java` deleted; `ImportService` no longer depends on it.
- [ ] `BatchJobExecutionMapper` correctly maps `JobExecution` fields to `ImportJobSummary` and `ImportJobProgress`:
  - `id` ← `JobExecution.id` (as String)
  - `status` ← mapped from `BatchStatus` → `ImportStatus`
  - `fileName` ← `JobParameters["filePath"]` or `"UNIPROT_API_REMOTE"` when absent
  - `progressPercent` ← derived from `StepExecution.writeCount` / estimated total if available
  - `entryCount` ← `StepExecution.writeCount` on the main step
  - `durationMs` ← `endTime - startTime` when completed
  - `createdAt` / `completedAt` ← `JobExecution.createTime` / `JobExecution.endTime`
  - `errorMessage` ← `JobExecution.exitStatus.exitDescription` when FAILED
- [ ] `BatchImportQueryService` exposes:
  - `PagedResponse<ImportJobSummary> listImportJobs(int page, int size)` — reads all UniProt job instances from
    `JobRepository`, sorted by start time descending
  - `ImportJobProgress getImportJobStatus(long jobExecutionId)` — returns live progress for a single execution
  - `boolean isAnyRunning()` — replaces `importJobRep.findByStatus(RUNNING)` concurrency guard
- [ ] `ImportService` uses `BatchImportQueryService` for all read operations and concurrency check.
- [ ] `ImportService.triggerImport` and `triggerRemoteImport` return `ImportJobSummary` populated from the
  `JobExecution` created by Spring Batch (not from a separately-saved entity).
- [ ] `ImportService.getImportJobStatus(String jobId)` accepts the Spring Batch `jobExecutionId` (long, as String) — *
  *API contract field name `id` unchanged**.
- [ ] All batch listeners that previously wrote to `import_job` are removed or replaced.
- [ ] Only listeners with valid side-effects survive: e.g., `MaterializedViewRefreshListener` (refreshes analytics views
  on job success).
- [ ] Frontend polling (`GET /api/admin/import/status/{jobId}`) continues to work with no contract change.
- [ ] Unit tests ≥ 80 % coverage for `BatchJobExecutionMapper` and `BatchImportQueryService`.
- [ ] Integration test: trigger file import → poll status → assert `ImportJobProgress` fields are populated correctly.

## Key Design Decisions

### ID strategy

Spring Batch uses `long` for `jobExecutionId`. The existing `ImportJobSummary.id` is a `String`.
Keep `String` in the DTO; cast at mapper boundary: `String.valueOf(execution.getId())`.

> **Breaking change note**: the `id` format changes from UUID string to a numeric string.
> Verify the frontend treats the ID as an opaque string (not a UUID).

### Pagination

`JobRepository` does not natively expose a paginated list.
`BatchImportQueryService` uses `JobRepository.findJobInstances(jobName, start, count)` +
`JobRepository.getJobExecutions(instance)` to build a page sorted by `JobExecution.startTime` DESC.

### Concurrency guard

`isAnyRunning()` calls `JobRepository.findRunningJobExecutions(JOB_NAME)` — equivalent to the
former `importJobRep.findByStatus(RUNNING)`.

### Remote import ID

`triggerRemoteImport()` previously saved a custom `ImportJob` entity to obtain an ID before the
Batch job ran. After this change, the ID is `JobExecution.id` returned by the executor after
`jobLauncher.run()`.

## References

- `documentation/api-contract.md` — REST contract must not change
- `documentation/domain-model.md` — `import_job` table DDL to be removed
- Spring Batch 6 `JobRepository` API — `findRunningJobExecutions`, `findJobInstances`, `getJobExecutions`
- `backend/.../job/dto/ImportJobSummary.java` — DTO contract to preserve
- `backend/.../job/dto/ImportJobProgress.java` — DTO contract to preserve
