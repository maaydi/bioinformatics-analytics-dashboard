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

| # | Task                                                                                          | Layer   | Status |
|---|-----------------------------------------------------------------------------------------------|---------|--------|
| 1 | Create `documentation/implementation/REMOTE-001/` files                                       | Docs    | ✅ Done |
| 2 | Create `UniprotQueryBuilder` — map `GeneSearchRequest` → UniProt Lucene query string          | Backend | ⬜ Todo |
| 3 | Update `UniprotKbRestService` — add `searchFiltered()`, `getByAccession()`, `fetchKeywords()` | Backend | ⬜ Todo |
| 4 | Create `UniprotKbGeneMapper` — map `UniProtEntry` → `ProteinSummaryDto` / `ProteinDetailDto`  | Backend | ⬜ Todo |
| 5 | Create `UniprotKbGeneService` — full `GeneService` implementation                             | Backend | ⬜ Todo |
| 6 | Unit tests: `UniprotQueryBuilderTest`                                                         | Tests   | ⬜ Todo |
| 7 | Unit tests: `UniprotKbGeneMapperTest`                                                         | Tests   | ⬜ Todo |
| 8 | Unit tests: `UniprotKbGeneServiceTest`                                                        | Tests   | ⬜ Todo |

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
    ├──► UniprotQueryBuilder.build(GeneSearchRequest) ──► query string
    │
    ├──► UniprotKbRestService.searchFiltered(query, size, cursor)
    │        └── RestClient → GET https://rest.uniprot.org/uniprotkb/search
    │
    └──► UniprotKbGeneMapper
             ├── toSummary(UniProtEntry) → ProteinSummaryDto
             └── toDetail(UniProtEntry)  → ProteinDetailDto
```

---

## Package Layout

```
providers/uniprotkb/
  query/
    UniprotQueryBuilder.java          ← NEW: pure mapping, no Spring deps
  gene/
    UniprotKbGeneService.java         ← NEW: GeneService implementation
    UniprotKbGeneMapper.java          ← NEW: DTO-level mapper (no JPA entity)
  service/
    UniprotKbRestService.java         ← UPDATE: add filtered + by-accession methods
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

---

## Detail View Strategy (`getGeneById`)

Since UniProt uses accession strings (e.g. `P12345`) and the `GeneService` contract uses `Long id`:

1. When `searchGenes` or `listGenes` returns results, each `ProteinSummaryDto.id` =
   `Math.abs(accession.hashCode()) as Long`
2. The service maintains a bounded in-memory map `Map<Long, String>` (id → accession), max 5000 entries (LRU).
3. `getGeneById(Long id)` looks up the accession in the map, then fetches full entry via `GET /uniprotkb/{accession}`.
4. If the id is not in the cache (e.g. stale bookmark), throw `ResourceNotFoundException`.

---

## Coverage Target

| Component              | Target |
|------------------------|--------|
| `UniprotQueryBuilder`  | ≥ 90%  |
| `UniprotKbGeneMapper`  | ≥ 85%  |
| `UniprotKbGeneService` | ≥ 80%  |

