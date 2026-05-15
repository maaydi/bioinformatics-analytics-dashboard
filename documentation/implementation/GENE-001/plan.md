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
- [x] Entities implemented
- [⚠️] DTOs and mappers implemented — ⚠️ **PARTIAL** (missing 3 nested DTOs, CSV schema wrong)
- [⚠️] GeneSpecification implemented — ⚠️ **PARTIAL** (missing keywords, lineage filters)
- [⚠️] GeneService implemented — ⚠️ **PARTIAL** (N+1 query, no row limit, validation issues)
- [⚠️] CsvExportService implemented — ⚠️ **PARTIAL** (wrong schema, no escaping, no date in filename)
- [⚠️] GeneController completed — ⚠️ **PARTIAL** (sort validation missing, error handling)
- [ ] Unit tests written — **0% COVERAGE** (critical blocker)
- [ ] Integration tests written — **0% COVERAGE** (critical blocker)
- [ ] Documentation updated (code review added)
- [x] Code reviewed — ⚠️ **13 ISSUES IDENTIFIED** (4 critical, 7 major, 2 minor)
- [ ] Coverage ≥ 80% — **NOT MET**

---

## BLOCKING ISSUES (Code Review 2026-05-15)

**Status: 🔴 CRITICAL — 4 blockers must be fixed before integration testing**

See detailed findings in `code-review-2026-05-15.md`.

### 1. ❌ N+1 Query in getGeneById() — CRITICAL

- **Issue:** Uses `findById()` (lazy) instead of `findByIdWithAllRelations()`
- **Impact:** 1 + 7 additional queries (violates SLA)
- **Breaking:** API performance NFR test will fail
- **Fix:** Use optimized fetch-join query

### 2. ❌ CSV Export: Wrong Schema & Missing Row Limit — CRITICAL

- **Issue:** Exporting ProteinDetailDto (30+ fields) instead of ProteinSummaryDto (12 fields)
- **Issue:** No 100K row limit per api-contract
- **Issue:** Filename is `export.csv` not `proteins_2026-05-15.csv`
- **Impact:** API contract violation
- **Fix:** Create ProteinSummaryCsvDto, add limit(100_000), use dynamic filename

### 3. ❌ Missing Cross-Field Validation — CRITICAL

- **Issue:** `lengthMin > lengthMax` not validated in GeneSearchRequest
- **Issue:** `molecularWeightMin > molecularWeightMax` not validated
- **Impact:** Invalid requests return 200 with wrong results instead of 400
- **Breaking:** Contract validation test will fail
- **Fix:** Add @AssertTrue cross-field validators to DTO

### 4. ❌ Sort Whitelist Not Enforced — CRITICAL

- **Issue:** User can pass any column name as `sort` parameter
- **Impact:** 400 errors only caught by Hibernate, not whitelisted
- **Breaking:** API test will fail on invalid sort
- **Fix:** Whitelist validation in GeneService.listGenes()

### 5. ❌ Test Coverage 0% — CRITICAL BLOCKER

- **Issue:** No unit or integration tests exist
- **Impact:** Cannot verify any fixes work; coverage requirement ≥80% not met
- **Fix:** Write 20+ tests (unit + integration)
- **Estimate:** 6–8 hours

---

## Major Issues (High Priority - Fix Before QA)

### 6. ⚠️ Missing GeneSpecification Filters

- **Miss:** Keywords filter (required for keyword array searches)
- **Miss:** Lineage filter (required for taxonomy searches)
- **Impact:** Silently ignores these filter fields (wrong results)
- **Effort:** 2 hours

### 7. ⚠️ Incomplete Repository Fetch Join

- **Missing:** LEFT JOIN FETCH p.comments
- **Missing:** LEFT JOIN FETCH p.publications
- **Missing:** LEFT JOIN FETCH p.hostOrganisms
- **Impact:** Will still cause N+1 after primary fix
- **Effort:** 1 hour

### 8. ⚠️ CSV Field Escaping Missing

- **Issue:** No RFC 4180 escaping for commas/quotes/newlines
- **Impact:** Corrupted CSV if any field contains special chars
- **Effort:** 1 hour

### 9. ⚠️ Direction Parameter Error Handling

