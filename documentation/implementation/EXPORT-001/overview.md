# EXPORT-001 — CSV & Chart Image Export

## Description

Implement the export features:

- **CSV export** — `POST /api/genes/export-csv` streams all filtered rows to a downloadable CSV file (no pagination, max
  100,000 rows, hard cap enforced server-side).
- **Chart image export** — client-side PNG download of any chart on the Analytics page (minimum 1200×600 px, chart title
  used as filename).

## Scope

| Layer              | Artifact                                                                         |
|--------------------|----------------------------------------------------------------------------------|
| Backend service    | `CsvExportService` — `StreamingResponseBody` to avoid loading all rows in memory |
| Backend controller | `GeneController.exportCsv()` — already stubbed                                   |
| Frontend           | Chart "Export PNG" button using chart library's built-in export API              |
| Frontend           | "Export CSV" button in `GenesPageComponent`; disabled when no data               |

## Acceptance Criteria

- [ ] `POST /api/genes/export-csv` with active filters returns `200` with `Content-Type: text/csv`,
  `Content-Disposition: attachment; filename="proteins_YYYY-MM-DD.csv"`.
- [ ] CSV columns and order match:
  `id, accession, entryName, proteinFullName, geneNamePrimary, organismName, taxid, reviewed, length, molecularWeight, evidenceLevel, keywords`.
- [ ] All filtered rows are included (not just the current page).
- [ ] If filtered result exceeds 100,000 rows, the server returns `413` with a message to refine the filter.
- [ ] Clicking "Export CSV" when filter yields 0 results: button is disabled, tooltip reads "No data to export".
- [ ] Each chart on the Analytics page has an "Export PNG" icon.
- [ ] Clicking "Export PNG" downloads a PNG at ≥ 1200×600 px with chart title as filename.
- [ ] All endpoints return `401` without JWT.
- [ ] Unit tests for `CsvExportService`.

## References

- `documentation/api-contract.md` §1 — `POST /api/genes/export-csv`
- `documentation/plan.md` — US-18, US-19
