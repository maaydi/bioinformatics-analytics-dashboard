# REFACTOR-001 — Pluggable Data Provider Architecture

## Description

Decouple domain services from a single PostgreSQL implementation by introducing a pluggable provider architecture. This
change enables the application to route data operations to different data backends (examples: PostgreSQL, MongoDB,
RDF/SPARQL endpoints, UniProt HTTP API) without altering controller-level code.

Goals:

- Introduce a small `Provider` abstraction and service interfaces (e.g. `GeneService`, `AnalyticsService`) used by
  controllers.
- Implement provider dispatchers that resolve the concrete provider implementation per request.
- Provide a default `postgres` provider implementation and move existing Postgres-specific services under a dedicated
  package (`providers/postgres`).
- Allow callers to select the provider per request via the `X-Data-Provider` header with a sensible default.
- Keep API contracts and DTOs unchanged for consumers.

## Scope

| Layer                | Artifact / Area                                                                  |
|----------------------|----------------------------------------------------------------------------------|
| Routing / Filter     | `providers/ProviderFilter` — reads `X-Data-Provider` header and sets context     |
| Context Holder       | `providers/ProviderContextHolder` — ThreadLocal holder for current provider name |
| Dispatcher           | `providers/dispatcher/AbstractProviderDispatcher` + `*ServiceDispatcher` beans   |
| Provider packages    | `providers/postgres/*` — Postgres implementations moved here                     |
| Interfaces           | `interfaces/*` — provider/service contracts (`GeneService`, `AnalyticsService`)  |
| Existing controllers | No changes; continue to inject service interface (dispatcher is `@Primary`)      |

## Acceptance Criteria

- [ ] Controllers inject `GeneService` / `AnalyticsService` interfaces; actual implementation resolved by dispatcher at
  runtime.
- [ ] Default provider when header is absent is `postgres`.
- [ ] Providers are discoverable by Spring (implementations annotated and registered as beans) and must expose
  `getProviderName()`.
- [ ] Adding a new provider requires: implement the service interface, provide `getProviderName()` string, annotate as
  component/service; no controller changes needed.
- [ ] Dispatchers log provider registry size at startup and reject unknown provider names with a mapped, well-documented
  error (preferably a domain exception mapped to HTTP 400 or 404).
- [ ] Unit tests cover dispatcher resolution logic and `ProviderFilter` behavior.
- [ ] Integration tests demonstrate switching provider via `X-Data-Provider` header (mock provider acceptable).
- [ ] Documentation added: `documentation/implementation/REFACTOR-001` with overview, plan, and journal.

## References

- `backend/src/main/java/com/bioinformatics/dashboard/providers/dispatcher/AbstractProviderDispatcher.java`
- `backend/src/main/java/com/bioinformatics/dashboard/providers/ProviderFilter.java`
- `backend/src/main/java/com/bioinformatics/dashboard/providers/ProviderContextHolder.java`
- `backend/src/main/java/com/bioinformatics/dashboard/providers/postgres/` (moved Postgres providers)
- `documentation/implementation/` — project implementation conventions

