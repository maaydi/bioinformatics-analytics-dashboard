# IMPORT-001 Implementation Plan

## Tasks
1. Analyze requirements and update plan
2. Implement backend changes (controller, service, repository, DTO, entity, mapper)
3. Implement frontend changes (Angular service, models, UI components)
4. Write and run unit tests
5. Update documentation
6. Review and refactor as needed

## Status
- [x] Requirements analyzed
- [ ] Backend implemented (controller stubs only, not implemented)
- [ ] Frontend implemented (UI/service exists, not verified complete)
- [ ] Unit tests written
- [x] Documentation updated (initial docs only)
- [ ] Code reviewed

---

## Detailed Checklist

### Backend
- [ ] Controller endpoints implemented:
	- [ ] POST /api/admin/import/uniprot (trigger import job)
	- [ ] GET /api/admin/import/status (list jobs, paginated)
	- [ ] GET /api/admin/import/status/{jobId} (job status)
- [ ] Service layer implemented (ImportService)
- [ ] Repository layer for job persistence
- [ ] DTOs for request/response (ImportJobSummary, ImportJobStatus, etc.)
- [ ] Entity/model for import jobs
- [ ] MapStruct mappers for DTO/entity
- [ ] Validation annotations and error handling
- [ ] Integration with Spring Batch for import logic
- [ ] Security: endpoints restricted to ROLE_ADMIN
- [ ] Pagination and filtering for job list
- [ ] Unit tests for service logic
- [ ] Integration tests for endpoints

### Frontend (Angular)
- [ ] Service for API calls (import-admin.service.ts)
- [ ] Models for API contracts (ImportJobSummary, ImportJobStatus, etc.)
- [ ] UI for:
	- [x] Uploading UniProt file and selecting strategy
	- [x] Viewing job list (paginated)
	- [x] Viewing job status/progress
- [x] Error, loading, and empty states handled
- [ ] Accessibility (WCAG AA, ARIA, focus management)
- [ ] Unit tests for service and components

### Documentation
- [ ] API contract updated if changed
- [ ] Domain model updated if changed
- [ ] Validation rules documented
- [ ] Implementation journal updated

### General
- [ ] Code reviewed
- [ ] Coverage ≥ 80% (backend and frontend)
