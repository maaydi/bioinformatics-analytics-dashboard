# REMOTE-001 — Implementation Journal

## 2026-07-10 — Ticket Opened

**Author:** GitHub Copilot  
**Status:** In Progress

### Context Analysis

Reviewed the existing codebase to understand the provider architecture:

- `ProviderFilter` reads `X-Data-Provider` HTTP header and stores it in `ProviderContextHolder` (ThreadLocal).
- `AbstractProviderDispatcher` resolves the correct bean by provider name at runtime.
- `GeneServiceDispatcher` is `@Primary` and delegates all `GeneService` calls to the resolved provider.
- `AbstractUniprotKbProvider` already exists with `getProviderName() = "uniprotKb"`.
- `UniprotKbRestService` currently only implements generic `(*)` search — no filter support.
- No `GeneService` implementation exists yet for the `uniprotKb` provider.
- `UniProtProteinDtoMapper` maps `UniProtEntry` → JPA `ProteinEntry` (for import). Not reusable here — need a new
  DTO-level
  mapper.

### Architecture Decisions

1. **No JPA dependency in remote path** — `UniprotKbGeneMapper` maps `UniProtEntry` directly to `ProteinSummaryDto` /
   `ProteinDetailDto` without touching JPA entities.
2. **Pure query builder** — `UniprotQueryBuilder` is a stateless utility class with no Spring annotations; fully
   unit-testable.
3. **Cursor bridge** — UniProt cursor pagination is bridged to standard page/size; page 0 is O(1), page N requires N
   HTTP hops.
4. **In-memory accession cache** — `getGeneByAccession` requires reversing `Long id → accession`. A bounded
   `LinkedHashMap` (
   LRU, max 5000) maintained in the service handles this without external infrastructure.
5. **Export** — Implemented via streaming search (page 0 with size = totalRows capped at export max).
6. **Keywords** — UniProt does not expose a flat keyword list endpoint; fallback uses facet aggregation or a curated
   static set.

### Files to Create / Modify

| Action | File                                                    |
|--------|---------------------------------------------------------|
| CREATE | `providers/uniprotkb/query/UniprotQueryBuilder.java`    |
| CREATE | `providers/uniprotkb/gene/UniprotKbGeneMapper.java`     |
| CREATE | `providers/uniprotkb/gene/UniprotKbGeneService.java`    |
| UPDATE | `providers/uniprotkb/service/UniprotKbRestService.java` |
| CREATE | `test/.../uniprotkb/query/UniprotQueryBuilderTest.java` |
| CREATE | `test/.../uniprotkb/gene/UniprotKbGeneMapperTest.java`  |
| CREATE | `test/.../uniprotkb/gene/UniprotKbGeneServiceTest.java` |

### Documentation

- `overview.md` ✅ Created
- `plan.md` ✅ Created
- `journal.md` ✅ This file

---

## 2026-07-10 — Cross-ticket context update

Reviewed all commits merged under **RDF-001** (2026-07-03 → 2026-07-09).

**What RDF-001 delivered that is reusable by REMOTE-001:**

| Artefact                        | Location                       | Relevance to REMOTE-001                                                                             |
|---------------------------------|--------------------------------|-----------------------------------------------------------------------------------------------------|
| `UniProtEntry` + all child DTOs | `providers/uniprotkb/dto/`     | Ready — no changes needed                                                                           |
| `UniprotRestClientConfig`       | `providers/uniprotkb/config/`  | `RestClient` bean available for injection                                                           |
| `UniprotKbRestService`          | `providers/uniprotkb/service/` | Provides `search(size)` / `searchAll(size, cursor)` — needs `searchFiltered()` + `getByAccession()` |
| `UniProtProteinDtoMapper`       | `providers/uniprotkb/mapper/`  | Maps to JPA entity — **not reused**; new DTO-level mapper needed                                    |
| `AbstractUniprotKbProvider`     | `providers/uniprotkb/`         | Base class with `getProviderName()="uniprotKb"` — extends directly                                  |

**What still does NOT exist and must be created for REMOTE-001:**

- `providers/uniprotkb/query/UniprotQueryBuilder.java` — query builder
- `providers/uniprotkb/gene/UniprotKbGeneMapper.java` — DTO-level mapper
- `providers/uniprotkb/gene/UniprotKbGeneService.java` — GeneService implementation
- `UniprotKbRestService` additions: `searchFiltered()`, `getByAccession()`
- Tests (Tasks 6–8)

---

