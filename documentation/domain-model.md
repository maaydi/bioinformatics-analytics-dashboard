# UniProt SQL Domain Model

> Designed for **visualisation and filter-driven queries** in a Spring Boot + PostgreSQL stack.
> Every table, column, index, and view is justified by a specific UI feature from the spec.

---

## Design Goals

| Goal                           | Design Decision                                |
|--------------------------------|------------------------------------------------|
| Fast multi-filter queries      | Normalised schema + JPA Specifications         |
| Dashboard KPIs & charts        | Owner-computed statistics published to analytics-owned aggregate snapshots |
| Full-text search               | PostgreSQL `tsvector` column on key fields     |
| Flexible metadata storage      | `jsonb` fallback column on main table          |
| Array-typed multi-value fields | PostgreSQL native arrays + GIN indexes         |
| Import from `.dat` flat file   | Every UniProt line tag maps to a column        |

## Target Persistence Ownership (ARCH-002)

The following is the target ownership model. Catalog objects belong to `gene-service` in `gene_data`; analytics
refresh state belongs to `analytics-service` in `analytics`. The audit domain is independently owned by
`audit-service` in `audit_log`. No audit table, sequence, migration, or credential belongs to `gene-service`,
`gene_data`, `common-starter`, or an audit-event producer.

| Schema / data | Owner and role boundary |
|---------------|------------------------|
| `gene_data` | `gene_data_owner` runs Flyway and owns catalog objects. Runtime `gene_service_svc` is a member of `gene_data_rw`, which has catalog DML, sequence usage, and required function execution. |
| `analytics` | Analytics migrations and runtime are owned by analytics-service; `analytics_svc` has no privilege on `gene_data` or `audit_log`. |
| `audit_log` | `audit_service_owner` runs audit Flyway; `audit_service_svc` accesses audit persistence. Audit-service has no privilege on `gene_data`. |
| `import_batch`, `auth`, `export_data` | `import_svc`, `auth_svc`, and `export_svc` access only their own schemas and have no privilege on `gene_data` or `audit_log`. |

`gene_data_rw` is a `NOLOGIN` group role. `gene_data_owner` is a migration-only login; `gene_service_svc` is the
catalog runtime login. Default privileges for `gene_data_owner` grant future catalog tables, sequences, and required
functions to `gene_data_rw`. Revoke schema and database access from `PUBLIC`, then grant `CONNECT` and schema
privileges explicitly. The physical replica carries the grants; `gene_service_svc` is read-only there.

The catalog Flyway history is relocated from `public` to `gene_data` before changing the configured default schema.
The legacy V13-V17 audit records in that history are retained as historical validation only. Audit-service has an
independent Flyway history and an approved, rehearsed data/history handoff; the old audit migrations must not be
replayed against `gene_data` or used as audit-service migrations.

---

## Entity Relationship Overview

```text
protein_entry  ──< protein_keyword >── keyword
     │
     ├──< protein_feature
     ├──< protein_go_term >── go_term
     ├──< cross_reference
     ├──< protein_comment
     ├──< protein_publication
     └──< host_organism
```

---

## 1. `protein_entry` — Core Table

Maps directly to one UniProt `.dat` record (delimited by `//`).

The table is created in `gene_data`. `entry_version` below remains the UniProt source version; `revision` is the
independent technical version used for catalog change detection and event ordering. The catalog SQL examples in
this document are scoped to `gene_data` unless another schema is named.

