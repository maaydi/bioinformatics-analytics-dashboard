# REMOTE-001 — UniProtKB Remote Provider (GeneService over REST)

## Description

Implement a fully functional `GeneService` provider that fetches protein/gene data **live from the UniProtKB REST API
** (`https://rest.uniprot.org/uniprotkb`) instead of the local PostgreSQL database.

This enables the application to run in a **remote-only mode** (no local data import required) by routing all gene
search, listing, and detail requests to the official UniProt web service.

The active provider is selected at runtime via the `X-Data-Provider: uniprotKb` HTTP request header, which is already
handled by `ProviderFilter` and `ProviderContextHolder`.

---

## Goals

| # | Goal                                                                                         |
|---|----------------------------------------------------------------------------------------------|
| 1 | Map all `GeneSearchRequest` fields to valid UniProtKB Lucene query syntax                    |
| 2 | Implement `UniprotKbGeneService` that satisfies the full `GeneService` contract              |
| 3 | Map `UniProtEntry` REST DTOs directly to `ProteinSummaryDto` and `ProteinDetailDto` (no JPA) |
| 4 | Support cursor-based pagination bridged to page/size semantics                               |
| 5 | Support CSV export via streamed remote fetch                                                 |
| 6 | Register the new service in the existing dispatcher with provider name `uniprotKb`           |

---

## Acceptance Criteria

- [ ] `POST /api/genes/search` with header `X-Data-Provider: uniprotKb` returns valid `PagedResponse<ProteinSummaryDto>`
  populated from UniProt REST API
- [ ] `GET /api/genes` with header `X-Data-Provider: uniprotKb` returns paginated protein summaries
- [ ] `GET /api/genes/{id}` with header `X-Data-Provider: uniprotKb` returns `ProteinDetailDto` fetched by accession
- [ ] `POST /api/genes/export-csv` with header `X-Data-Provider: uniprotKb` produces valid CSV output
- [ ] `GET /api/genes/keywords` with header `X-Data-Provider: uniprotKb` returns a keyword list
- [ ] All `GeneSearchRequest` filter fields are translated to UniProt query parameters
- [ ] Unit tests cover `UniprotQueryBuilder` for all filter combinations
- [ ] Unit tests cover `UniprotKbGeneMapper` (summary + detail mapping)
- [ ] Unit tests cover `UniprotKbGeneService` (mock REST client)

---

## Scope — Out of Scope

**In scope:**

- `UniprotQueryBuilder` — pure mapping from `GeneSearchRequest` → UniProt Lucene query string
- `UniprotKbGeneMapper` — `UniProtEntry` → `ProteinSummaryDto` / `ProteinDetailDto`
- `UniprotKbGeneService` — full `GeneService` implementation (list, search, detail, export, keywords)
- Update `UniprotKbRestService` — add filtered search, single-entry fetch, keyword retrieval
- Documentation update

**Out of scope:**

- Modifying any PostgreSQL provider code
- Changing the REST API contract (`api-contract.md`)
- Frontend changes (provider selection header is already configurable)
- Persistent caching of remote results (out of MVP scope)

---

## Constraints & Risks

| Risk                                                                       | Mitigation                                                                                     |
|----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| UniProt rate limiting                                                      | Add `Retry-After` aware retry + exponential backoff via RestClient interceptor                 |
| Cursor-based pagination vs page/size                                       | Bridge: page 0 = direct call; page N = iterate cursors (N ≤ 10 limit enforced)                 |
| `getGeneById(Long id)` requires numeric id, UniProt uses accession strings | Use a bounded in-memory map `Long → accession` populated on each search response               |
| UniProt max page size = 500                                                | Cap `size` param at `min(request.size(), 500)`                                                 |
| Network latency                                                            | Document that UniProtKB remote provider does not meet the ≤1s NFR; acceptable for non-prod use |
| `totalElements` accuracy                                                   | Parse `X-Total-Results` header from UniProt response                                           |

---

## Authorization

Same as existing gene endpoints:

- `ROLE_USER` required
- `ROLE_ADMIN` required for export (same policy as Postgres provider)

---

## Related Files

| File                                                      | Role                                        |
|-----------------------------------------------------------|---------------------------------------------|
| `providers/uniprotkb/AbstractUniprotKbProvider.java`      | Base class — provider name `"uniprotKb"`    |
| `providers/uniprotkb/service/UniprotKbRestService.java`   | HTTP client wrapper — to be extended        |
| `providers/uniprotkb/config/UniprotRestClientConfig.java` | RestClient bean                             |
| `providers/uniprotkb/dto/UniProtEntry.java`               | UniProt REST response DTO                   |
| `providers/uniprotkb/mapper/UniProtEntryMapper.java`      | Existing mapper (entity-level) — not reused |
| `providers/dispatcher/GeneServiceDispatcher.java`         | Dispatcher — auto-discovers new bean        |
| `interfaces/gene/GeneService.java`                        | Contract to implement                       |
| `model/gene/GeneSearchRequest.java`                       | Filter input — all fields must be mapped    |

