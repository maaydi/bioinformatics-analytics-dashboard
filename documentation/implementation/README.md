# Implementation Journals

This folder contains per-ticket implementation artifacts, created as development progresses.

## Workflow (from documentation/constitution.md)

When starting a ticket, create a subfolder `<Ticket-ID>/` containing:

| File | Purpose |
|---|---|
| `overview.md` | Ticket description and acceptance criteria (copied from backlog) |
| `journal.md` | Chronological log of actions: date, action taken, outcome |
| `analyse.md` | Ambiguity analysis — if any exist, add this file and wait for clarification |
| `plan.md` | Task breakdown with status tracking (not-started / in-progress / done) |

## Example structure

```
documentation/implementation/
├── GENE-001/
│   ├── overview.md
│   ├── journal.md
│   ├── plan.md
│   └── analyse.md    ← only if ambiguities exist
├── IMPORT-001/
│   ├── overview.md
│   ├── journal.md
│   └── plan.md
└── AUTH-001/
    ├── overview.md
    ├── journal.md
    └── plan.md
```

## Ticket Catalog (Planned)

| ID              | Scope                                                                        | Status        |
|-----------------|------------------------------------------------------------------------------|---------------|
| `AUTH-001`      | DB schema (V1 migration) + JWT auth endpoints                                | `done`        |
| `IMPORT-001`    | Spring Batch import pipeline + ImportController                              | `done`        |
| `GENE-001`      | GeneController + GeneService + GeneSpecification                             | `done`        |
| `GENE-002`      | GeneFilter Angular component + reactive form                                 | `in-progress` |
| `GENE-003`      | GenesTable AG Grid component                                                 | `not-started` |
| `DETAIL-001`    | Gene Detail page (all tabs)                                                  | `not-started` |
| `ANALYTICS-001` | Analytics endpoints + materialized views                                     | `not-started` |
| `DASH-001`      | Dashboard page (KPI cards + charts)                                          | `not-started` |
| `FILTER-001`    | Saved filter feature (backend + frontend)                                    | `not-started` |
| `EXPORT-001`    | CSV export (backend streaming + frontend trigger)                            | `not-started` |
| `COMPARE-001`   | Compare mode analytics (Set A vs Set B)                                      | `not-started` |
| `OPS-001`       | Audit log + health/readiness + rate limiting + pagination cap                | `not-started` |
| `A11Y-001`      | Accessibility, loading/error/empty state host, theme toggle, password change | `not-started` |

## Chronological Implementation Order (Recommended)

Implement tickets in this order so dependencies are respected and each increment is visible to end users.

| Order | Ticket          | Status        | Why Now                                                      | End-User Visible Result                                                            |
|-------|-----------------|---------------|--------------------------------------------------------------|------------------------------------------------------------------------------------|
| 1     | `AUTH-001`      | `done`        | Security baseline for protected routes and role-based access | Users can log in/out, refresh sessions, and access only authorized pages           |
| 2     | `IMPORT-001`    | `done`        | Data ingestion must exist before meaningful exploration      | Admin can upload UniProt files and monitor import jobs                             |
| 3     | `GENE-001`      | `done`        | Core backend APIs needed by all gene views                   | `/api/genes` list/search/detail/export endpoints become functional                 |
| 4     | `GENE-002`      | `in-progress` | Filter UX depends on search contract from `GENE-001`         | Users can apply complex filters and see active filter chips                        |
| 5     | `GENE-003`      | `not-started` | Table view consumes filters and list/search APIs             | Users can browse, sort, paginate, and open gene rows                               |
| 6     | `DETAIL-001`    | `not-started` | Detail page depends on gene detail endpoint                  | Users can inspect full protein details, sequence, annotations, and references      |
| 7     | `ANALYTICS-001` | `not-started` | Dashboard/charts require analytics data sources              | Analytics endpoints return KPI/histogram/ratio/evidence/keyword datasets           |
| 8     | `DASH-001`      | `not-started` | UI layer on top of analytics endpoints                       | Users see dashboard KPI cards and charts with fast load times                      |
| 9     | `FILTER-001`    | `not-started` | Saved work is useful once filters/table are operational      | Users can save, reload, and delete personal filter presets                         |
| 10    | `EXPORT-001`    | `not-started` | Export is meaningful after search/table/charts are stable    | Users can export filtered CSV and chart PNG images                                 |
| 11    | `COMPARE-001`   | `not-started` | Compare mode reuses filters + analytics foundation           | Users can compare two populations side by side                                     |
| 12    | `OPS-001`       | `not-started` | Hardening after core features are in place                   | Better reliability: health probes, throttling, auditability, safe page-size limits |
| 13    | `A11Y-001`      | `not-started` | Final polish/certification after features are complete       | Improved accessibility, theme toggle, consistent states, password update flow      |

## End-User Validation Milestones

Use these milestone checks after each phase to confirm user-visible progress.

1. **Authentication Ready** (`AUTH-001`): login works, protected routes redirect unauthenticated users, admin-only pages
   block standard users.
2. **Data Available** (`IMPORT-001`): admin import starts/completes and imported records appear in gene listing.
3. **Explorer Usable** (`GENE-001` + `GENE-002` + `GENE-003` + `DETAIL-001`): users can search/filter/sort/paginate and
   inspect a full gene detail page.
4. **Insights Usable** (`ANALYTICS-001` + `DASH-001`): dashboard charts and KPIs load correctly and reflect data.
5. **Power Features Ready** (`FILTER-001` + `EXPORT-001` + `COMPARE-001`): users can save filters, export results, and
   compare cohorts.
6. **Production Ready UX/Ops** (`OPS-001` + `A11Y-001`): platform is hardened and accessible with reliable runtime
   behavior.