```sql
CREATE TABLE protein_entry
(
    -- Primary key
    id                   BIGSERIAL PRIMARY KEY,

    -- === IDENTIFICATION (ID line) ===
    accession            VARCHAR(20)  NOT NULL UNIQUE,        -- AC: Q6GZX4
    entry_name           VARCHAR(50)  NOT NULL,               -- ID: 001R_FRG3G
    reviewed             BOOLEAN      NOT NULL DEFAULT FALSE, -- ID: Reviewed/Unreviewed

    -- === DATES (DT lines) ===
    integrated_date      DATE,                                -- DT: integrated into UniProtKB
    sequence_date        DATE,                                -- DT: sequence version 1
    updated_date         DATE,                                -- DT: entry version N
    sequence_version     SMALLINT,
    entry_version        SMALLINT,

    -- === CATALOG TRACEABILITY (ARCH-002) ===
    revision             BIGINT       NOT NULL DEFAULT 0,
    last_ingestion_id    TEXT,

    -- === PROTEIN NAME (DE lines) ===
    protein_full_name    TEXT,                                -- DE RecName: Full=...
    protein_short_name   VARCHAR(200),-- DE RecName: Short=...
    protein_ec_number    VARCHAR(50),                         -- DE EC=...

    -- === GENE NAME (GN lines) ===
    gene_name_primary    VARCHAR(100),-- GN Name=...
    gene_name_synonyms   TEXT[],                              -- GN Synonyms=...  (array)
    gene_orf_names       TEXT[],                              -- GN ORFNames=...  (array)
    gene_ordered_locus   TEXT[],                              -- GN OrderedLocusNames=... (array)

    -- === ORGANISM (OS / OC / OX lines) ===
    organism_name        VARCHAR(300) NOT NULL,               -- OS: full name
    organism_common_name VARCHAR(150),                        -- OS: common name in ()
    taxid                INTEGER      NOT NULL,               -- OX: NCBI_TaxID
    lineage              TEXT[],                              -- OC: array of taxonomy levels

    -- === SEQUENCE PROPERTIES (SQ line) ===
    length               INTEGER      NOT NULL,               -- SQ: 256 AA
    molecular_weight     INTEGER,                             -- SQ: 29735 MW
    sequence_checksum    VARCHAR(20),                         -- SQ: CRC64
    sequence             TEXT,                                -- actual FASTA sequence

    -- === EVIDENCE LEVEL (PE line) ===
    -- 1=Protein  2=Transcript  3=Homology  4=Predicted  5=Uncertain
    evidence_level       SMALLINT     NOT NULL CHECK (evidence_level BETWEEN 1 AND 5),

    -- === FULL-TEXT SEARCH ===
    -- Precomputed tsvector for fast search across name fields
    search_vector        TSVECTOR,

    -- === FLEXIBLE OVERFLOW ===
    -- Extra parsed fields or raw sections not yet mapped
    metadata_jsonb       JSONB,

    -- === ROW LIFECYCLE ===
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

`last_ingestion_id` stores the ingestion identifier that materially changed the row. A material merge increments
`revision` once and updates `updated_at` and `last_ingestion_id`; a no-op does not write the entry or emit an
entry-change event. The ingestion ledger uses the same text representation for its ID so it can retain the import
job's identifier without coupling to Spring Batch's internal numeric key.

### Indexes on `protein_entry`

```sql
-- Exact lookups (filter panel: accession, entry name)
CREATE UNIQUE INDEX idx_pe_accession ON protein_entry (accession);
CREATE INDEX idx_pe_entry_name ON protein_entry (entry_name);

-- Filter panel: organism, taxid
CREATE INDEX idx_pe_organism_name ON protein_entry (organism_name);
CREATE INDEX idx_pe_taxid ON protein_entry (taxid);

-- Filter panel: reviewed toggle
CREATE INDEX idx_pe_reviewed ON protein_entry (reviewed);

-- Filter panel: evidence level dropdown
CREATE INDEX idx_pe_evidence ON protein_entry (evidence_level);

-- Filter panel: length range slider
CREATE INDEX idx_pe_length ON protein_entry (length);

-- Filter panel: molecular weight range
CREATE INDEX idx_pe_mw ON protein_entry (molecular_weight);

-- Composite: common combined filter (reviewed + organism)
CREATE INDEX idx_pe_reviewed_org ON protein_entry (reviewed, organism_name);

-- Composite: common combined filter (reviewed + evidence)
CREATE INDEX idx_pe_reviewed_ev ON protein_entry (reviewed, evidence_level);

-- Array fields: gene synonyms and ORF names (GIN for ANY/contains queries)
CREATE INDEX idx_pe_synonyms ON protein_entry USING GIN (gene_name_synonyms);
CREATE INDEX idx_pe_orf ON protein_entry USING GIN (gene_orf_names);

-- Array field: taxonomy lineage (filter by kingdom/phylum/class)
CREATE INDEX idx_pe_lineage ON protein_entry USING GIN (lineage);

