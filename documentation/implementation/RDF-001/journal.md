# RDF-001 — Implementation Journal

---

## 2026-07-03 — Ticket Opened

**Commit**: `b5dec77`  
Initial documentation created: `overview.md`, `plan.md`, `journal.md`.  
`documentation/implementation/README.md` updated with RDF-001 entry.

---

## 2026-07-04 — UniProtKB REST Infrastructure

**Commit `c278950` — Create UniprotKB protein response**  
Created the full set of `record` DTOs matching the UniProtKB JSON schema:
`UniProtEntry`, `UniprotKbResponse`, `Organism`, `Gene`, `GeneName`, `Sequence`, `Feature`,
`FeatureLocation`, `StartEnd`, `Keyword`, `Comment`, `Reference`, `Citation`,
`CitationCrossReference`, `UniProtKBCrossReference`, `CrossReferenceProperty`, `Evidence`,
`EntryAudit`, `ProteinDescription`, `RecommendedName`, `FullName`, `Disease`,
`SubcellularLocation`, `Interaction`, `Interactant`, `AlternativeSequence`, `Text`, `Location`,
`Topology`, `DiseaseCrossReference`, `FeatureCrossReference`, `ExtraAttributes`,
`CountByFeatureType`, `CountByCommentType`.

**Commit `79a5410` — Create UniprotKB RestClient**  
`UniprotRestClientConfig` bean added (Jackson + `JdkClientHttpRequestFactory`, 1 h timeout,
base URL `https://rest.uniprot.org/uniprotkb`).  
`UniprotKbRestService` created with `search(pageSize)` and `searchAll(pageSize, cursor)`.  
`pom.xml` updated with required dependencies.

---

## 2026-07-06 — Analytics + Facets

**Commit `9585703` — Facet response from uniprot**  
Added `Facet`, `FacetValue`, `Facets` DTOs for faceted search results.

**Commit `2789554` — Config uniprot endpoint + analytics**  
`AbstractUniprotKbProvider` created (provider name `"uniprotKb"`).  
`UniprotKbAnalyticsService` created — queries UniProt facet endpoints to derive dashboard KPIs
(reviewed/unreviewed counts, organism distribution, annotation score distribution).

**Commit `dcded2b` — Define Analytics KPIs from uniprot endpoint**  
Added `documentation/implementation/RDF-001/data-api.md` — full reference of UniProt REST API
endpoints used and their response structures.

---

## 2026-07-08 — Spring Batch API Loader

**Commit `8f36f46` — Fix Gene DTO**  
Added missing `orfNames` field to `Gene` record.

**Commit `5f7c27d` — Mapper UniProtKB api to ProteinEntry Postgres**  
`UniProtEntryMapper` created (413 lines) — maps `UniProtEntry` to fully populated `ProteinEntry`
JPA aggregate including keywords, features, GO terms (from cross-references), cross-references,
comments (polymorphic), and publications.

**Commit `79f313f` — Create Api Client for UniprotKB**  
`UniProtKbApiClient` created — handles cursor-based pagination with `Link` header parsing.  
`UniProtApiClient` interface added.  
`UniprotKbRestService` extended with filtered search support.  
Removed temporary `SearchController` stub.

**Commit `63bd543` — Add Job to load data from api**  
Full Spring Batch API loader job created:

- `UniProtApiImportJobConfig` — job definition with chunk-oriented step (reader → processor → writer)
- `UniProtApiImportJobExecutor` — async launcher
- `UniProtApiItemReader` — cursor-paged `ItemReader<ProteinEntry>`
- `UniProtApiPage` — page model for API pagination
- `UniProtApiEntryProcessor` — `ItemProcessor` using `UniProtEntryMapper`

**Commit `cd79fca` — Fix GoTerm relation + GoTermResolver**  
`GoTermResolver` created — find-or-create semantics for `GoTerm` entities during batch write.  
`GoTermRepository` created.  
`ProteinEntry.goTerms` relationship fixed (`@ManyToMany` with cascade).

**Commit `1a3715d` — Add app start runnable for auto-start/recovery**  
`RunningJobFailureMarker` updated to mark stale `RUNNING` API import jobs as `FAILED` on startup.  
`Constants` updated with API job name constant.  
`AppProperties` extended; `application.yml` updated with new properties.

**Commit `7d405a9` — Refactor batch → job package**  
All classes moved from `com.bioinformatics.dashboard.batch` to `com.bioinformatics.dashboard.job`.  
New sub-package layout:

```
job/
  uniprot/
    apiloader/   — API-based Spring Batch job
    fileloader/  — File-based Spring Batch job
  listener/      — Chunk + job listeners
  resolver/      — GoTermResolver, KeywordResolver, ProteinAccessionResolver
  service/       — MaterializedViewRefreshService, ViewRefreshAlertService
  writer/        — ProteinAggregateItemWriter
  dto/           — RefreshResult, ViewToRefresh
```

**Commit `8d36070` — Configure APP_DIR**  
`CacheFolderInitializer` created — creates temp import directory at application startup.  
`APP_DIR` property added to `application.yml` and `.env.example`.

---

## 2026-07-09 — Service Integration + Tests

**Commit `0ef76f8` — Refactor UniProtEntryMapper**  
Mapper refactored for clarity: extracted helper methods, improved null-safety, better separation
between organism / gene name / cross-reference extraction logic.

**Commit `5d9c1e0` — Make remote api import run from service**  
`ImportService.triggerRemoteImport()` implemented — creates `ImportJob` record and launches the
API loader job asynchronously.  
`ImportController` extended: `POST /api/admin/import/uniprot/remote` endpoint added.  
`documentation/api-contract.md` updated with new endpoint schema.  
`ImportServiceTest` + `ImportControllerIntegrationTest` extended for remote import success + error
cases.

---

## 2026-07-10 — Documentation Sync

Ticket scope clarification: original plan (eliminate `import_job` table) was never executed.
Actual scope was the UniProt REST API import pipeline.  
`plan.md` rewritten to reflect actual delivered work.  
`journal.md` (this file) backfilled with chronological commit log.

**Ticket RDF-001: DONE** — filter-driven `GeneService` implementation deferred to **REMOTE-001**.