- **Issue:** Sort.Direction.fromString() throws uncaught exception if null/invalid
- **Issue:** Validation should be in DTO with @Pattern
- **Effort:** 0.5 hours

### 10. ⚠️ Missing Document Nested DTOs

- **Missing:** CommentItemDto
- **Missing:** PublicationItemDto
- **Missing:** HostOrganismItemDto
- **Missing:** MapStruct mappings for these types
- **Effort:** 2 hours

### 11. ⚠️ Plan Accuracy

- **Issue:** Many items marked complete when only partial
- **Fix:** Update plan.md to reflect realistic status
- **Effort:** 1 hour

### 12. ⚠️ CsvWriter Has No Escaping Strategy

- **Issue:** Plain field joining without RFC 4180 compliance
- **Fix:** Implement CSV escaping/quoting
- **Effort:** 1 hour

---

## Detailed Checklist

### Database Migration

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
- [ ] `HostOrganism` entity
- [ ] `ProteinComment` entity
- [ ] `ProteinPublication` entity
- [x] All `@ManyToOne` / `@OneToMany` mappings with `FetchType.LAZY`

### Backend — DTO Layer

- [✅] `ProteinSummaryDto` — flat projection for list/search results
- [⚠️] `ProteinDetailDto` — full detail including nested items — **⚠️ PARTIAL: CSV row() method has wrong schema (30
  fields vs 12)**
- [✅] `FeatureItemDto`, `GoTermDto`, `CrossReferenceDto` — implemented
- [❌] `CommentItemDto` — **❌ MISSING**
- [❌] `PublicationItemDto` — **❌ MISSING**
- [❌] `HostOrganismItemDto` — **❌ MISSING**
- [⚠️] `GeneSearchRequest` — all optional filter fields with `@Valid` constraints
    - [⚠️] Cross-field `@AssertTrue isLengthRangeValid()` — **❌ MISSING (BLOCKER)**
    - [⚠️] Cross-field `@AssertTrue isMolecularWeightRangeValid()` — **❌ MISSING (BLOCKER)**
    - [✅] `@Pattern` for `goTermId` matching `GO:\d{7}`
    - [✅] `@Min`/`@Max` on `evidenceLevels` items (1–5)
- [✅] `PagedResponse<T>` generic envelope

### Backend — Repository

- [✅] `ProteinEntryRepository extends JpaRepository, JpaSpecificationExecutor`
- [⚠️] Custom JPQL fetch-join queries for detail endpoint (avoid N+1) — **⚠️ PARTIAL: missing 3 collections (comments,
  publications, hostOrganisms)**
- [ ] Sort whitelist validation in service layer — **❌ NOT YET IMPLEMENTED (in Service section)**

### Backend — Specification

- [✅] `GeneSpecification.globalSearch(String)` — tsvector / ILIKE fallback
- [✅] `GeneSpecification.accessionLike(String)`
- [✅] `GeneSpecification.entryNameLike(String)`
- [✅] `GeneSpecification.geneNameLike(String)`
- [✅] `GeneSpecification.proteinNameLike(String)`
- [✅] `GeneSpecification.reviewed(Boolean)`
- [✅] `GeneSpecification.organismLike(String)`
- [✅] `GeneSpecification.taxid(Integer)`
- [❌] `GeneSpecification.lineageLike(String)` — **❌ MISSING (BLOCKER)**
- [✅] `GeneSpecification.lengthBetween(Integer min, Integer max)`
- [✅] `GeneSpecification.molecularWeightBetween(Integer min, Integer max)`
- [✅] `GeneSpecification.evidenceLevelsIn(List<Integer>)`
- [❌] `GeneSpecification.keywordsContainAll(List<String>)` — **❌ MISSING (BLOCKER)**
- [✅] `GeneSpecification.goTermId(String)`
- [✅] `GeneSpecification.goAspect(String)`
- [✅] `GeneSpecification.featureType(String)`
- [✅] `GeneSpecification.crossRefSource(String)`

### Backend — Service

- [⚠️] `GeneService.listGenes(page, size, sort, direction)` — validates sort field against whitelist — **❌ BLOCKER: sort
  whitelist not implemented**
