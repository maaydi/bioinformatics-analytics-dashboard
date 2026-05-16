# GENE-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Implement DB migration (indexes: GIN tsvector, trigram, B-tree on sort columns)
3. Implement entity layer (all related entities with proper JPA mappings)
4. Implement DTOs and MapStruct mappers
5. Implement `GeneSpecification` (composable predicates for each filter field)
6. Implement `GeneService` (list, search, detail, export)
7. Implement `CsvExportService` with `StreamingResponseBody`
8. Complete `GeneController` (remove stubs, wire service)
9. Write unit tests for `GeneService`
10. Write integration tests for all endpoints
11. Update documentation

## Status

- [x] Requirements analyzed
- [x] DB migration created
- [x] DB migration created
- [x] Entities implemented
- [x] DTOs and mappers implemented
- [x] DTOs and mappers implemented
- [x] GeneSpecification implemented
- [x] GeneService implemented
- [x] CsvExportService implemented
- [x] GeneController completed
- [x] GeneService implemented
- [x] CsvExportService implemented
- [x] GeneController completed

- [x] Unit tests written
- [x] Integration tests written
- [x] Documentation updated
- [x] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Database Migration

- [x] `V3__gene_indexes.sql` — GIN index on `search_tsv` generated column
- [x] Trigram extension + `ix_protein_organism_trgm` index
- [x] B-tree indexes on: `accession`, `gene_name_primary`, `organism_name`, `length`, `molecular_weight`, `reviewed`,
  `evidence_level`, `updated_date`
- [x] `V5__gene_indexes.sql` — Trigram extension + trigram indexes for organism_name & gene_name_primary
- [x] B-tree indexes on: `gene_name_primary`, `updated_date`
- [x] B-tree indexes on: `accession`, `organism_name`, `length`, `molecular_weight`, `reviewed`, `evidence_level` (
  created in V1)

### Backend — Entity Layer

- [x] `ProteinEntry` entity with all fields from domain model
- [x] `Keyword` entity + `ProteinKeyword` join entity
- [x] `GoTerm` entity + `ProteinGoTerm` join entity
- [x] `CrossReference` entity
- [x] `ProteinFeature` entity
- [x] `HostOrganism` entity
- [x] `ProteinComment` entity
- [x] `ProteinPublication` entity
- [x] All `@ManyToOne` / `@OneToMany` mappings with `FetchType.LAZY`

### Backend — DTO Layer

- [x] `ProteinSummaryDto` — flat projection for list/search results
- [x] `ProteinDetailDto` — full detail including nested items
- [x] `FeatureItemDto`, `GoTermItemDto`, `CrossReferenceItemDto`, `CommentItemDto`, `PublicationItemDto`,
  `HostOrganismItemDto`
- [x] `GeneSearchRequest` — all optional filter fields with `@Valid` constraints
  - [x] Cross-field `@AssertTrue isLengthRangeValid()`
  - [x] Cross-field `@AssertTrue isMolecularWeightRangeValid()`
  - [x] `@Pattern` for `goTermId` matching `GO:\d{7}`
  - [x] `@Min`/`@Max` on `evidenceLevels` items (1–5)
- [x] `PagedResponse<T>` generic envelope

### Backend — Repository

- [x] `ProteinEntryRepository extends JpaRepository, JpaSpecificationExecutor`
- [x] Custom JPQL fetch-join queries for detail endpoint (avoid N+1)
- [x] Sort whitelist validation in service layer

### Backend — Specification

- [x] `GeneSpecification.globalSearch(String)` — tsvector / ILIKE fallback
- [x] `GeneSpecification.accessionLike(String)`
- [x] `GeneSpecification.entryNameLike(String)`
- [x] `GeneSpecification.geneNameLike(String)`
- [x] `GeneSpecification.proteinNameLike(String)`
- [x] `GeneSpecification.reviewed(Boolean)`
- [x] `GeneSpecification.organismLike(String)`
- [x] `GeneSpecification.taxid(Integer)`
- [x] `GeneSpecification.lineageLike(String)`
- [x] `GeneSpecification.lengthBetween(Integer min, Integer max)`
- [x] `GeneSpecification.molecularWeightBetween(Integer min, Integer max)`
- [x] `GeneSpecification.evidenceLevelsIn(List<Integer>)`
- [x] `GeneSpecification.keywordsContainAll(List<String>)`
- [x] `GeneSpecification.goTermId(String)`
- [x] `GeneSpecification.goAspect(String)`
- [x] `GeneSpecification.featureType(String)`
- [x] `GeneSpecification.crossRefSource(String)`

### Backend — Service

- [x] `GeneService.listGenes(page, size, sort, direction)` — validates sort field against whitelist
- [x] `GeneService.searchGenes(GeneSearchRequest)` — composes specification from non-null fields
- [x] `GeneService.getGeneById(Long id)` — fetch join all relations; throw `GeneNotFoundException` if missing
- [x] `GeneService.exportCsv(GeneSearchRequest, HttpServletResponse)` — streams via `StreamingResponseBody`; hard cap
  100,000 rows

### Backend — Controller

- [x] Remove `UnsupportedOperationException` stubs
- [x] Wire `GeneService`; keep controller thin
- [x] Return correct HTTP status codes (`200`, `400`, `401`, `404`)

### Backend — Tests

- [x] `GeneServiceTest` — unit tests (mock repository):
  - [x] `listGenes_returnsPage`
  - [x] `listGenes_invalidSort_throws`
  - [x] `searchGenes_withOrganism`
  - [x] `searchGenes_lengthRangeInvalid_throws`
  - [x] `getGeneById_found`
  - [x] `getGeneById_notFound_throws`
- [x] `GeneControllerIntegrationTest` — Testcontainers PostgreSQL:
  - [x] `GET /api/genes` — 200 with paged body
  - [x] `GET /api/genes?sort=invalid` — 400
  - [x] `POST /api/genes/search` — 200 with filtered result
  - [x] `POST /api/genes/search` with invalid filter — 400
  - [x] `GET /api/genes/{id}` — 200 full detail
  - [x] `GET /api/genes/9999` — 404
  - [x] `POST /api/genes/export-csv` — 200 CSV content-type

### General

- [x] Code reviewed
- [ ] Coverage ≥ 80% (JaCoCo report recorded in journal)

