# RDF-001 — Implementation Plan

> **Scope correction (2026-07-10):** The original plan described eliminating the `import_job`
> table in favour of Spring Batch's native `JobRepository`. The actual work delivered was a full
> **UniProtKB REST API import pipeline** (Spring Batch job reading live data from
> `https://rest.uniprot.org/uniprotkb`) plus a public `POST /api/admin/import/uniprot/remote`
> endpoint. The `ImportJob` entity is unchanged. The table-elimination was deferred/cancelled.

## Delivered Goal

Enable data import directly from the UniProt REST API without requiring a local `.dat` / `.tsv`
file, accessible via `POST /api/admin/import/uniprot/remote`.

---

## Task Breakdown

| Task | Description                                                                                                                                    | Status                       |
|------|------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|
| T-01 | UniProtKB REST DTOs (`UniProtEntry`, `Organism`, `Gene`, `Sequence`, `Feature`, `Comment`, `Keyword`, `Facet`, …)                              | ✅ Done                      |
| T-02 | `UniprotRestClientConfig` — `RestClient` bean, JDK HTTP client, 1 h read timeout                                                               | ✅ Done                      |
| T-03 | `UniprotKbRestService` — `search(size)` / `searchAll(size, cursor)` wrappers                                                                   | ✅ Done                      |
| T-04 | `UniProtProteinDtoMapper` — `UniProtEntry` → `ProteinEntry` JPA aggregate                                                                      | ✅ Done                      |
| T-05 | `UniProtApiItemReader` + `UniProtKbApiClient` + `UniProtApiPage` — cursor-paged Spring Batch reader                                            | ✅ Done                      |
| T-06 | `UniProtApiEntryProcessor` — maps API page items via `UniProtProteinDtoMapper` + resolvers                                                     | ✅ Done                      |
| T-07 | `GoTermResolver` / `KeywordResolver` / `ProteinAccessionResolver` — find-or-create entity resolvers                                            | ✅ Done                      |
| T-08 | `UniProtApiImportJobConfig` + `UniProtApiImportJobExecutor` — Spring Batch job + async launcher                                                | ✅ Done                      |
| T-09 | Refactor: rename `batch` package → `job`; organise sub-packages (`fileloader/`, `apiloader/`, `listener/`, `resolver/`, `service/`, `writer/`) | ✅ Done                      |
| T-10 | `UniprotKbAnalyticsService` — Analytics KPIs via UniProt facet endpoints                                                                       | ✅ Done                      |
| T-11 | `ImportService.triggerRemoteImport()` + `POST /api/admin/import/uniprot/remote` controller endpoint                                            | ✅ Done                      |
| T-12 | `CacheFolderInitializer` + `APP_DIR` property in `application.yml` + `.env.example`                                                            | ✅ Done                      |
| T-13 | `RunningJobFailureMarker` updated to cover the API import job on startup recovery                                                              | ✅ Done                      |
| T-14 | `ImportServiceTest` + `ImportControllerIntegrationTest` extended for remote import                                                             | ✅ Done                      |
| T-15 | `UniprotQueryBuilder` + `UniprotKbGeneMapper` + `UniprotKbGeneService` (filter-driven search)                                                  | ⬜ Deferred → **REMOTE-001** |

---

## Ticket Status: ✅ DONE (T-15 formally tracked under REMOTE-001)
