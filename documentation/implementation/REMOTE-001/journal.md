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

## Next Steps

- [ ] Implement `UniprotQueryBuilder` (Task 2)
- [ ] Update `UniprotKbRestService` with filtered + single-entry methods (Task 3)
- [ ] Implement `UniprotKbGeneMapper` (Task 4)
- [ ] Implement `UniprotKbGeneService` (Task 5)
- [ ] Write unit tests (Tasks 6–8)
- [ ] Run `./mvnw test` and verify ≥ 80% coverage on new classes
- [ ] Update `plan.md` task statuses

