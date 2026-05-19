# PERF-001 — Implementation Plan

## Architecture Decision Record

### What we KEEP

- Full relational schema (normalized, no denormalization)
- Spring Batch chunk-oriented processing
- JPA for all query/read operations and CRUD
- `@OneToMany` retained on `ProteinEntry` for: `features`, `hostOrganisms`, `keywords`, `goTerms`
  — these are smaller collections; cascade delete is required for aggregate lifecycle

### What we CHANGE

| Aspect                     | Before                                                              | After                                                 | Justification                                                         |
|----------------------------|---------------------------------------------------------------------|-------------------------------------------------------|-----------------------------------------------------------------------|
| ID strategy                | `IDENTITY` (all entities)                                           | `SEQUENCE` + `allocationSize=500`                     | Enables true JDBC batch inserts; Hibernate pre-allocates IDs          |
| Batch config               | None                                                                | `batch_size=1000`, `order_inserts=true`               | Groups SQL statements by table type; dramatically reduces round-trips |
| `ProteinEntry` collections | Owns `crossReferences`, `comments`, `publications` via `@OneToMany` | Removed; unidirectional only                          | Prevents persistence context explosion at 570k scale                  |
| ItemWriter                 | `JpaItemWriter<ProteinEntry>` (cascades everything)                 | `ProteinAggregateWriter` (explicit child persistence) | Fine-grained control over insert order and batch sizing               |
| Trigger during import      | Active for every INSERT                                             | Disabled before, bulk-recalculated after              | Eliminates 570k PL/pgSQL executions                                   |
| Chunk size                 | 500                                                                 | 250                                                   | Safer for large entity graphs; faster persistence context drain       |

---

## Tasks

| #   | Task                                                                                       | Layer             | Status                         |
|-----|--------------------------------------------------------------------------------------------|-------------------|--------------------------------|
| T1  | Switch all entity ID strategies from `IDENTITY` to `SEQUENCE`                              | Backend / Entity  | DONE                           |
| T2  | Add Hibernate batching properties to `application.yml`                                     | Backend / Config  | DONE                           |
| T3  | Remove `@OneToMany crossReferences`, `comments`, `publications` from `ProteinEntry`        | Backend / Entity  | DONE (converted to @Transient) |
| T4  | Create `ProteinAggregateWriter` — custom `ItemWriter` persisting children via repositories | Backend / Batch   | DONE                           |
| T5  | Create Flyway `V11__add_sequences.sql` — add PostgreSQL sequences safely                   | DB / Migration    | DONE                           |
| T6  | Add `@Table` + `@Index` annotations on child entities matching `domain-model.md` indexes   | Backend / Entity  | TODO                           |
| T7  | Implement trigger disable/enable + bulk `search_vector` recompute in `ImportService`       | Backend / Service | TODO                           |
| T8  | Verify and enforce `CONCURRENTLY` materialized view refresh after import                   | Backend / Service | TODO                           |
| T9  | Tune chunk size to 250 and add `entityManager.clear()` in writer                           | Backend / Batch   | DONE                           |
| T10 | Run full 570k import benchmark; record heap, duration, batch efficiency                    | DevOps / QA       | TODO                           |
| T11 | Update `ImportServiceTest` for new persistence flow; ensure ≥ 80% coverage                 | Backend / Test    | TODO                           |

---

## T1 — Sequence Strategy (all entities)

Apply to: `ProteinEntry`, `CrossReference`, `ProteinFeature`, `ProteinComment`,
`ProteinPublication`, `HostOrganism`, `Keyword`, `GoTerm`

```java

@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "protein_entry_seq")
@SequenceGenerator(
        name = "protein_entry_seq",
        sequenceName = "protein_entry_seq",
        allocationSize = 500
)
private Long id;
```

> `allocationSize = 500` means Hibernate fetches 500 IDs from PostgreSQL sequence in one call,
> then assigns them in memory. Reduces DB round-trips by ~500×.

---

