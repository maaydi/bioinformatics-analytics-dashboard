# PERF-001 — Ambiguities & Analysis

## Status: No blocking ambiguities — implementation can proceed

---

## Resolved Decisions

### 1. Which `@OneToMany` collections to remove from `ProteinEntry`?

**Decision:** Remove `crossReferences`, `comments`, `publications`.  
**Rationale:**

| Collection        | Avg rows/protein  | Total at 570k | Decision                                                                 |
|-------------------|-------------------|---------------|--------------------------------------------------------------------------|
| `crossReferences` | ~6.25             | ~3,500,000    | **REMOVE**                                                               |
| `comments`        | ~3–5              | ~2,000,000    | **REMOVE**                                                               |
| `publications`    | ~2–4              | ~1,500,000    | **REMOVE**                                                               |
| `features`        | ~5–10             | ~3,000,000    | **KEEP** — needed for feature-type filter queries via JPA Specifications |
| `hostOrganisms`   | ~0–2 (virus only) | ~100,000      | **KEEP** — small, lifecycle-bound                                        |
| `keywords` (M:N)  | ~3–8              | ~2,000,000    | **KEEP** — M:N join table managed by Hibernate; no `orphanRemoval`       |
| `goTerms` (M:N)   | ~5–10             | ~3,000,000    | **KEEP** — same as keywords                                              |

Removed collections will be queried via dedicated repositories:

```java
crossReferenceRepository.findByProteinId(id)
proteinCommentRepository.

findByProteinId(id)
proteinPublicationRepository.

findByProteinId(id)
```

---

### 2. `allocationSize` — 500 or 1000?

**Decision:** `allocationSize = 500` as a conservative starting point.  
**Rationale:** 500 is a safe default. If the benchmark (T10) shows sequence fetch contention,
increase to 1000. The sequence gap on restart is acceptable (PostgreSQL sequences are not
required to be gapless).

---

### 3. Trigger disable — does it require superuser on PostgreSQL?

**Decision:** Attempt `ALTER TABLE ... DISABLE TRIGGER` in `ImportService`.  
**Fallback:** If the PostgreSQL user lacks `SUPERUSER` or `TRIGGER` privilege,
catch the `DataAccessException` and log a `WARN`:

```
[PERF-001] Could not disable tg_pe_search_vector — trigger will run per-row during import.
Consider granting TRIGGER privilege to the application user.
```

The import still succeeds; performance degrades gracefully.

---

### 4. Should the `sequence` TEXT column be offloaded?

**Decision:** No — defer.  
**Rationale:** 570,000 × average 500 characters = ~285 MB of FASTA sequence data.
PostgreSQL TOAST compression handles this transparently. Columns > 2 KB are compressed
and stored out-of-line automatically. No schema change required.

---

### 5. `ON DELETE CASCADE` — does removing ORM cascade break deletes?

**Decision:** No — DB-level `ON DELETE CASCADE` already exists in all child table DDLs
(see `domain-model.md` FK definitions). Deleting a `ProteinEntry` will cascade
at the PostgreSQL level regardless of Hibernate cascade settings.

---

### 6. Does `order_inserts=true` break FK ordering?

**Decision:** No issue — the custom `ProteinAggregateWriter` explicitly calls:

1. `proteinRepository.saveAll(proteins)` first
2. child `saveAll()` calls after

Hibernate reorders statements *within* a table group, not across parent/child boundaries.
Since we flush parents before children in code, FK constraints are always satisfied.

---

## Open Questions (non-blocking)

| Question                                                                          | Owner  | Priority                          |
|-----------------------------------------------------------------------------------|--------|-----------------------------------|
| What is the actual measured import duration today (baseline)?                     | DevOps | Medium — needed for T10 benchmark |
| Is the PostgreSQL user `app_user` granted `TRIGGER` privilege?                    | DevOps | Low — fallback exists             |
| Should `protein_go_term` evidence_code be indexed? Not in current domain-model.md | Arch   | Low                               |

