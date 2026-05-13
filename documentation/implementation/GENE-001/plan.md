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
- [ ] DB migration created
- [x] Entities implemented
- [ ] DTOs and mappers implemented
- [x] GeneSpecification implemented
- [ ] GeneService implemented
- [ ] CsvExportService implemented
- [ ] GeneController completed
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Database Migration

- [ ] `V3__gene_indexes.sql` — GIN index on `search_tsv` generated column
- [ ] Trigram extension + `ix_protein_organism_trgm` index
- [ ] B-tree indexes on: `accession`, `gene_name_primary`, `organism_name`, `length`, `molecular_weight`, `reviewed`,
  `evidence_level`, `updated_date`

### Backend — Entity Layer

- [x] `ProteinEntry` entity with all fields from domain model
- [x] `Keyword` entity + `ProteinKeyword` join entity
- [x] `GoTerm` entity + `ProteinGoTerm` join entity
- [x] `CrossReference` entity
- [x] `ProteinFeature` entity
- [ ] `HostOrganism` entity
- [ ] `ProteinComment` entity
- [ ] `ProteinPublication` entity
- [x] All `@ManyToOne` / `@OneToMany` mappings with `FetchType.LAZY`

### Backend — DTO Layer

- [ ] `ProteinSummaryDto` — flat projection for list/search results
- [ ] `ProteinDetailDto` — full detail including nested items
- [ ] `FeatureItemDto`, `GoTermItemDto`, `CrossReferenceItemDto`, `CommentItemDto`, `PublicationItemDto`,
  `HostOrganismItemDto`
- [ ] `GeneSearchRequest` — all optional filter fields with `@Valid` constraints
    - [ ] Cross-field `@AssertTrue isLengthRangeValid()`
    - [ ] Cross-field `@AssertTrue isMolecularWeightRangeValid()`
    - [ ] `@Pattern` for `goTermId` matching `GO:\d{7}`
    - [ ] `@Min`/`@Max` on `evidenceLevels` items (1–5)
- [ ] `PagedResponse<T>` generic envelope

### Backend — Repository

- [ ] `ProteinEntryRepository extends JpaRepository, JpaSpecificationExecutor`
- [ ] Custom JPQL fetch-join queries for detail endpoint (avoid N+1)
- [ ] Sort whitelist validation in service layer

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
- [ ] `GeneService.searchGenes(GeneSearchRequest)` — composes specification from non-null fields
- [ ] `GeneService.getGeneById(Long id)` — fetch join all relations; throw `GeneNotFoundException` if missing
- [ ] `GeneService.exportCsv(GeneSearchRequest, HttpServletResponse)` — streams via `StreamingResponseBody`; hard cap
  100,000 rows

### Backend — Controller

- [x] Remove `UnsupportedOperationException` stubs
- [ ] Wire `GeneService`; keep controller thin
- [ ] Return correct HTTP status codes (`200`, `400`, `401`, `404`)

### Backend — Tests

- [ ] `GeneServiceTest` — unit tests (mock repository):
    - [ ] `listGenes_returnsPage`
    - [ ] `listGenes_invalidSort_throws`
    - [ ] `searchGenes_withOrganism`
    - [ ] `searchGenes_lengthRangeInvalid_throws`
    - [ ] `getGeneById_found`
    - [ ] `getGeneById_notFound_throws`
- [ ] `GeneControllerIntegrationTest` — Testcontainers PostgreSQL:
    - [ ] `GET /api/genes` — 200 with paged body
    - [ ] `GET /api/genes?sort=invalid` — 400
    - [ ] `POST /api/genes/search` — 200 with filtered result
    - [ ] `POST /api/genes/search` with invalid filter — 400
    - [ ] `GET /api/genes/{id}` — 200 full detail
    - [ ] `GET /api/genes/9999` — 404
    - [ ] `POST /api/genes/export-csv` — 200 CSV content-type

### General

- [ ] Code reviewed
- [ ] Coverage ≥ 80% (JaCoCo report recorded in journal)