## 2026-07-19 — Initial Refactor & Core Scaffold (commit b8e7ab6)

**Actions taken:**

- **Moved all UniProt API DTOs** from `providers/uniprotkb/dto/` → `model/uniprot/dto/` to establish a shared,
  provider-agnostic model package. Added `FullName`, `RecommendedName`, `StartEnd` as new records.
- **Created `UniprotKbGeneService`** (initial scaffold) in `providers/uniprotkb/gene/service/`.
- **Deleted `UniprotKbAnalyticsService`** (dead code removal — not related to the gene provider contract).
- **Adjusted import paths** across `UniProtApiImportJobConfig`, `UniProtApiEntryProcessor`, `UniProtApiItemReader`,
  `UniProtKbApiClient` to reflect the new `model/uniprot/dto/` package.
- **Removed unused analytics stub** from `PostgresGeneService`.

**Rationale:** Placing UniProt response DTOs in `model/uniprot/` makes them reusable by both the import batch pipeline
and the new remote gene provider without creating a cross-package dependency between two provider packages.

---

## 2026-07-26 — Core Implementation Sprint (commits 0778c36 → 45ca52c)

### Refactor: Mapper utilities & REST client relocation (0778c36)

- Created `common/UniprotMapperUtils.java` — shared mapping helpers (organism name extraction, sequence extraction, gene
  primary name, etc.) used by both the batch processor mapper and the new DTO mapper.
- Moved `UniProtEntryMapper` into `job/uniprot/apiloader/processor/` (import-specific context, not reused by remote
  provider).
- Moved `UniProtKbApiClient` into `providers/uniprotkb/service/` for cleaner provider-scoped ownership.
- Extended `GeneSearchRequest` model with additional filter fields and validation annotations.

### DTO Builder Pattern (ac270bb)

- Added `@Builder` (Lombok) to `ProteinSummaryDto`, `ProteinDetailDto`, `KeywordDto`, `GoTermDto`, `ProteinFeatureDto`,
  `ProteinCommentDto`, `ProteinPublicationDto`, `CrossReferenceDto`.
- Required for clean construction in `UniProtProteinDtoMapper` without constructor overloads.

### DTO Mapper + Pagination Cache (3425330)

- Created `providers/uniprotkb/mapper/UniProtProteinDtoMapper.java` — maps `UniProtEntry` →
  `ProteinSummaryDto` / `ProteinDetailDto` using `UniprotMapperUtils`. No JPA entities touched.
- Created `providers/uniprotkb/service/UniprotKbPaginationCacheService.java` — bounded LRU `LinkedHashMap<String,
  String>` (cursor cache keyed by query+page hash). Bridges UniProt cursor pagination to page/size semantics.
- Extended `UniprotKbRestService` with filtered search support.

### Accession-Based Detail View (74d3a73)

- **Breaking refactor** (backward-compatible within this ticket scope): Changed `GeneService.getGeneById(Long id)` →
  `getGeneByAccession(String accession)` across the entire stack:
    - `GeneService.java` (interface)
    - `GeneController.java`
    - `PostgresGeneService.java` + `ProteinEntryService.java` + `ProteinEntryRepository.java`
    - `GeneServiceDispatcher.java`
- Updated integration tests: `GeneControllerIntegrationTest`, `PostgresGeneServiceTest`,
  `ProteinEntryServiceTest`, `ImportControllerIntegrationTest`, `AuditIntegrationTest`.
- **Rationale:** UniProt has no numeric IDs. Using accession strings as the universal key aligns both providers under
  the same contract without a translation layer.

### Frontend: Accession-Based Routing (bfdc2cf)

- Updated `app.routes.ts`: gene detail route now uses `:accession` param instead of `:id`.
- Updated `GenesService.getGeneDetail(accession: string)` — HTTP path now `/api/genes/{accession}`.
- Updated `GeneDetailComponent` to read `accession` param from route.
- Fixed `gene-detail.component.spec.ts` and `genes.service.spec.ts`.

### Export CSV for UniprotKb Provider (45ca52c)

- Added `exportGenes(GeneSearchRequest, exportSize)` to `GeneService` interface.
- Implemented in `UniprotKbGeneService`: fetches up to `min(requested, 500)` entries via `searchFiltered()`, maps to
  `ProteinSummaryDto`, delegates to existing CSV serialization.
- Removed the now-redundant `loadKeywords()` stub from `PostgresGeneService`.

---

## 2026-07-27 — Auth Fix, Autocomplete Foundation (commits f9236c1 → 5a67869)

