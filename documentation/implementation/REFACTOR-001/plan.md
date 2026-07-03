# REFACTOR-001 — Implementation Plan

## Tasks

1. Add `Provider` abstraction and standardize service interfaces under `interfaces/*`.
2. Introduce `ProviderContextHolder` and `ProviderFilter` to capture request-scoped provider choice.
3. Implement `AbstractProviderDispatcher<T>` and concrete dispatchers for core services (`GeneServiceDispatcher`,
   `AnalyticsServiceDispatcher`).
4. Move existing PostgreSQL implementations under `providers/postgres/` and have them implement the provider interfaces
   and return `getProviderName() = "postgres"`.
5. Ensure dispatchers are `@Primary` so controllers continue to inject the interface.
6. Implement robust error handling for unknown providers (replace `RuntimeException` with domain exception and
   `@ControllerAdvice`).
7. Add unit tests for dispatcher resolution and `ProviderFilter` (header present / absent / unknown provider).
8. Add an integration test that registers a mock provider and verifies the dispatcher delegates correctly.
9. Update documentation and add `REFACTOR-001` implementation docs.
10. Add optional configuration: whitelist of allowed providers and admin endpoint to list providers.

## Status

- [x] Provider abstraction introduced
- [x] `ProviderFilter` and `ProviderContextHolder` added
- [x] `AbstractProviderDispatcher` implemented
- [x] `GeneServiceDispatcher` and `AnalyticsServiceDispatcher` implemented and marked `@Primary`
- [x] Postgres implementations moved under `providers/postgres` and adapted to implement interfaces
- [ ] Replace generic `RuntimeException` for missing provider with domain exception and map to 400/404
- [ ] Add provider whitelist configuration and validation
- [ ] Add integration tests for provider switching
- [ ] Add admin endpoint for provider discovery

---

## Detailed Checklist

### Dispatcher & Context

- [x] `ProviderContextHolder` — ThreadLocal storage for provider name
- [x] `ProviderFilter` — reads `X-Data-Provider` header and sets context; clears afterwards
- [x] `AbstractProviderDispatcher` — builds `Map<String, T>` from injected `List<T>` and logs registry size
- [x] `resolve()` method returns concrete provider or currently throws `RuntimeException` (to be replaced)

### Provider Implementations

- [x] `providers/postgres/*` — Postgres provider implementations moved here (e.g. `PostgresGeneService`)
- [x] `AbstractPostgresProvider` returns `getProviderName() = "postgres"`
- [x] All Postgres provider beans annotated for component scanning

### Tests

- [x] Unit tests for PostgresGeneService preserved/adapted
- [ ] Unit tests for `AbstractProviderDispatcher.resolve()` (nominal + unknown provider)
- [ ] Unit tests for `ProviderFilter` (header present/absent)
- [ ] Integration tests that inject a mock provider and assert dispatcher delegation

### Error Handling & Security

- [ ] Add `NoSuchDataProviderException` and map to `400 Bad Request` or `404 Not Found` via `GlobalExceptionHandler`
- [ ] Add application config for `app.providers.whitelist` and validate header values
- [ ] Add audit log entry when provider selection differs from default

### Documentation

- [x] `documentation/implementation/REFACTOR-001/overview.md` (this folder)
- [x] `documentation/implementation/REFACTOR-001/plan.md` (this file)
- [ ] `documentation/implementation/REFACTOR-001/journal.md` (to record commits & dates)

## How to add a new provider (quick)

1. Implement the service interface, e.g. `class MongoGeneService implements GeneService`.
2. Implement `getProviderName()` and return a short provider id (e.g. `mongo`).
3. Annotate the class with `@Component` or `@Service` so Spring auto-discovers it.
4. Ensure parity with required interface methods (search, list, detail, export, count).
5. Add unit tests and an integration test that uses `X-Data-Provider: <name>`.

## Commands

```bash
# Run backend unit tests
cd backend
./mvnw test

# Run single test class (example)
./mvnw -Dtest=com.bioinformatics.dashboard.providers.dispatcher.AbstractProviderDispatcherTest test
```

