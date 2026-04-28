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

## Suggested Ticket IDs (MVP Roadmap)

| ID | Scope |
|---|---|
| `AUTH-001` | DB schema (V1 migration) + JWT auth endpoints |
| `IMPORT-001` | Spring Batch import pipeline + ImportController |
| `GENE-001` | GeneController + GeneService + GeneSpecification |
| `GENE-002` | GeneFilter Angular component + reactive form |
| `GENE-003` | GenesTable AG Grid component |
| `DETAIL-001` | Gene Detail page (all tabs) |
| `ANALYTICS-001` | Analytics endpoints + materialized views |
| `DASH-001` | Dashboard page (KPI cards + charts) |
| `FILTER-001` | Saved filter feature (backend + frontend) |
| `EXPORT-001` | CSV export (backend streaming + frontend trigger) |