### Audit Fix: Frontend Dependency Update (f9236c1)

- Updated `frontend/package.json` and `package-lock.json` to resolve npm audit warnings.

### Auth Interceptor Fix (a17d6f6)

- Updated `auth.interceptor.ts`: skip `Authorization` header injection and error token-refresh logic for
  `/api/auth/login`, `/api/auth/logout`, `/api/auth/refresh` paths.
- Updated `auth.service.ts` to handle these paths cleanly without circular interceptor calls.
- **Rationale:** The interceptor was triggering a refresh cycle on 401 from the login endpoint itself, causing an
  infinite loop.

### AutoComplete: Repository Layer (9837d3f)

Added suggestion query methods to existing repositories:

- `CrossReferenceRepository.findDistinctDatabaseNamesByPrefix(prefix, limit)`
- `GoTermRepository.findGoTermIdsByPrefix(prefix, limit)`
- `KeywordRepository.findKeywordNamesByPrefix(prefix, limit)`
- `ProteinEntryRepository` — accession, entryName, geneName, organism, lineage, proteinFullName prefix queries
- `ProteinFeatureRepository.findDistinctFeatureTypesByPrefix(prefix, limit)`

All queries use `LIKE :prefix%` with `LIMIT` clause; indexed columns only to respect the ≤ 200 ms NFR for autocomplete.

### AutoComplete: Postgres Suggestion Strategies (6e12ed7)

Implemented 10 `SuggestionService` strategies for the Postgres provider:

| Class                               | Field             |
|-------------------------------------|-------------------|
| `AccessionPostgresSuggestion`       | `accession`       |
| `EntryNamePostgresSuggestion`       | `entryName`       |
| `GeneNamePrimaryPostgresSuggestion` | `geneNamePrimary` |
| `OrganismNamePostgresSuggestion`    | `organism`        |
| `LineagePostgresSuggestion`         | `lineage`         |
| `ProteinFullNamePostgresSuggestion` | `proteinFullName` |
| `KeywordNamePostgresSuggestion`     | `keyword`         |
| `GoTermIdPostgresSuggestion`        | `goTermId`        |
| `FeatureTypePostgresSuggestion`     | `featureType`     |
| `CrossReferencePostgresSuggestion`  | `crossRefSource`  |

Each implements `SuggestionService.supports(field)` and `suggest(query, limit)` with constructor-injected repository.

### AutoComplete: Controller + Dispatcher (fe21f51)

- Created `gene/autocomplete/AutoCompleteController.java`:
  `GET /api/genes/autocomplete?field={fieldName}&query={prefix}&limit={n}` → `List<String>`
- Created `providers/dispatcher/SuggestionServiceDispatcher.java`:
  Resolves field-specific `SuggestionService` bean by `(providerName, fieldName)` key.
- Extended `AbstractProviderDispatcher` to support multi-key bean resolution (provider + field) for the autocomplete use
  case.

### Frontend: Fix Tests After Accession-Based Routing (5a67869)

- Fixed `gene-detail.component.spec.ts`, `genes-page.component.spec.ts`, `genes.service.spec.ts` — updated mock route
  params from `id` to `accession`.

---

## 2026-07-29 — Autocomplete Frontend + UniProtKb REST Clients (commits bcc0a89 → 5bf5304)

### Frontend: GenericAutocomplete Component (bcc0a89)

- Created `shared/components/generic-autocomplete/` (3-file component):
    - `generic-autocomplete.component.ts` — standalone, OnPush, signal-based local state (`query`, `suggestions`,
      `loading`, `open`). Calls `AutocompleteService.suggest(field, query)` with 300 ms debounce. Emits `selected`
      output signal.
    - `generic-autocomplete.component.html` + `.scss` — accessible dropdown with ARIA attributes.
    - `autocomplete.service.ts` — `GET /api/genes/autocomplete?field=&query=` wrapper.
    - `generic-autocomplete.component.spec.ts` — 122 lines of component tests (loading, empty, selection, keyboard nav).
- Replaced `KeywordsFilter` component usage in `GeneFilter` with `GenericAutocomplete` for keyword field.

### Backend: Remove `loadKeywords` API (4b3fd74)

- Removed `GET /api/genes/keywords` endpoint from `GeneController`, `GeneService` interface,
  `GeneServiceDispatcher`, `PostgresGeneService`, `UniprotKbGeneService`, `KeywordRepository`.