- [⚠️] `GeneService.searchGenes(GeneSearchRequest)` — composes specification from non-null fields — **❌ BLOCKER: missing
  keywords/lineage, cross-field validation missing**
- [❌] `GeneService.getGeneById(Long id)` — fetch join all relations; throw `GeneNotFoundException` if missing — **❌
  BLOCKER: uses findById() (N+1), should use findByIdWithAllRelations()**
- [❌] `GeneService.exportCsv(GeneSearchRequest, HttpServletResponse)` — streams via `StreamingResponseBody`; hard cap
  100,000 rows — **❌ BLOCKER: wrong schema (ProteinDetail vs Summary), no row limit, wrong filename**
  100,000 rows

### Backend — Controller

- [⚠️] Remove `UnsupportedOperationException` stubs — **✅ DONE**
- [⚠️] Wire `GeneService`; keep controller thin — **✅ DONE but see blockers in service**
- [⚠️] Return correct HTTP status codes (`200`, `400`, `401`, `404`) — **⚠️ PARTIAL: error handling incomplete**

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

- [ ] Code reviewed — **✅ COMPLETE** (see code-review-2026-05-15.md)
- [ ] Coverage ≥ 80% (JaCoCo report recorded in journal) — **❌ 0% — BLOCKER**

---

## Priority Fix Order (Estimated Effort)

### Tier 1: Critical Blockers (Must fix before any testing)

1. **GeneService fixes** (2–3 hrs)
    - Use `findByIdWithAllRelations()` in `getGeneById()`
    - Add sort whitelist validation in `listGenes()`
    - Add cross-field validators to `GeneSearchRequest`

2. **CSV Export fixes** (2–3 hrs)
    - Create `ProteinSummaryCsvDto` (12 columns, not 30)
    - Add `.limit(100_000)` to export query
    - Add date to filename
    - Implement RFC 4180 CSV escaping

3. **Missing Validators** (1 hr)
    - `@AssertTrue isLengthRangeValid()` in DTO
    - `@AssertTrue isMolecularWeightRangeValid()` in DTO

**Subtotal: 5–7 hours**

### Tier 2: Major Quality Issues (Before QA)

1. **Missing specifications** (2 hrs)
    - Implement `GeneSpecification.keywords()`
    - Implement `GeneSpecification.lineage()`

2. **Missing DTOs & Mappers** (2 hrs)
    - Create `CommentItemDto`, `PublicationItemDto`, `HostOrganismItemDto`
    - Add MapStruct mappings

3. **Repository improvements** (1 hr)
    - Add missing FETCH JOINs

**Subtotal: 5 hours**

### Tier 3: Testing (Critical Coverage Requirement)

1. **Unit tests** (4–5 hrs)
    - GeneService: 8–10 test cases
    - Target: ≥85% coverage on service

2. **Integration tests** (4–5 hrs)
    - GeneController: 15–20 test cases
    - Testcontainers PostgreSQL
    - Target: ≥80% overall coverage

**Subtotal: 8–10 hours**

### Tier 4: Polish (Low priority)

1. Error handling / logging (1 hr)
2. Documentation (0.5 hrs)

**TOTAL EFFORT TO "DONE": 18–24 hours**

---

## Risks & Mitigations

| Risk                          | Impact                        | Mitigation                                  |
|-------------------------------|-------------------------------|---------------------------------------------|
| N+1 queries not caught        | API SLA failure in production | Load testing with 100K+ rows before release |
| CSV schema mismatch           | Client parsing errors         | Strict contract-driven tests                |
| Missing filters silently fail | Wrong results to users        | Integration tests per filter combination    |
| Test coverage regression      | Quality unknown               | Enforce ≥80% gate in CI/CD                  |

---

## Success Criteria

- [x] Code review completed
- [ ] All 4 critical blockers fixed and tested
- [ ] All 8 major issues fixed and tested
- [ ] Unit test coverage ≥85% (GeneService)
- [ ] Integration test coverage ≥80% (GeneController)
- [ ] All endpoints return documented HTTP status codes
- [ ] CSV export produces RFC 4180-compliant output
- [ ] All queries analyzed for N+1 (max 1 query per collection)
- [ ] API response times meet NFR targets (< 500ms for list, < 100ms for detail)
