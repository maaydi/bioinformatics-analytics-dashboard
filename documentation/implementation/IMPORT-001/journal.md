# IMPORT-001 Implementation Journal

## 2026-05-01
- Created implementation folder and initial documentation files for IMPORT-001
- To be updated as implementation progresses

## 2026-05-07

- Backend: implemented import admin API surface and service layer
    - `com.bioinformatics.dashboard.admin.controller.ImportController` — endpoints:
        - POST `/api/admin/import/uniprot` (triggers import)
        - GET `/api/admin/import/status` (paginated job list)
        - GET `/api/admin/import/status/{jobId}` (job progress)
    - `com.bioinformatics.dashboard.admin.service.ImportService` — file save, strategy handling, concurrent-run check,
      job persistence and job execution trigger
    - `com.bioinformatics.dashboard.job.mapper.ImportJobMapper` (MapStruct) — maps entity → DTO and calculates
      progress/elapsed time
    - `com.bioinformatics.dashboard.job.repository.ImportJobRepository` — repository + helper query `findByStatus`
    - Exceptions added: `ImportAlreadyRunningException`, `ExecuteJobException`, `MalformedUniprotFileException`
    - Backend files of note:
        - `backend/src/main/java/com/bioinformatics/dashboard/admin/service/ImportService.java`
        - `backend/src/main/java/com/bioinformatics/dashboard/admin/controller/ImportController.java`
        - `backend/src/main/java/com/bioinformatics/dashboard/job/mapper/ImportJobMapper.java`
        - `backend/src/main/java/com/bioinformatics/dashboard/job/repository/ImportJobRepository.java`

## 2026-05-08

- Frontend: implemented Import Admin UI and API client
    - `frontend/src/app/features/import-admin/import-admin.component.ts` — full component implementing file selection,
      client-side validation (extensions + 2GB), strategy selector, submit flow, polling of job progress (5s) and job
      history refresh (2s), pagination support
    - `frontend/src/app/features/import-admin/import-admin.service.ts` — API client for triggerImport, listImportJobs,
      getJobProgress
    - `frontend/src/app/core/models/import.model.ts` — TypeScript DTOs matching backend contract
    - Notes: component uses signals for state, material components for layout, and maps HTTP error codes (409/413/422)
      to user-friendly messages

- Status summary
    - Feature endpoints and UI are implemented and wired end-to-end on the current branch.
    - Client-side validation and polling behaviour are implemented.
    - Outstanding work:
        - Unit tests for `ImportService` and related backend business logic (required by project rules)
        - Angular unit tests for `ImportAdminService` and `ImportAdminComponent`
        - Integration tests that validate the API contract and an end-to-end test for file uploads (large-file
          behaviour)
        - Review and extend global exception-to-HTTP mappings if needed (ensure 422/413 are returned by server where
          appropriate)

### 2026-05-08 — Unit tests added

- Added backend unit tests (service layer):
    - `backend/src/test/java/com/bioinformatics/dashboard/admin/service/ImportServiceTest.java`
    - Tests cover: listing jobs mapping, triggerImport when another import is running (exception), successful
      triggerImport (saves job and triggers executor), getImportJobStatus missing job.

- Added frontend unit tests:
    - `frontend/src/app/features/import-admin/import-admin.service.spec.ts`
    - Tests cover: POST triggerImport (FormData), GET listImportJobs with pagination params, GET job progress by id.

Test execution results (local):

- Frontend tests executed successfully (vitest via `ng test`): 5 tests passed.
- Backend tests could not be executed: Maven reported a Java release mismatch ("release version 25 not supported").
    - Action: backend tests are present and ready; run them locally after installing a compatible JDK or adjust
      `maven-compiler-plugin` settings.

## Notes / assumptions

- Assumed import storage location comes from `AppProperties.importConfig.tempDir` (configured in `application.yml`).
- The import engine is executed asynchronously by `AsyncUniprotImportJobExecutor` and monitored via the `ImportJob`
  entity state.