-- Full-text search across name fields
CREATE INDEX idx_pe_fts ON protein_entry USING GIN (search_vector);

-- JSONB: ad-hoc metadata queries
CREATE INDEX idx_pe_metadata ON protein_entry USING GIN (metadata_jsonb);
```

### Trigger: auto-update `search_vector`

```sql
-- Combines accession + entry_name + protein_full_name + gene_name_primary
CREATE
OR REPLACE FUNCTION trg_protein_entry_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector
:=
        setweight(to_tsvector('english', COALESCE(NEW.accession,         '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.entry_name,        '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.gene_name_primary, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.protein_full_name, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(NEW.organism_name,     '')), 'D');
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER tg_pe_search_vector
    BEFORE INSERT OR
UPDATE ON protein_entry
    FOR EACH ROW EXECUTE FUNCTION trg_protein_entry_search_vector();
```

---

## 2. `keyword` — Keyword Lookup

Maps to `KW` lines. Shared vocabulary across all entries.

```sql
CREATE TABLE keyword
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE -- e.g. "Activator", "Kinase"
);

CREATE UNIQUE INDEX idx_kw_name ON keyword (name);
```

---

## 3. `protein_keyword` — Protein ↔ Keyword (M:N)

```sql
CREATE TABLE protein_keyword
(
    protein_id BIGINT  NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    keyword_id INTEGER NOT NULL REFERENCES keyword (id) ON DELETE CASCADE,
    PRIMARY KEY (protein_id, keyword_id)
);

-- Lookups from keyword side (filter: keyword contains "X")
CREATE INDEX idx_pk_keyword ON protein_keyword (keyword_id);
```

> **Used by:** keyword frequency chart, keyword filter in Gene Explorer.

---

## 4. `go_term` — Gene Ontology Terms

Parsed from `DR   GO; GO:0046782; P:regulation of viral transcription; ...`

```sql
CREATE TABLE go_term
(
    id          SERIAL PRIMARY KEY,
    go_id       VARCHAR(15) NOT NULL UNIQUE, -- e.g. GO:0046782
    aspect      CHAR(1)     NOT NULL         -- P=Process F=Function C=Component
        CHECK (aspect IN ('P', 'F', 'C')),
    description TEXT        NOT NULL         -- e.g. "regulation of viral transcription"
);

CREATE UNIQUE INDEX idx_go_goid ON go_term (go_id);
CREATE INDEX idx_go_aspect ON go_term (aspect);
```

---

## 5. `protein_go_term` — Protein ↔ GO Term (M:N)

```sql
CREATE TABLE protein_go_term
(
    protein_id    BIGINT  NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    go_term_id    INTEGER NOT NULL REFERENCES go_term (id) ON DELETE CASCADE,
    evidence_code VARCHAR(10), -- IEA, IDA, IMP, ...
    PRIMARY KEY (protein_id, go_term_id)
);

-- Lookups from GO side (filter: has GO term)
CREATE INDEX idx_pgt_go ON protein_go_term (go_term_id);
-- Filter by GO aspect
CREATE INDEX idx_pgt_aspect ON protein_go_term (go_term_id) INCLUDE (protein_id);
```

> **Used by:** "has GO term" filter, future GO enrichment chart.

---

## 6. `cross_reference` — External Database Links

Maps to `DR` lines: EMBL, RefSeq, KEGG, Pfam, InterPro, SwissPalm, Proteomes…

```sql
CREATE TABLE cross_reference
(
    id            BIGSERIAL PRIMARY KEY,
    protein_id    BIGINT       NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    source        VARCHAR(30)  NOT NULL, -- EMBL | RefSeq | GO | Pfam | KEGG | InterPro …
    identifier    VARCHAR(100) NOT NULL, -- primary xref ID
    secondary_id  VARCHAR(100),          -- optional second field
    tertiary_info VARCHAR(200)           -- optional third field
);

CREATE INDEX idx_xref_protein ON cross_reference (protein_id);
CREATE INDEX idx_xref_source ON cross_reference (source);

