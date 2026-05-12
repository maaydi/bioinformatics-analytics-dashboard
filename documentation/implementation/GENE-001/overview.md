# GENE-001 — Gene/Protein Backend Endpoints

## Description

Implement the full backend for the Gene/Protein REST API:

- `GET /api/genes` — paginated, sortable list of protein summaries.
- `POST /api/genes/search` — multi-field search and filter with AND logic.
- `GET /api/genes/{id}` — full protein detail including relations.
- `POST /api/genes/export-csv` — streaming CSV export matching the active filter.

## Scope

| Layer         | Artifact                                                                                                                                                |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Entity        | `ProteinEntry` + all related entities (`Keyword`, `GoTerm`, `CrossReference`, `ProteinFeature`, `HostOrganism`, `ProteinComment`, `ProteinPublication`) |
| DTOs          | `ProteinSummaryDto`, `ProteinDetailDto`, `GeneSearchRequest`, `PagedResponse<T>`, nested item DTOs                                                      |
| Repository    | `ProteinEntryRepository` with JPA Specification support                                                                                                 |
| Specification | `GeneSpecification` — composable predicates for all filter fields                                                                                       |
| Service       | `GeneService` — orchestrates search, detail fetch, and CSV export                                                                                       |
| Mapper        | `ProteinMapper` (MapStruct) — entity → summary and entity → detail                                                                                      |
| Controller    | `GeneController` — thin, delegates to service                                                                                                           |
| CSV Export    | `CsvExportService` — streams via `StreamingResponseBody`                                                                                                |
| DB Indexes    | Full-text index `ix_protein_search` (GIN tsvector), trigram index `ix_protein_organism_trgm`, standard B-tree indexes on sort columns                   |

## Acceptance Criteria

- [ ] `GET /api/genes` returns `200` with `PagedResponse<ProteinSummaryDto>` (default page=0, size=50).
- [ ] `GET /api/genes` with `sort=accession&direction=desc` returns correctly sorted results.
- [ ] `GET /api/genes` with `sort=<invalid>` returns `400`.
- [ ] `POST /api/genes/search` with all-null filters returns the full paginated set.
- [ ] `POST /api/genes/search` combining organism + reviewed + keywords returns AND-filtered results.
- [ ] `POST /api/genes/search` with `lengthMin > lengthMax` returns `400`.
- [ ] `POST /api/genes/search` with invalid `goTermId` (not matching `GO:\d{7}`) returns `400`.
- [ ] `GET /api/genes/{id}` returns `200` with `ProteinDetailDto` including all nested relations.
- [ ] `GET /api/genes/{id}` for non-existent id returns `404`.
- [ ] `POST /api/genes/export-csv` returns `200` with `Content-Type: text/csv` and correct column headers.
- [ ] All endpoints return `401` when called without a valid JWT.
- [ ] N+1 queries eliminated (fetch joins / projections used for relations).
- [ ] Unit tests for `GeneService` covering happy path, empty results, and validation errors.
- [ ] Integration tests for all four endpoints (happy + error paths).

## References

- `documentation/api-contract.md` §1 — Gene / Protein Endpoints
- `documentation/domain-model.md` — `protein_entry` and related tables
- `documentation/validation-rules.md` — filter validation rules
- `documentation/plan.md` — US-4, US-5, US-6, US-10, US-31