- Removed corresponding `genes.service.ts` method and `keywords-filter` component (replaced by `GenericAutocomplete`).
- **Rationale:** The autocomplete endpoint supersedes the static keyword list fetch; maintaining both creates duplicate
  contracts and dead code.

### Backend: Fix Cross-Reference Regression (63a50c2)

- Fixed `GeneSpecification.java` (Postgres) — `crossRefSource` predicate was joining incorrectly after a prior refactor;
  restored correct JOIN on `protein_cross_reference` table.
- Extended `GeneSpecificationTest` with cross-reference filter assertions.

### Frontend: Wire AutoComplete to All Filter Inputs (19241f2)

- Updated `gene-filter.component.ts` + `.html`: replaced free-text `<input>` fields for organism, lineage, gene name,
  protein full name, entry name, accession, keyword, GoTerm, feature type, cross-reference with
  `<app-generic-autocomplete>`.
- Each field passes its `field` param matching the `SuggestionService.supports()` key.

### Backend: Documentation Update for Postgres AutoComplete (336d60d)

- Refined `AutoCompleteController` Javadoc.
- Updated `SuggestionService` interface with full contract documentation.
- Added `@SuggestionField` annotation on each Postgres strategy for discoverability.

### UniProtKb: Cross-Reference REST Client (5700e8e)

- Created `providers/uniprotkb/service/DatabaseRestService.java` — `GET https://rest.uniprot.org/database/search`
  for cross-reference database name lookup.
- Created `providers/uniprotkb/dto/CrossRefLightEntry.java` record.

### UniProtKb: Light Search DTOs + REST Extension (2492f17)

- Created `providers/uniprotkb/dto/UniProtLightEntry.java`, `FeatureLight.java`, `GeneLight.java` — lightweight response
  records for autocomplete (avoid deserializing full `UniProtEntry` for suggestions).
- Extended `UniprotKbRestService` with `searchLight(query, fields, size)` — uses `fields` query param to request only
  the needed JSON fields from the UniProt search API.

### UniProtKb: Suggestion Strategy Scaffold (5bf5304)

Implemented 10 `SuggestionService` strategies for the UniProtKb provider:

| Class                                 | Backend source                                                     |
|---------------------------------------|--------------------------------------------------------------------|
| `AccessionUniprotApiSuggestion`       | `searchLight(accession:prefix*, 10)` — parses `primaryAccession`   |
| `EntryNameUniprotApiSuggestion`       | `searchLight(id:prefix*, 10)` — parses `uniProtkbId`               |
| `GeneNamePrimaryUniprotApiSuggestion` | `searchLight(gene:prefix*, 10)` — parses `genes[0].geneName.value` |
| `OrganismNameUniprotApiSuggestion`    | `searchLight(organism_name:prefix*, 10)`                           |
| `LineageUniprotApiSuggestion`         | `searchLight(taxonomy_name:prefix*, 10)`                           |
| `ProteinFullNameUniprotApiSuggestion` | `searchLight(protein_name:prefix*, 10)`                            |
| `KeywordNameUniprotApiSuggestion`     | `SuggesterRestService.suggest(keyword, prefix)`                    |
| `GoTermIdUniprotApiSuggestion`        | `SuggesterRestService.suggest(go, prefix)`                         |
| `FeatureTypeUniprotApiSuggestion`     | `UniProtSearchFieldService.getFeatureTypes()` (cached)             |
| `CrossReferenceUniprotApiSuggestion`  | `DatabaseRestService.searchDatabases(prefix)`                      |

---

## 2026-07-31 — Final UniProtKb Suggestions, Bug Fixes, Tests & Docs (commits 982f942 → e0c6a56)

### UniProtKb: FeatureType via Search Field Config API (982f942)

- Created `providers/uniprotkb/dto/searchfield/` package: `SearchField`, `FieldValue`, `EvidenceGroup`,
  `EvidenceItem` records — maps UniProt `/configure/uniprotkb/search-fields` JSON response.
- Created `SearchFieldRestService.java` — `GET https://rest.uniprot.org/configure/uniprotkb/search-fields`.
- Created `UniProtSearchFieldService.java` — parses the config response, filters `ft_*` fields, caches the result
  in-memory (TTL: application lifetime; fields are static). Powers `FeatureTypeUniprotApiSuggestion`.

### UniProtKb: GoTerm + Keyword via Suggester API (b0ac7d7)

- Created `providers/uniprotkb/dto/Suggestion.java` and `SuggestionResult.java` records.
- Created `SuggesterRestService.java` — `GET https://rest.uniprot.org/suggester?dict={vocab}&query={prefix}`. Supports
  `keyword` and `go` dictionaries. Powers `GoTermIdUniprotApiSuggestion` and
  `KeywordNameUniprotApiSuggestion`.