-- Fast lookup: does protein have a RefSeq/Pfam/KEGG entry?
CREATE INDEX idx_xref_source_id ON cross_reference (source, identifier);
```

> **Used by:** Gene Detail page (cross-reference tab), "has Pfam domain" filter.

---

## 7. `protein_feature` — Annotated Sequence Features

Maps to `FT` lines: CHAIN, DOMAIN, SIGNAL, BINDING, REGION, VARIANT, MUTAGENESIS…

```sql
CREATE TABLE protein_feature
(
    id           BIGSERIAL PRIMARY KEY,
    protein_id   BIGINT      NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    feature_type VARCHAR(30) NOT NULL, -- CHAIN | DOMAIN | SIGNAL | BINDING | REGION …
    start_pos    INTEGER,              -- start AA position
    end_pos      INTEGER,              -- end AA position
    note         TEXT,                 -- /note="..."
    feature_id   VARCHAR(20)           -- /id="PRO_0000410512"
        evidence   TEXT                -- /evidence="ECO:0000256|SAM:MobiDB-lite"
);

CREATE INDEX idx_ft_protein ON protein_feature (protein_id);
CREATE INDEX idx_ft_type ON protein_feature (feature_type);

-- Filter: "has SIGNAL peptide", "has DOMAIN"
CREATE INDEX idx_ft_type_protein ON protein_feature (feature_type, protein_id);
```

> **Used by:** Gene Detail page (features tab), "feature exists" filter.

---

## 8. `host_organism` — Virus Host Organisms

Maps to `OH` lines (only present for virus proteins).

```sql
CREATE TABLE host_organism
(
    id SERIAL PRIMARY KEY,
    protein_id BIGINT  NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    taxid      INTEGER NOT NULL,
    name       TEXT    NOT NULL
);

CREATE INDEX idx_ho_protein ON host_organism (protein_id);
CREATE INDEX idx_ho_taxid ON host_organism (taxid);
```

---

## 9. `protein_comment` — Functional Comments

Maps to `CC -!-` lines: FUNCTION, CATALYTIC ACTIVITY, PATHWAY, SUBCELLULAR LOCATION…

```sql
CREATE TABLE protein_comment
(
    id           BIGSERIAL PRIMARY KEY,
    protein_id   BIGINT      NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    comment_type VARCHAR(50) NOT NULL, -- FUNCTION | CATALYTIC ACTIVITY | PATHWAY …
    text         TEXT        NOT NULL
);

CREATE INDEX idx_cc_protein ON protein_comment (protein_id);
CREATE INDEX idx_cc_type ON protein_comment (comment_type);
```

> **Used by:** Gene Detail page (summary tab).

---

## 10. `protein_publication` — Scientific References

Maps to `RN/RP/RX/RA/RT/RL` lines.

```sql
CREATE TABLE protein_publication
(
    id         BIGSERIAL PRIMARY KEY,
    protein_id BIGINT NOT NULL REFERENCES protein_entry (id) ON DELETE CASCADE,
    ref_number SMALLINT,     -- RN [1], [2] …
    pubmed_id  VARCHAR(20),  -- RX PubMed=15165820
    doi        VARCHAR(200), -- RX DOI=...
    authors    TEXT,         -- RA
    title      TEXT,         -- RT
    journal    VARCHAR(300)  -- RL
);

CREATE INDEX idx_pub_protein ON protein_publication (protein_id);
CREATE INDEX idx_pub_pubmed ON protein_publication (pubmed_id);
```

> **Used by:** Gene Detail page (publications tab).

---

## 11. Legacy Materialized Views (transition only)

The following views document the existing legacy analytics read model. They are not the target cross-service
architecture: during P6 shadow mode they remain available for parity comparison, then legacy refresh/read paths are
disabled after sign-off and removed in P8. The target analytics read model is described in §11.7 and does not query
`gene_data` directly.

### 11.1 `mv_length_histogram`

```sql
CREATE
MATERIALIZED VIEW mv_length_histogram AS
SELECT width_bucket(length, 0, 10000, 100)             AS bucket,
       (width_bucket(length, 0, 10000, 100) - 1) * 100 AS range_min,
       width_bucket(length, 0, 10000, 100) * 100 - 1   AS range_max,
       COUNT(*) AS count
FROM protein_entry
GROUP BY bucket
ORDER BY bucket;

