# RDF-001 — Implementation Journal

---

## 2026-07-03

### Ticket created

- Created `overview.md`, `plan.md`, and `journal.md` in `documentation/implementation/RDF-001/` to track work.
- Purpose: add a SPARQL-backed `RdfGeneProvider` allowing runtime selection between `postgres` and `rdf` data sources.
- Next actions: design `GeneProvider` interface and spike a simple SPARQL query mapping to `ProteinSummaryDto`.

## 2026-07-04 — planned

- Design `GeneProvider` interface and add to `backend` codebase.
- Implement a minimal `RdfGeneProvider` prototype that runs a simple SPARQL query against a configurable endpoint and
  maps results to existing DTOs (no caching yet).
- Add unit tests for mapping and a contract test comparing the postgres and rdf providers for a small set of queries.

---

## Notes / Risks

- Remote SPARQL endpoints may enforce rate limits and return variable latencies — caching and a clear fallback strategy
  are required for production use.
- Not all filters supported by SQL may translate to efficient SPARQL—document differences and provide partial support
  metadata in responses where necessary.