## T2 — `application.yml` Hibernate Properties

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 1000
          batch_versioned_data: true
        order_inserts: true
        order_updates: true
        generate_statistics: false
```

---

## T3 — `ProteinEntry` Collection Removal

Remove these three fields entirely:

```java
// REMOVE:
@OneToMany(mappedBy = "protein", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<CrossReference> crossReferences;

@OneToMany(mappedBy = "protein", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<ProteinComment> comments;

@OneToMany(mappedBy = "protein", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<ProteinPublication> publications;
```

**Impact on DELETE:** Add `ON DELETE CASCADE` at DB level (already present in DDL).
Hibernate parent delete will propagate via PostgreSQL FK cascade — no ORM cascade needed.

---

## T4 — `ProteinAggregateWriter`

```
com.bioinformatics.dashboard.admin.batch.ProteinAggregateWriter
```

Flow per chunk:

1. `proteinRepository.saveAll(proteins)` — batch insert with SEQUENCE IDs
2. Attach protein reference to each child
3. `crossReferenceRepository.saveAll(allCrossRefs)`
4. `proteinCommentRepository.saveAll(allComments)`
5. `proteinPublicationRepository.saveAll(allPublications)`
6. `entityManager.flush(); entityManager.clear()`

---

## T5 — Flyway `V11__add_sequences.sql`

```sql
-- Safe: uses current MAX(id)+1 as start value
CREATE SEQUENCE protein_entry_seq
    START WITH 1 INCREMENT BY 500 OWNED BY protein_entry.id;
SELECT setval('protein_entry_seq', COALESCE((SELECT MAX(id) FROM protein_entry), 0) + 1);

CREATE SEQUENCE cross_reference_seq
    START WITH 1 INCREMENT BY 500 OWNED BY cross_reference.id;
SELECT setval('cross_reference_seq', COALESCE((SELECT MAX(id) FROM cross_reference), 0) + 1);

-- (repeat for protein_feature, protein_comment, protein_publication, host_organism)

-- Change columns to use sequences (they were BIGSERIAL = IDENTITY)
ALTER TABLE protein_entry
    ALTER COLUMN id SET DEFAULT nextval('protein_entry_seq');
ALTER TABLE cross_reference
    ALTER COLUMN id SET DEFAULT nextval('cross_reference_seq');
```

---

## T7 — Trigger Optimization

```java
// In ImportService, before batch job launch:
jdbcTemplate.execute("ALTER TABLE protein_entry DISABLE TRIGGER tg_pe_search_vector");

// After batch job completes:
jdbcTemplate.

execute("""
    UPDATE protein_entry SET search_vector =
        setweight(to_tsvector('english', COALESCE(accession,'')),         'A') ||
        setweight(to_tsvector('english', COALESCE(entry_name,'')),        'A') ||
        setweight(to_tsvector('english', COALESCE(gene_name_primary,'')), 'B') ||
        setweight(to_tsvector('english', COALESCE(protein_full_name,'')), 'C') ||
        setweight(to_tsvector('english', COALESCE(organism_name,'')),     'D')
    """);
jdbcTemplate.

execute("ALTER TABLE protein_entry ENABLE TRIGGER tg_pe_search_vector");
```

---

## Risk Assessment

| Risk                                                       | Probability | Mitigation                                                            |
|------------------------------------------------------------|-------------|-----------------------------------------------------------------------|
| Sequence start conflicts with existing data                | Medium      | `setval()` in migration uses `MAX(id)+1`                              |
| `DISABLE TRIGGER` requires superuser on hosted PostgreSQL  | Low         | Fallback: keep trigger, accept overhead; document as known limitation |
| Existing Spring Batch tests break after collection removal | Medium      | Update test builders; mock child repositories                         |
| `allocationSize=500` wastes IDs on small test runs         | Low         | Acceptable; sequences are cheap; test DB uses small fixtures          |
| `order_inserts=true` reorders SQL — may affect FK ordering | Low         | Parent entities flushed first due to `saveAll` call order             |