CREATE UNIQUE INDEX ON mv_length_histogram (bucket);
```

> Feeds: **Protein Length Histogram** (Analytics page).

---

### 11.2 `mv_organism_counts`

```sql
CREATE
MATERIALIZED VIEW mv_organism_counts AS
SELECT organism_name,
       taxid,
       COUNT(*) AS total,
       COUNT(*)    FILTER (WHERE reviewed = TRUE)         AS reviewed_count, COUNT(*) FILTER (WHERE reviewed = FALSE)        AS unreviewed_count, ROUND(AVG(length)) AS avg_length
FROM protein_entry
GROUP BY organism_name, taxid
ORDER BY total DESC;

CREATE INDEX ON mv_organism_counts (organism_name);
CREATE INDEX ON mv_organism_counts (total DESC);
```

> Feeds: **Proteins by Organism** bar chart, top organisms KPI.

---

### 11.3 `mv_reviewed_ratio`

```sql
CREATE
MATERIALIZED VIEW mv_reviewed_ratio AS
SELECT reviewed,
       COUNT(*) AS count
FROM protein_entry
GROUP BY reviewed;

CREATE UNIQUE INDEX ON mv_reviewed_ratio (reviewed);
```

> Feeds: **Reviewed vs Unreviewed** pie chart, Dashboard KPI.

---

### 11.4 `mv_evidence_distribution`

```sql
CREATE
MATERIALIZED VIEW mv_evidence_distribution AS
SELECT evidence_level,
       CASE evidence_level
           WHEN 1 THEN 'Protein level'
           WHEN 2 THEN 'Transcript level'
           WHEN 3 THEN 'Homology'
           WHEN 4 THEN 'Predicted'
           WHEN 5 THEN 'Uncertain'
           END AS label,
       COUNT(*) AS count
FROM protein_entry
GROUP BY evidence_level
ORDER BY evidence_level;

CREATE UNIQUE INDEX ON mv_evidence_distribution (evidence_level);
```

> Feeds: **Evidence Level Pie Chart** (Dashboard).

---

### 11.5 `mv_keyword_frequency`

```sql
CREATE
MATERIALIZED VIEW mv_keyword_frequency AS
SELECT k.name AS keyword,
       COUNT(pk.protein_id) AS count
FROM keyword k
    JOIN protein_keyword pk
ON pk.keyword_id = k.id
GROUP BY k.name
ORDER BY count DESC;

CREATE INDEX ON mv_keyword_frequency (count DESC);
```

> Feeds: **Keyword Frequency** word-cloud / bar chart (Analytics page), keyword filter suggestions.

---

### 11.6 `mv_dashboard_kpis`

```sql
CREATE
MATERIALIZED VIEW mv_dashboard_kpis AS
SELECT COUNT(*)                     AS total_proteins,
       COUNT(*)                        FILTER (WHERE reviewed = TRUE)     AS reviewed_count, COUNT(DISTINCT organism_name) AS organism_count,
       COUNT(DISTINCT taxid)        AS taxon_count,
       ROUND(AVG(length))           AS avg_length,
       ROUND(AVG(molecular_weight)) AS avg_molecular_weight,
       MIN(length)                  AS min_length,
       MAX(length)                  AS max_length
