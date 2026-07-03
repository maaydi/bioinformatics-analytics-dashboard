# RDF-001 — Remote UniProt RDF Data Provider

## Description

Add an alternate data provider that queries the remote UniProt RDF/SPARQL endpoint so users can:

- Toggle the application data source between the local PostgreSQL provider and the remote UniProt RDF/SPARQL provider.
- Execute search, filter and summary queries against the remote RDF endpoint using SPARQL when the RDF provider
  is active.
- Preserve identical API contracts for frontend consumers: the REST endpoints remain the same but may delegate
  to a different provider implementation under the hood.

Motivation: allow live queries against the authoritative UniProt RDF dataset for users who need up-to-date RDF-backed
results while keeping the existing Postgres SQL provider for performance and offline scenarios.

## Scope

| Layer             | Artifact / Responsibility                                                                  |
|-------------------|--------------------------------------------------------------------------------------------|
| Backend provider  | `gene.provider.rdf` — SPARQL-backed implementation of gene search / summary methods        |
| Backend config    | `app.datasource.provider` toggle + feature flag to prefer `postgres` or `rdf`              |
| Controller        | `GeneController` delegates to an abstraction `GeneProvider` (SQL or RDF)                   |
| Service layer     | `GeneService` uses `GeneProvider` for query execution and DTO mapping                      |
| Mapping / DTOs    | Reuse existing `dto` contracts (e.g., `ProteinSummaryDto`) — provider maps results to DTOs |
| Integration tests | Contract tests ensuring identical JSON envelope from both providers                        |
| Frontend          | `GenesService` unchanged; feature toggle UI to switch provider (optional)                  |
| Docs / Ops        | Runbook describing RDF rate-limits, caching and fallback strategy                          |

## Acceptance Criteria

- [ ] Application exposes the same REST API responses whether `postgres` or `rdf` provider is selected.
- [ ] New `GeneProvider` abstraction exists with two implementations: `PostgresGeneProvider` and `RdfGeneProvider`.
- [ ] `RdfGeneProvider` executes parameterized SPARQL queries against a configurable SPARQL endpoint and
  maps results to `ProteinSummaryDto` (and other DTOs used by existing endpoints).
- [ ] SPARQL queries support pagination and the common filters supported by `POST /api/genes/search` (global search,
  accession, organism, reviewed, length range, keywords, GO terms). When a filter cannot be expressed efficiently in
  SPARQL, the provider must document the limitation and return appropriate results with metadata indicating partial
  support.
- [ ] Caching layer (in-memory TTL or Redis) is implemented for RDF query results to avoid rate-limit issues; cache
  invalidation strategy documented in Ops runbook.
- [ ] Feature toggle `app.datasource.provider` (values: `postgres`, `rdf`) available in `application.yml` and respected
  at runtime (hot-reload not required).
- [ ] Integration tests compare a sample set of search queries against both providers and assert schema-equivalent
  responses (status codes, envelope shape, DTO fields). Tests must be runnable locally with a mock SPARQL server
  or recorded fixtures.
- [ ] Error handling: remote SPARQL failures produce `502 Bad Gateway` with a helpful message; validation errors remain
  `400 Bad Request`.
- [ ] Security: SPARQL endpoint credentials (if needed) stored in environment variables / secrets and not in source.
- [ ] Documentation: `documentation/implementation/RDF-001/plan.md` contains implementation steps and operational notes.

## References

- `documentation/api-contract.md` — authoritative REST contract; provider must not change API shape
- `documentation/domain-model.md` — domain definitions to map RDF properties → DTO fields
- `documentation/validation-rules.md` — validation rules for query DTOs
- `documentation/implementation/README.md` — ticket workflow and journal requirements

---

