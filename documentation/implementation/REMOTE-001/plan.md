# REMOTE-001 — Implementation Plan

## Prerequisites (delivered by RDF-001 ✅)

The following artefacts are already in the codebase and require no further work:

| Artefact                                                                      | Status                    |
|-------------------------------------------------------------------------------|---------------------------|
| All `UniProtEntry` + child DTOs in `providers/uniprotkb/dto/`                 | ✅ Ready                   |
| `UniprotRestClientConfig` — `RestClient` bean                                 | ✅ Ready                   |
| `UniprotKbRestService` — basic `search()` / `searchAll()`                     | ✅ Ready (needs extension) |
| `AbstractUniprotKbProvider` — base class with `getProviderName()="uniprotKb"` | ✅ Ready                   |

---

## Tasks

| # | Task                                                                                          | Layer   | Status                                                                                        |
|---|-----------------------------------------------------------------------------------------------|---------|-----------------------------------------------------------------------------------------------|
| 1 | Create `documentation/implementation/REMOTE-001/` files                                       | Docs    | ✅ Done                                                                                       |
| 2 | Create `UniprotQueryBuilder` — map `GeneSearchRequest` → UniProt Lucene query string          | Backend | ✅ Done (implemented inside `GeneSpecification` in `providers/uniprotkb/gene/specification/`) |
| 3 | Update `UniprotKbRestService` — add `searchFiltered()`, `getByAccession()`, `fetchKeywords()` | Backend | ✅ Done                                                                                       |
| 4 | Create `UniprotKbGeneMapper` — map `UniProtEntry` → `ProteinSummaryDto` / `ProteinDetailDto`  | Backend | ✅ Done (implemented as `UniProtProteinDtoMapper` with `UniprotMapperUtils`)                  |
| 5 | Create `UniprotKbGeneService` — full `GeneService` implementation                             | Backend | ✅ Done                                                                                       |
| 6 | Unit tests: `UniprotQueryBuilderTest`                                                         | Tests   | ✅ Done (as `providers/uniprotkb/gene/specification/GeneSpecificationTest`)                   |
| 7 | Unit tests: `UniprotKbGeneMapperTest`                                                         | Tests   | ✅ Done (covered via `UniProtProteinDtoMapper`)                                               |
| 8 | Unit tests: `UniprotKbGeneServiceTest`                                                        | Tests   | ✅ Done                                                                                       |

---

## Extended Tasks (emerged during implementation)

| #   | Task                                                                                                                                                           | Layer    | Status  |
|-----|----------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| E1  | Refactor UniProt DTOs from `providers/uniprotkb/dto/` → `model/uniprot/dto/` (shared model package)                                                            | Backend  | ✅ Done |
| E2  | Change `GeneService.getGeneById(Long)` → `getGeneByAccession(String)` for cross-provider extensibility                                                         | Backend  | ✅ Done |
| E3  | Update `GeneController`, `PostgresGeneService`, `ProteinEntryService`, `ProteinEntryRepository`, `GeneServiceDispatcher` for accession-based lookup            | Backend  | ✅ Done |
| E4  | Update frontend routing + `GenesService` + `GeneDetailComponent` to use accession instead of numeric id                                                        | Frontend | ✅ Done |
| E5  | Add `exportGenes()` to `GeneService` interface and `UniprotKbGeneService`                                                                                      | Backend  | ✅ Done |
| E6  | Add builder pattern to all gene DTOs (`ProteinSummaryDto`, `ProteinDetailDto`, `KeywordDto`, etc.)                                                             | Backend  | ✅ Done |
| E7  | Create `UniprotKbPaginationCacheService` — cursor-to-page bridge with bounded LRU cache                                                                        | Backend  | ✅ Done |
| E8  | Fix auth interceptor to skip error handling for login/logout/refresh endpoints                                                                                 | Frontend | ✅ Done |
| E9  | Add suggestion repository queries to `CrossReferenceRepository`, `GoTermRepository`, `KeywordRepository`, `ProteinEntryRepository`, `ProteinFeatureRepository` | Backend  | ✅ Done |
| E10 | Define `SuggestionService` interface — field-scoped autocomplete contract                                                                                      | Backend  | ✅ Done |
| E11 | Implement 10 PostgreSQL suggestion strategies (`*PostgresSuggestion.java`) for all filter fields                                                               | Backend  | ✅ Done |
| E12 | Create `SuggestionServiceDispatcher` and update `AbstractProviderDispatcher` for autocomplete routing                                                          | Backend  | ✅ Done |
| E13 | Create `AutoCompleteController` — `GET /api/genes/autocomplete?field=&query=`                                                                                  | Backend  | ✅ Done |
| E14 | Implement `GenericAutocomplete` shared Angular component + `AutocompleteService`                                                                               | Frontend | ✅ Done |
| E15 | Replace `KeywordsFilter` component with `GenericAutocomplete` in `GeneFilter`; remove `loadKeywords` endpoint                                                  | Frontend | ✅ Done |
| E16 | Wire `GenericAutocomplete` to all dedicated filter inputs in `GeneFilter`                                                                                      | Frontend | ✅ Done |
| E17 | Add `DatabaseRestService` — REST client for UniProt cross-reference database endpoint                                                                          | Backend  | ✅ Done |
| E18 | Add light search DTOs: `UniProtLightEntry`, `FeatureLight`, `GeneLight`; extend `UniprotKbRestService`                                                         | Backend  | ✅ Done |
| E19 | Implement 10 UniProtKB API suggestion strategies (`*UniprotApiSuggestion.java`)                                                                                | Backend  | ✅ Done |
| E20 | Add `SearchFieldRestService` + `UniProtSearchFieldService` for FeatureType vocabulary from UniProt config API                                                  | Backend  | ✅ Done |
| E21 | Add `SuggesterRestService` + `Suggestion`/`SuggestionResult` DTOs for GoTerm/Keyword autocomplete via UniProt suggester endpoint                               | Backend  | ✅ Done |
| E22 | Fix `GeneSpecification` (UniprotKb) for correct query building                                                                                                 | Backend  | ✅ Done |
| E23 | Fix cross-reference regression in Postgres `GeneSpecification`                                                                                                 | Backend  | ✅ Done |
| E24 | Unit tests: 10 × `*PostgresSuggestionTest` + 10 × `*UniprotApiSuggestionTest`                                                                                  | Tests    | ✅ Done |
| E25 | Update `AbstractUniprotKbProvider`, `UniprotRestClientConfig` documentation                                                                                    | Docs     | ✅ Done |