FROM protein_entry;
```

> Feeds: all top KPI cards on the **Dashboard**.

---

### Refresh strategy

This direct catalog refresh is legacy behavior only. The target flow is completion event -> gene-service statistics
API -> analytics-owned staged aggregate results -> atomic publication.

```sql
-- Called by the Spring Batch import job after a full import completes
REFRESH
MATERIALIZED VIEW CONCURRENTLY mv_length_histogram;
REFRESH
MATERIALIZED VIEW CONCURRENTLY mv_organism_counts;
REFRESH
MATERIALIZED VIEW CONCURRENTLY mv_reviewed_ratio;
REFRESH
MATERIALIZED VIEW CONCURRENTLY mv_evidence_distribution;
REFRESH
MATERIALIZED VIEW CONCURRENTLY mv_keyword_frequency;
REFRESH
MATERIALIZED VIEW CONCURRENTLY mv_dashboard_kpis;
```

> Use `CONCURRENTLY` so reads are never blocked during refresh.

### 11.7 API-backed analytics aggregate and refresh state (target)

Analytics persists only aggregate results returned by `POST /internal/v1/protein-entries/statistics`; no catalog
entity, per-protein snapshot, or cross-schema view is introduced. All of the following objects belong to the
`analytics` schema and are inaccessible to gene-service:

| Logical persistence component | Data and invariant |
|--------------------------------|--------------------|
| Aggregate staging set | Complete result rows for an unpublished generation. A failed or partial fetch never replaces the published generation. |
| Published aggregate set | The last complete KPI, distribution, and related aggregate results read by dashboard views/endpoints. |
| Refresh metadata | Published generation ID, source generation timestamp, last successful publication, pending/failed state, and last error. |
| Durable refresh requests | Completion event/ingestion identity, deduplication state, attempts, next retry time, and failure details. Persist before acknowledging Kafka; duplicate markers may coalesce safely. |

Publication swaps the complete staged set and its metadata in one analytics-local transaction. Keep the prior
published set available until commit. A marker received during a fetch remains pending for a subsequent fetch;
restarts, retries, and concurrent service instances must not lose requests or publish stale partial results. The
physical table names and aggregate row shape are finalized with the P6 statistics contract and migrations.

---

## 12. Filter ↔ Column Mapping

This table is the reference for building **JPA Specifications** in `GeneSpecification.java`.

| UI Filter              | Table                         | Column / Join                               | Query Pattern                      |
|------------------------|-------------------------------|---------------------------------------------|------------------------------------|
| Accession contains     | `protein_entry`               | `accession`                                 | `ILIKE '%x%'`                      |
| Entry name             | `protein_entry`               | `entry_name`                                | `ILIKE '%x%'`                      |
| Gene name              | `protein_entry`               | `gene_name_primary` or `gene_name_synonyms` | `= x` or `ANY(gene_name_synonyms)` |
| Protein name           | `protein_entry`               | `protein_full_name`                         | `ILIKE '%x%'`                      |
| Reviewed               | `protein_entry`               | `reviewed`                                  | `= true/false`                     |
| Organism               | `protein_entry`               | `organism_name`                             | `ILIKE '%x%'`                      |
| Tax ID                 | `protein_entry`               | `taxid`                                     | `= x`                              |
| Lineage (kingdom)      | `protein_entry`               | `lineage`                                   | `'Bacteria' = ANY(lineage)`        |
| Length range           | `protein_entry`               | `length`                                    | `BETWEEN min AND max`              |
| Molecular weight range | `protein_entry`               | `molecular_weight`                          | `BETWEEN min AND max`              |
| Evidence level         | `protein_entry`               | `evidence_level`                            | `= x` or `IN (...)`                |
| Keyword                | `protein_keyword` + `keyword` | `keyword.name`                              | `JOIN + ILIKE`                     |
| GO term                | `protein_go_term` + `go_term` | `go_term.go_id` or `description`            | `JOIN + =`                         |
| GO aspect              | `protein_go_term` + `go_term` | `go_term.aspect`                            | `JOIN + = 'P'/'F'/'C'`             |
| Feature exists         | `protein_feature`             | `feature_type`                              | `EXISTS (subquery)`                |
| Has cross-ref source   | `cross_reference`             | `source`                                    | `EXISTS (subquery)`                |
| Global search          | `protein_entry`               | `search_vector`                             | `@@ plainto_tsquery(x)`            |

---

## 13. UniProt `.dat` Tag → Column Mapping

Quick reference for the ETL / Spring Batch `ItemProcessor`.

| `.dat` Tag              | Column(s)                                                                               |
|-------------------------|-----------------------------------------------------------------------------------------|
| `ID`                    | `entry_name`, `reviewed`, `length`                                                      |
| `AC`                    | `accession` (first value = primary)                                                     |
| `DT`                    | `integrated_date`, `sequence_date`, `updated_date`, `entry_version`, `sequence_version` |
| `DE RecName: Full=`     | `protein_full_name`                                                                     |
| `DE RecName: Short=`    | `protein_short_name`                                                                    |
| `DE EC=`                | `protein_ec_number`                                                                     |
| `GN Name=`              | `gene_name_primary`                                                                     |
| `GN Synonyms=`          | `gene_name_synonyms[]`                                                                  |
| `GN ORFNames=`          | `gene_orf_names[]`                                                                      |
| `GN OrderedLocusNames=` | `gene_ordered_locus[]`                                                                  |
| `OS`                    | `organism_name`, `organism_common_name`                                                 |
| `OC`                    | `lineage[]`                                                                             |
| `OX NCBI_TaxID=`        | `taxid`                                                                                 |
| `OH NCBI_TaxID=`        | `host_organism.taxid`, `host_organism.name`                                             |
| `CC -!-`                | `protein_comment.comment_type`, `.text`                                                 |
| `DR GO;`                | `go_term.go_id`, `.aspect`, `.description` → `protein_go_term`                          |
| `DR <other>;`           | `cross_reference.source`, `.identifier`, `.secondary_id`                                |
| `PE`                    | `evidence_level`                                                                        |
| `KW`                    | `keyword.name` → `protein_keyword`                                                      |
| `FT`                    | `protein_feature.feature_type`, `.start_pos`, `.end_pos`, `.note`, `.feature_id`        |
| `SQ`                    | `length`, `molecular_weight`, `sequence_checksum`                                       |
| sequence lines          | `sequence`                                                                              |
| `RN/RP/RX/RA/RT/RL`     | `protein_publication.*`                                                                 |

---

## 14. Historical Base DDL Summary (ordered by dependency)

```sql
-- 1. Vocabulary tables (no FKs)
keyword
go_term