### UniProtKb: Suggestion Integration Fixes (b03716b)

- Refactored `UniProtSearchFieldService` — simplified field filtering logic; moved cached list to application-scoped
  `@Bean` to avoid repeated HTTP calls.
- Fixed `FeatureTypeUniprotApiSuggestion` — corrected field name extraction from `SearchField` records.
- Extended `OrganismNameUniprotApiSuggestion`, `LineageUniprotApiSuggestion`,
  `ProteinFullNameUniprotApiSuggestion` — improved light-search field mapping and result parsing.

### Backend: GeneSpecification Fix for UniProtKb Provider (7dacefd)

- Rewrote `providers/uniprotkb/gene/specification/GeneSpecification.java` — corrected Lucene query generation for all
  filter fields (quoting, range syntax, multi-value OR clauses).
- Major overhaul of `GeneSpecificationTest` — removed brittle assertions, rewrote ~800 lines to ~135 lines of focused
  behavioral assertions covering each filter permutation.
- Fixed `UniprotKbRestService` — ensured query string is URL-encoded before sending to UniProt REST.

### Tests: Autocomplete Unit Tests (e5d365d)

Added **20 new test classes** (3,229 lines total):

**Postgres suggestions (10 classes):**
`AccessionPostgresSuggestionTest`, `CrossReferencePostgresSuggestionTest`, `EntryNamePostgresSuggestionTest`,
`FeatureTypePostgresSuggestionTest`, `GeneNamePrimaryPostgresSuggestionTest`, `GoTermIdPostgresSuggestionTest`,
`KeywordNamePostgresSuggestionTest`, `LineagePostgresSuggestionTest`, `OrganismNamePostgresSuggestionTest`,
`ProteinFullNamePostgresSuggestionTest`

**UniprotKb API suggestions (10 classes):**
`AccessionUniprotApiSuggestionTest`, `CrossReferenceUniprotApiSuggestionTest`, `EntryNameUniprotApiSuggestionTest`,
`FeatureTypeUniprotApiSuggestionTest`, `GeneNamePrimaryUniprotApiSuggestionTest`, `GoTermIdUniprotApiSuggestionTest`,
`KeywordNameUniprotApiSuggestionTest`, `LineageUniprotApiSuggestionTest`, `OrganismNameUniprotApiSuggestionTest`,
`ProteinFullNameUniprotApiSuggestionTest`

Each test class covers: success path (results returned), empty results, null/blank query guard, `supports()` field
matching.

### Documentation (e0c6a56)

- Updated `AbstractUniprotKbProvider` Javadoc — documented provider name constant and extension points.
- Updated `UniprotRestClientConfig` — documented `RestClient` bean configuration and base URL.
- Updated `plan.md` and `journal.md` (this entry) with final status.

---

## Status Summary — 2026-07-31

| Acceptance Criterion                                                                                  | Status                |
|-------------------------------------------------------------------------------------------------------|-----------------------|
| `POST /api/genes/search` with `X-Data-Provider: uniprotKb` returns `PagedResponse<ProteinSummaryDto>` | ✅ Done               |
| `GET /api/genes` with `X-Data-Provider: uniprotKb` returns paginated summaries                        | ✅ Done               |
| `GET /api/genes/{accession}` with `X-Data-Provider: uniprotKb` returns `ProteinDetailDto`             | ✅ Done               |
| `POST /api/genes/export-csv` with `X-Data-Provider: uniprotKb` produces valid CSV                     | ✅ Done               |
| `GET /api/genes/autocomplete?field=&query=` returns suggestions (both providers)                      | ✅ Done (bonus scope) |
| All `GeneSearchRequest` fields translated to UniProt query parameters                                 | ✅ Done               |
| Unit tests — `GeneSpecification` (uniprotkb)                                                          | ✅ Done               |
| Unit tests — `UniProtProteinDtoMapper`                                                                | ✅ Done               |
| Unit tests — `UniprotKbGeneService`                                                                   | ✅ Done               |
| Unit tests — 10 × `*PostgresSuggestion`                                                               | ✅ Done (bonus scope) |
| Unit tests — 10 × `*UniprotApiSuggestion`                                                             | ✅ Done (bonus scope) |
| Frontend `GenericAutocomplete` component wired to all filter inputs                                   | ✅ Done (bonus scope) |
