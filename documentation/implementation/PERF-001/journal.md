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

All tasks in `plan.md` are **TODO** — awaiting implementation sprint.

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

> To be filled when T1 (SEQUENCE migration) implementation begins.

