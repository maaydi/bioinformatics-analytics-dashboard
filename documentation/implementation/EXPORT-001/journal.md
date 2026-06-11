# EXPORT-001 — Implementation Journal

---

## 2026-05-12

### Ticket created

- Created `overview.md` and `plan.md` from backlog stories US-18, US-19.
- Reviewed existing backend: `GeneController.exportCsv()` is stubbed with `UnsupportedOperationException`.
  `CsvExportService` is absent.
- Frontend: "Export CSV" button and chart "Export PNG" buttons are absent from current component stubs.
- Depends on `GENE-001` (backend) and `ANALYTICS-001` / `DASH-001` (for chart export) being implemented.
- Implementation not yet started.

## 2026-06-11

### Branch work (summarised from commits)

- Implemented frontend "Export CSV" button in `GenesPageComponent` and `ActiveFiltersComponent`; wired click handler to
  download a Blob and added a loading indicator while download is in progress.
- Added client-side handling for `413 Export limit exceeded` and show a user-friendly toast message when the server
  rejects large exports.
- Implemented server-side export endpoint: `POST /api/genes/export-csv` in `GeneController` — delegates to `GeneService`
  which asserts row cap then streams rows to the response writer.
- Added CSV serialization support (`CsvSerializable` + `CsvWriter`) and `ProteinSummaryDto` implements CSV row
  generation.
- Added chart "Export PNG" buttons across analytics components, centralized notification usage and adapted exported
  filenames to use chart titles.
- Added analytics improvements: Length vs Molecular Weight scatter chart, filter propagation to analytics, and unit
  tests for analytics components/services.




