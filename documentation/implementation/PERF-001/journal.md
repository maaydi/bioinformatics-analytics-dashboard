# PERF-001 — Implementation Journal

## 2026-05-19 — Ticket initialized

### Actions taken

- Read and fully analysed `analyse_db_model.md` (1938 lines)
- Reviewed all 8 JPA entity files in `backend/src/main/java/com/bioinformatics/dashboard/gene/entity/`
- Reviewed `documentation/domain-model.md` — DDL, indexes, materialized views, trigger definitions
- Identified 6 critical performance gaps (detailed in `overview.md`)
- Created `overview.md`, `plan.md`, `journal.md`, `analyse.md`

### Key findings

- `GenerationType.IDENTITY` used on ALL entities — JDBC batching is effectively disabled
- `ProteinEntry` owns 5 `@OneToMany` collections with `CascadeType.ALL` — persistence context will explode at 570k scale
- No `hibernate.jdbc.batch_size` anywhere in `application.yml`
- `tsvector` trigger `tg_pe_search_vector` fires on every INSERT — 570k additional PL/pgSQL calls
- Materialized views exist and are correct; `CONCURRENTLY` refresh strategy documented but not enforced

### Status

All tasks in `plan.md` are **TODO** unless otherwise marked. Several backend changes have been implemented and are
recorded below.

---

## Benchmark Targets (to fill after T10)

| Metric                                            | Baseline (estimated) | Target            | Actual |
|---------------------------------------------------|----------------------|-------------------|--------|
| Full 570k import duration                         | ~120+ min (untuned)  | < 30 min          | —      |
| JVM heap peak                                     | unknown              | < 2 GB            | —      |
| Hibernate SQL batch efficiency                    | ~0% (IDENTITY)       | > 90%             | —      |
| `cross_reference` insert rate                     | unknown              | > 50 000 rows/sec | —      |
| `search_vector` bulk UPDATE duration              | N/A                  | < 5 min           | —      |
| Materialized view refresh duration (CONCURRENTLY) | unknown              | < 2 min           | —      |

---

## Next Entry

---

## 2026-05-19 — Code Review & Status Update

### Actions taken

- Performed an adversarial review of the `batch` package (focus: reader, processors, writer, job config).
- Ran and added unit tests for `LineProcessorRegistry` (tests added under
  `backend/src/test/java/.../LineProcessorRegistryTest.java`).
- Verified presence of `ProteinAggregateItemWriter` and supporting `ProteinEntryWriterConfig` bean.
- Verified Flyway migration `V11__add_sequences.sql` exists and config includes Hibernate batching properties.

### Quick status (implementation progress)

- T1 (SEQUENCE ids): DONE — entities use `GenerationType.SEQUENCE` with allocationSize=500 (e.g., `ProteinEntry`).
- T2 (JDBC batching): DONE — `application.yml` contains `hibernate.jdbc.batch_size=1000` and related settings.
- T3 (remove cascaded OneToMany): DONE — large child collections converted to `@Transient` and persisted explicitly by
  writer.
- T4 (ProteinAggregateWriter): DONE — `ProteinAggregateItemWriter` implemented and wired.
- T5 (Flyway sequences): DONE — `V11__add_sequences.sql` present in `db/migration`.
- T9 (chunk tuning & EM clear): DONE — default chunk size = 250 in `application.yml`; writers call
  `entityManager.clear()`.

### Findings & Recommendations (actionable)

1. ProteinAggregateItemWriter
   - Finding: writer uses `org.springframework.batch.infrastructure.item.Chunk` and `ItemWriter` (infrastructure
     package). Prefer public API types (`org.springframework.batch.item.ItemWriter` and `List<? extends T> write(...)`)
     for clearer compatibility and to avoid reliance on internal classes.
   - Finding: code assumes child collections are non-null; `ProteinEntry` currently initializes with empty collections,
     so safe, but guard or add explicit `Objects.requireNonNull` for clarity.
   - Action: replace infrastructure imports with public API and update method signature to
     `write(List<? extends ProteinEntry> items)`. Add null-safety checks.

2. LineProcessorRegistry
   - Finding: constructor collects processors using `LineProcessor::getPrefix`; `process(...)` uses
     `line.substring(0,2)` to lookup by a 2-char key. This is an implicit contract (prefix length == 2) not enforced
     anywhere.
   - Finding: `process` does not validate `line` for null (NPE risk) and logs `Regsitry` typo.
   - Action: enforce prefix length in implementations (or in registry constructor), add
     `Objects.requireNonNull(line, "line")` at method start, and either use `startsWith` matching or require
     `getPrefix().length()==2` and validate in constructor.

3. Trigger & Materialized-view orchestration (T7 / T8)
   - Finding: I did not find implementation for disabling `tg_pe_search_vector` or refreshing materialized views
     `CONCURRENTLY`. These remain TODO.
   - Action: implement trigger disable/enable and bulk `UPDATE` of `search_vector` in `ImportService`. Ensure code runs
     with appropriate DB privileges or provide documented fallback.

4. Testing & Benchmarking (T10 / T11)
   - Finding: Unit tests were added for `LineProcessorRegistry`. Integration/smoke tests and the full import benchmark
     are still TODO.
   - Action: Create `ImportServiceTest` that verifies trigger disable/enable flow (mock jdbcTemplate), and an
     integration benchmark harness that runs import against a realistic test dataset in Testcontainers Postgres.

### Next steps (recommended immediate tasks)

1. Patch `ProteinAggregateItemWriter` to use Spring Batch public ItemWriter API and add defensive null checks. Write
   unit tests for writer behavior (flush, clear, child persistence order).
2. Add trigger disable/enable + bulk `search_vector` recompute to `ImportService` and unit tests covering the SQL
   execution path.
3. Implement a small integration test using Testcontainers to run a short import (e.g., 1000 records) and validate
   batching behaviour (verify inserts grouped).

---

## Next Entry

> To be filled when T7/T8 implementation begins or when the full benchmark (T10) runs.

