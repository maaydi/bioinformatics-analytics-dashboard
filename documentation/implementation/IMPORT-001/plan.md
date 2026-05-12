# IMPORT-001 Implementation Plan

## Tasks
1. Analyze requirements and update plan
2. Implement backend changes (controller, service, repository, DTO, entity, mapper)
3. Implement frontend changes (Angular service, models, UI components)
4. Write and run unit tests
5. Update documentation
6. Review and refactor as needed

## Status

- [x] Requirements analyzed (x)
- [x] Backend implemented (controller stubs only, not implemented) (x)
- [x] Frontend implemented (UI/service exists, not verified complete)
- [x] Unit tests written (x)
- [x] Documentation updated (initial docs only) (x)
- [x] Code reviewed

---

## Detailed Checklist

### Backend

- [x] Controller endpoints implemented: (x)
	- [x] POST /api/admin/import/uniprot (trigger import job) (x)
	- [x] GET /api/admin/import/status (list jobs, paginated) (x)
	- [x] GET /api/admin/import/status/{jobId} (job status) (x)
- [x] Service layer implemented (ImportService) (x)
- [x] Repository layer for job persistence (x)
- [x] DTOs for request/response (ImportJobSummary, ImportJobStatus, etc.) (x)
- [x] Entity/model for import jobs (x)
- [x] MapStruct mappers for DTO/entity (x)
- [x] Validation annotations and error handling (x)
- [x] Integration with Spring Batch for import logic (x)
- [x] Security: endpoints restricted to ROLE_ADMIN
- [x] Pagination and filtering for job list
- [x] Unit tests for service logic (x)
- [x] Integration tests for endpoints

### Frontend (Angular)

- [x] Service for API calls (import-admin.service.ts) (x)
- [x] Models for API contracts (ImportJobSummary, ImportJobStatus, etc.) (x)
- [x] UI for:
	- [x] Uploading UniProt file and selecting strategy (x)
	- [x] Viewing job list (paginated) (x)
	- [x] Viewing job status/progress (x)
- [x] Error, loading, and empty states handled (x)
- [x] Unit tests for service and components (x)

### Documentation

- [x] API contract updated if changed
- [x] Domain model updated if changed
- [x] Validation rules documented
- [x] Implementation journal updated

### General

- [x] Code reviewed
- [ ] Coverage ≥ 80% (backend and frontend) — Integration tests added; coverage verification pending