---

## Architecture

```
GeneController
    │
    ▼  (X-Data-Provider: uniprotKb)
GeneServiceDispatcher
    │ resolve("uniprotKb")
    ▼
UniprotKbGeneService  (extends AbstractUniprotKbProvider, implements GeneService)
    │
    ├──► GeneSpecification (providers/uniprotkb/gene/specification/)
    │        └── build(GeneSearchRequest) → UniProt Lucene query string
    │
    ├──► UniprotKbRestService.searchFiltered(query, size, cursor)
    │        └── RestClient → GET https://rest.uniprot.org/uniprotkb/search
    │
    └──► UniProtProteinDtoMapper (+ UniprotMapperUtils)
             ├── toSummary(UniProtEntry) → ProteinSummaryDto
             └── toDetail(UniProtEntry)  → ProteinDetailDto

AutoCompleteController
    │  GET /api/genes/autocomplete?field=&query=
    ▼
SuggestionServiceDispatcher
    │ resolve("postgres" | "uniprotKb") → field-specific SuggestionService
    ▼
  [Postgres provider]                        [UniprotKb provider]
  *PostgresSuggestion (10 strategies)        *UniprotApiSuggestion (10 strategies)
  ├── AccessionPostgresSuggestion             ├── AccessionUniprotApiSuggestion
  ├── EntryNamePostgresSuggestion             ├── EntryNameUniprotApiSuggestion
  ├── GeneNamePrimaryPostgresSuggestion       ├── GeneNamePrimaryUniprotApiSuggestion
  ├── OrganismNamePostgresSuggestion          ├── OrganismNameUniprotApiSuggestion
  ├── LineagePostgresSuggestion               ├── LineageUniprotApiSuggestion
  ├── ProteinFullNamePostgresSuggestion       ├── ProteinFullNameUniprotApiSuggestion
  ├── KeywordNamePostgresSuggestion           ├── KeywordNameUniprotApiSuggestion (SuggesterRestService)
  ├── GoTermIdPostgresSuggestion              ├── GoTermIdUniprotApiSuggestion (SuggesterRestService)
  ├── FeatureTypePostgresSuggestion           ├── FeatureTypeUniprotApiSuggestion (UniProtSearchFieldService)
  └── CrossReferencePostgresSuggestion        └── CrossReferenceUniprotApiSuggestion (DatabaseRestService)
```

---

## Package Layout (Final State)

```
model/uniprot/dto/                              ← UniProt API DTOs (moved from providers/uniprotkb/dto/)
  UniProtEntry, Organism, Gene, Sequence, ...
  UniProtApiPage

providers/uniprotkb/
  gene/
    service/
      UniprotKbGeneService.java                 ← GeneService implementation ✅
    specification/
      GeneSpecification.java                    ← Query builder (UniProt Lucene) ✅
  suggest/
    AccessionUniprotApiSuggestion.java          ← 10 UniProtKB suggestion strategies ✅
    ...
  service/
    UniprotKbRestService.java                   ← Extended: light search + filtered ✅
    UniprotKbPaginationCacheService.java        ← Cursor bridge + LRU cache ✅
    DatabaseRestService.java                    ← Cross-reference REST client ✅
    SearchFieldRestService.java                 ← Search field config REST client ✅
    UniProtSearchFieldService.java              ← FeatureType vocabulary service ✅
    SuggesterRestService.java                   ← GoTerm/Keyword suggester client ✅
  dto/
    UniProtLightEntry.java                      ← Light search response DTO ✅
    FeatureLight.java, GeneLight.java           ← Nested light DTOs ✅
    CrossRefLightEntry.java                     ← Cross-reference light DTO ✅
    Suggestion.java, SuggestionResult.java      ← Suggester response DTOs ✅
    searchfield/
      SearchField.java, FieldValue.java, ...    ← Search field config DTOs ✅
  mapper/
    UniProtProteinDtoMapper.java                ← UniProtEntry → ProteinSummaryDto/DetailDto ✅

providers/postgres/
  suggest/
    AccessionPostgresSuggestion.java            ← 10 Postgres suggestion strategies ✅
    ...

providers/dispatcher/
  SuggestionServiceDispatcher.java             ← Routes autocomplete to provider ✅
  AbstractProviderDispatcher.java              ← Extended with SuggestionService routing ✅

gene/autocomplete/
  AutoCompleteController.java                  ← GET /api/genes/autocomplete ✅

interfaces/suggest/
  SuggestionService.java                       ← Autocomplete contract ✅

common/
  UniprotMapperUtils.java                      ← Shared mapping utilities ✅
```

