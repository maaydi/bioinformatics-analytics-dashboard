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

- [ ] Requirements analyzed
- [ ] DB migration created
- [ ] Entities implemented
- [ ] DTOs and mappers implemented
- [ ] GeneSpecification implemented
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

- [ ] `ProteinEntry` entity with all fields from domain model
- [ ] `Keyword` entity + `ProteinKeyword` join entity
- [ ] `GoTerm` entity + `ProteinGoTerm` join entity
- [ ] `CrossReference` entity
- [ ] `ProteinFeature` entity
- [ ] `HostOrganism` entity
- [ ] `ProteinComment` entity
- [ ] `ProteinPublication` entity
- [ ] All `@ManyToOne` / `@OneToMany` mappings with `FetchType.LAZY`

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

- [ ] `GeneSpecification.globalSearch(String)` — tsvector / ILIKE fallback
- [ ] `GeneSpecification.accessionLike(String)`
- [ ] `GeneSpecification.entryNameLike(String)`
- [ ] `GeneSpecification.geneNameLike(String)`
- [ ] `GeneSpecification.proteinNameLike(String)`
- [ ] `GeneSpecification.reviewed(Boolean)`
- [ ] `GeneSpecification.organismLike(String)`
- [ ] `GeneSpecification.taxid(Integer)`
- [ ] `GeneSpecification.lineageLike(String)`
- [ ] `GeneSpecification.lengthBetween(Integer min, Integer max)`
- [ ] `GeneSpecification.molecularWeightBetween(Integer min, Integer max)`
- [ ] `GeneSpecification.evidenceLevelsIn(List<Integer>)`
- [ ] `GeneSpecification.keywordsContainAll(List<String>)`
- [ ] `GeneSpecification.goTermId(String)`
- [ ] `GeneSpecification.goAspect(String)`
- [ ] `GeneSpecification.featureType(String)`
- [ ] `GeneSpecification.crossRefSource(String)`

### Backend — Service

- [ ] `GeneService.listGenes(page, size, sort, direction)` — validates sort field against whitelist
- [ ] `GeneService.searchGenes(GeneSearchRequest)` — composes specification from non-null fields
- [ ] `GeneService.getGeneById(Long id)` — fetch join all relations; throw `GeneNotFoundException` if missing
- [ ] `GeneService.exportCsv(GeneSearchRequest, HttpServletResponse)` — streams via `StreamingResponseBody`; hard cap
  100,000 rows

### Backend — Controller

- [ ] Remove `UnsupportedOperationException` stubs
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
