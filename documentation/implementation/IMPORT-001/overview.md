# IMPORT-001: Import Admin Feature Implementation

## Description
This document describes the implementation of the Import Admin feature as per ticket IMPORT-001.

## Acceptance Criteria

- POST /api/admin/import/uniprot — triggers an import job and returns 202 Accepted with the created job summary (fields:
  id, status, createdAt) — STATUS: Done (controller + service implemented)
- GET /api/admin/import/status?page=&size= — paginated list of import jobs returning the pagination envelope and
  ImportJobSummary items — STATUS: Done (backend pagination + frontend list)
- GET /api/admin/import/status/{jobId} — realtime job progress schema (ImportJobProgress) — STATUS: Done (backend +
  frontend polling)
- File upload handling: saves uploaded file to server temp dir, supports "OVERWRITE" and non-overwrite strategies,
  enforces accepted extensions and 2 GB size limit (server + client enforcement) — STATUS: Done (server save logic +
  client validation)
- Prevent concurrent imports: starting a new import while another is RUNNING must return 409 Conflict — STATUS: Done (
  repository check + exception)
- Frontend Import Admin UI: file picker, strategy selector, submit flow, progress bar, and job history table with
  pagination — STATUS: Done (component + service implemented)
- Security: all import admin endpoints restricted to ROLE_ADMIN — STATUS: Done (@PreAuthorize present on controller)
- Logging, error handling and meaningful error messages returned for 409 / 413 / 422 / 500 — STATUS: Partially done (
  exceptions and client mapping exist; review global exception handler mappings)
- Unit tests for backend service logic and Angular unit tests for component/service — STATUS: To do
- Integration tests for import endpoints (API contract validation) and an end-to-end test for large file upload
  behaviour — STATUS: To do

## References
- See `documentation/overview.md` for user stories
- See `documentation/api-contract.md` for REST API contract
- See `documentation/domain-model.md` for database schema
- See `documentation/validation-rules.md` for validation rules