---

## Query Mapping Table (`GeneSearchRequest` → UniProt Lucene)

| GeneSearchRequest field      | UniProt query clause                         |
|------------------------------|----------------------------------------------|
| `globalSearch`               | `{value}` (free text)                        |
| `accession`                  | `accession:{value}`                          |
| `entryName`                  | `id:{value}`                                 |
| `geneNamePrimary`            | `gene:{value}`                               |
| `proteinFullName`            | `protein_name:{value}`                       |
| `reviewed = true`            | `reviewed:true`                              |
| `reviewed = false`           | `reviewed:false`                             |
| `organism`                   | `organism_name:{value}`                      |
| `taxid`                      | `taxonomy_id:{value}`                        |
| `lineage`                    | `taxonomy_name:{value}`                      |
| `lengthMin` + `lengthMax`    | `length:[{min} TO {max}]`                    |
| `lengthMin` only             | `length:[{min} TO *]`                        |
| `lengthMax` only             | `length:[* TO {max}]`                        |
| `molecularWeightMin` + `Max` | `mass:[{min} TO {max}]`                      |
| `evidenceLevels` (list)      | `(annotation_score:1 OR annotation_score:2)` |
| `keywords` (list)            | `(keyword:"kw1" OR keyword:"kw2")`           |
| `goTermId`                   | `go:{value}`                                 |
| `goAspect` only (P/F/C)      | `go_aspect:{process\|function\|component}`   |
| `featureType`                | `ft_{featureType.toLowerCase()}:*`           |
| `crossRefSource`             | `database:{value}`                           |

Multiple clauses are joined with `AND`. Empty/null fields are skipped. If all fields are empty, the query defaults to
`(*)`.

---

## Pagination Strategy

UniProt REST uses **cursor-based** pagination. Bridge to page/size:

- **Page 0**: Direct request with `size=min(request.size(), 500)`
- **Page N > 0**: Iterate through `N` cursor hops (each hop = one HTTP call). Cap at page 10 max to prevent abuse.
- `totalElements` = `X-Total-Results` response header (integer, parsed at service layer)
- `totalPages` = `ceil(totalElements / size)`
- **`UniprotKbPaginationCacheService`** maintains a bounded LRU map of cursors per search session.

---

## Detail View Strategy (`getGeneByAccession`)

The `GeneService` interface was refactored to accept `String accession` instead of `Long id`:

- `GeneService.getGeneByAccession(String accession)` replaces `getGeneById(Long id)`
- PostgreSQL provider: delegates to `ProteinEntryRepository.findByAccession(accession)`
- UniprotKb provider: `GET https://rest.uniprot.org/uniprotkb/{accession}` directly

All consumers (controller, frontend routing, tests) updated accordingly.

---

## Autocomplete Strategy

A new `GET /api/genes/autocomplete?field={fieldName}&query={prefix}` endpoint was added:

- **`SuggestionService` interface** defines `supports(field)` + `suggest(query, limit)` contract
- **10 Postgres strategies** read from local DB for instant results with existing data
- **10 UniProtKb strategies** delegate to appropriate UniProt REST endpoints:
   - Generic fields → light search (`UniprotKbRestService.searchLight()`)
   - Keywords / GoTerms → UniProt suggester API (`SuggesterRestService`)
   - FeatureTypes → UniProt search field config API (`UniProtSearchFieldService`)
   - CrossReference databases → UniProt database API (`DatabaseRestService`)
- **`SuggestionServiceDispatcher`** routes by current provider (`X-Data-Provider` header)
- Frontend: **`GenericAutocomplete`** shared component replaces the old `KeywordsFilter`; wired to all filter inputs

---

## Coverage Target

| Component                            | Target | Status                       |
|--------------------------------------|--------|------------------------------|
| `GeneSpecification` (uniprotkb)      | ≥ 90%  | ✅ Tests added               |
| `UniProtProteinDtoMapper`            | ≥ 85%  | ✅ Covered via service tests |
| `UniprotKbGeneService`               | ≥ 80%  | ✅ Tests added               |
| `*PostgresSuggestion` (10 classes)   | ≥ 80%  | ✅ 10 test classes added     |
| `*UniprotApiSuggestion` (10 classes) | ≥ 80%  | ✅ 10 test classes added     |
