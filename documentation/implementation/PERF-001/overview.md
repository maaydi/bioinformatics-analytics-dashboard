# PERF-001 — Database & ORM Performance Enhancement for 570k UniProt Proteins

## Context

The platform imports ~570,000 UniProt entries, generating approximately:

- 570,000 rows in `protein_entry`
- ~3,500,000 rows in `cross_reference` (~6.25 avg per protein)
- ~2,000,000+ rows in `protein_feature`, `protein_comment`, `protein_publication`

The current JPA/Hibernate configuration is not tuned for this scale.
This ticket addresses all identified bottlenecks from `analyse_db_model.md`.

---

## Identified Bottlenecks

| # | Problem                                         | Current Code                                                               | Impact at 570k scale                                                         |
|---|-------------------------------------------------|----------------------------------------------------------------------------|------------------------------------------------------------------------------|
| 1 | `GenerationType.IDENTITY` on all entities       | `ProteinEntry`, `CrossReference`, `ProteinFeature`, etc.                   | Disables JDBC batching — Hibernate issues individual INSERT + SELECT per row |
| 2 | No JDBC batch configuration                     | `application.yml` has no `hibernate.jdbc.batch_size`                       | Hibernate defaults to batch_size=1 effectively                               |
| 3 | `@OneToMany(cascade=ALL)` × 5 on `ProteinEntry` | `crossReferences`, `comments`, `publications`, `features`, `hostOrganisms` | Persistence context tracks millions of managed entities per chunk            |
| 4 | `tsvector` trigger fires on every INSERT        | `tg_pe_search_vector` runs PL/pgSQL for each of 570k rows                  | ~570k PL/pgSQL function calls during import                                  |
| 5 | Chunk size may be too large for big graphs      | Current chunk=500 with full entity graph                                   | Effective managed entity count: 500 × ~12 children = ~6000 entities/chunk    |
| 6 | Materialized views not verified as CONCURRENT   | Refresh strategy not enforced in code                                      | Risk of blocking reads during post-import refresh                            |

---

## Acceptance Criteria

1. **ID Generation** — All entities use `GenerationType.SEQUENCE` with `allocationSize = 500`
2. **JDBC Batching** — `hibernate.jdbc.batch_size=1000`, `order_inserts=true`, `order_updates=true`,
   `batch_versioned_data=true` enabled in `application.yml`
3. **Import throughput** — Full 570k import benchmark recorded in `journal.md`
4. **Memory stability** — JVM heap peak recorded; target < 2 GB sustained
5. **Aggregate refactoring** — `ProteinEntry` no longer holds `@OneToMany` for `CrossReference`, `ProteinComment`,
   `ProteinPublication`; these become unidirectional (`ManyToOne` side only)
6. **Flyway migration** — New PostgreSQL sequences added via `V11__add_sequences.sql`, non-destructive, uses
   `START WITH` based on current `MAX(id)`
7. **Import trigger optimization** — `search_vector` trigger disabled before batch import, `search_vector` recomputed in
   a single bulk `UPDATE` afterward, trigger re-enabled
8. **Materialized view refresh** — All 6 views refreshed `CONCURRENTLY` after import; called explicitly in
   `ImportService`
9. **Custom ItemWriter** — `ProteinAggregateWriter` replaces `JpaItemWriter<ProteinEntry>` for the three removed
   collections; persists children via dedicated repositories
10. **Unit tests** — `ImportServiceTest` updated; all existing tests pass; no regressions

---

## Out of Scope

- Migrating to JDBC-only ingestion pipeline (evaluated but deferred — JPA is sufficient at 570k with tuning)
- Offloading `sequence` TEXT column to a separate `protein_sequence` table (TOAST handles it; ~285 MB acceptable)
- Elasticsearch / OpenSearch integration (future roadmap item)
- Removing `@OneToMany` for `ProteinFeature`, `HostOrganism`, `Keyword`, `GoTerm` (smaller collections; cascade delete
  needed)