-- 2. Core entity
protein_entry

-- 3. One-to-many children
protein_feature
protein_comment
protein_publication
host_organism
cross_reference

-- 4. Many-to-many join tables
protein_keyword
protein_go_term

-- 5. Legacy materialized views (transition only; not the target analytics read model)
mv_dashboard_kpis
mv_reviewed_ratio
mv_evidence_distribution
mv_length_histogram
mv_organism_counts
mv_keyword_frequency
```

The ARCH-002 target catalog additions are `protein_ingestion`, `catalog_outbox`, `saved_filter`, and the traceability
columns on `gene_data.protein_entry`. Analytics aggregate staging/publication/refresh state is stored in
`analytics`; audit persistence is stored separately in `audit_log` and is not included in this catalog DDL summary.

The gene-service Flyway scripts live in:

```text
backend/services/gene-service/src/main/resources/db/migration/
  V1__create_protein_entry.sql
  V2__create_keyword_tables.sql
  V3__create_go_term_tables.sql
  V4__create_cross_reference.sql
  V5__create_protein_feature.sql
  V6__create_host_organism.sql
  V7__create_protein_comment.sql
  V8__create_protein_publication.sql
  V9__create_materialized_views.sql
  V10__create_search_vector_trigger.sql
```

Managed by **Flyway** (already in Spring Boot project structure).

The ingestion ledger is catalog-owned by gene-service. Its ID is the external ingestion identifier (stored as text);
the record includes source service, source type (`FILE_DAT`, `FILE_TSV`, `UNIPROT_API`), initiating user, status,
created/updated/unchanged/stale/rejected counters, and start/completion timestamps. `last_ingestion_id` on
`protein_entry` refers to this ID; it is updated only when a merge materially changes that entry.

The target outbox DDL is summarized below:

```sql
CREATE TABLE gene_data.catalog_outbox (
    id                  BIGSERIAL PRIMARY KEY,
    aggregate_type      TEXT NOT NULL,
    aggregate_id        TEXT NOT NULL,
    aggregate_version   BIGINT,
    event_type          TEXT NOT NULL,
    topic               TEXT NOT NULL,
    message_key         TEXT NOT NULL,
    payload             JSONB NOT NULL,
    correlation_id      TEXT,
    occurred_at         TIMESTAMPTZ NOT NULL,
    published_at        TIMESTAMPTZ,
    attempts            INTEGER NOT NULL DEFAULT 0,
    last_error          TEXT
);

CREATE INDEX idx_catalog_outbox_unpublished
    ON gene_data.catalog_outbox (id)
    WHERE published_at IS NULL;
```

One transaction commits each ingestion chunk's catalog changes and entry-change outbox rows. Ingestion completion
and its completion marker are committed together after all chunks succeed. The relay publishes at least once and
sets `published_at` only after broker acknowledgement; consumers deduplicate by event ID. Catalog deletion and
tombstones are not supported.
