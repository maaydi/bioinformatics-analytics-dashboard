# RDF-001 Implementation Plan

## Tasks

1. Create ticket folder `documentation/implementation/RDF-001/` and add overview/plan/journal (this ticket).
2. Define `GeneProvider` interface in backend (contract for search, detail, export operations).
3. Implement `PostgresGeneProvider` adapter (thin facade over existing repository/specification) if not already present.
4. Implement `RdfGeneProvider`:
    - Implement SPARQL query templates for each operation (search, summary, details, counts).
    - Use a parameterized, safe SPARQL construction approach to avoid injection and support pagination.
    - Map SPARQL result bindings → `ProteinSummaryDto` / other DTOs.
5. Add configuration: `application.yml` property `app.datasource.provider: postgres|rdf` and
   `app.datasource.rdf.endpoint` + credentials.
6. Implement caching layer for RDF results (TTL + configurable max-size). Prefer existing cache infra (Redis) or
   `Caffeine` local cache.
7. Add error handling: map SPARQL/HTTP errors → appropriate REST responses (502 / 504 / 500) with explanatory messages.
8. Add integration/contract tests comparing sample queries on both providers using a mock SPARQL server or recorded
   fixture data.
9. Add documentation: Ops runbook with rate-limit, caching, expected latencies, and recommended usage.
10. Add minimal frontend indicator/UI hook for provider toggle (optional): show current provider in UI header for
    transparency.
11. Security review: ensure credentials are stored in environment and not logged; add secrets docs.
12. Code review & merge, update `documentation/implementation/README.md` ticket catalog status.

## Status

- [x] Ticket created (overview/plan/journal)
- [ ] `GeneProvider` interface designed
- [ ] `RdfGeneProvider` implemented (in progress)
- [ ] Caching and config implemented
- [ ] Integration tests added
- [ ] Documentation and runbook completed

---

## Detailed Checklist

### Backend — Architecture

- [ ] `GeneProvider` interface defined under `gene/provider/` containing methods used by `GeneService`:
    - `PagedResponse<ProteinSummaryDto> search(GeneSearchRequest req)`
    - `ProteinDetailDto findByAccession(String accession)`
    - `InputStream export(GeneSearchRequest req)` (or similar streaming export contract)

- [ ] `PostgresGeneProvider` implementation delegates to existing repository/specifications.

- [ ] `RdfGeneProvider` responsibilities:
    - Bind filter DTO to SPARQL query parameters safely.
    - Support pagination using SPARQL OFFSET/LIMIT and/or cursor-based approach if performant.
    - Use HTTP client with configurable timeouts and retry/backoff for contacting remote SPARQL endpoint.
    - Transform SPARQL JSON results to DTOs.

### SPARQL Query Design

- [ ] Create parameterized templates for each filter combination where possible; prefer modular fragments for reuse.
- [ ] For full-text/global search, use UniProt RDF text search predicates if available; otherwise document the
  limitation.
- [ ] Provide a fallback plan if the SPARQL endpoint is rate-limited: return `503` or cached stale results with
  metadata.

### Caching & Rate Limits

- [ ] Implement caching (Caffeine or Redis). Default TTL = 30s for search endpoints, configurable.
- [ ] For heavy exports, require `postgres` provider or limit remote export size; document in API.

### Configuration

- [ ] `application.yml` entries:
  ```yaml
  app:
    datasource:
      provider: postgres # or rdf
      rdf:
        endpoint: https://sparql.uniprot.org/sparql
        username: ${RDF_USER:}
        password: ${RDF_PASSWORD:}
  ```

### Security

- [ ] Ensure SPARQL credentials are sourced from environment variables or vault.
- [ ] Do not log raw query parameters containing secrets.

### Tests

- [ ] Unit tests for `RdfGeneProvider` mapping functions (binding → DTO).
- [ ] Integration/contract tests that assert the REST JSON envelope is identical across providers for representative
  queries.
- [ ] Add test fixture or lightweight mock SPARQL server (WireMock with pre-recorded JSON) to run tests offline.

### Frontend

- [ ] No mandatory frontend changes required: REST contract unchanged.
- [ ] Optional: show provider source label in UI header (e.g., "Data source: Postgres / UniProt RDF") for transparency.

### Ops / Runbook

- [ ] Document SPARQL endpoint limits, expected latencies, recommended cache settings, and fallback plan.
- [ ] Add steps to rotate credentials and to disable RDF provider quickly if the remote service is unreliable.

---

