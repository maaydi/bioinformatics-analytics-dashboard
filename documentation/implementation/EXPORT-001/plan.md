# EXPORT-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Implement `CsvExportService` (streaming, row cap enforcement)
3. Complete `GeneController.exportCsv()` (remove stub, wire service)
4. Implement "Export CSV" button in `GenesPageComponent`
5. Implement chart "Export PNG" buttons in `AnalyticsComponent` / `DashboardComponent`
6. Write unit tests for `CsvExportService`
7. Write integration test for export endpoint
8. Update documentation

## Status

- [x] Requirements analyzed
- [x] CsvExportService implemented
- [x] GeneController.exportCsv() completed
- [x] Export CSV button implemented in frontend
- [x] Export PNG buttons implemented
- [x] Unit tests written
- [x] Integration test written
- [x] Documentation updated
- [x] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Backend — `CsvExportService`

- [x] `exportCsv(GeneSearchRequest request, HttpServletResponse response)`:
  - [x] Set response headers: `Content-Type: text/csv`,
      `Content-Disposition: attachment; filename="proteins_<date>.csv"`
  - [x] Count matching rows first; if > 100,000 throw `ExportRowCapExceededException` → 413
  - [x] Write CSV header row:
      `id,accession,entryName,proteinFullName,geneNamePrimary,organismName,taxid,reviewed,length,molecularWeight,evidenceLevel,keywords`
  - [x] Escape commas and quotes in all string fields (RFC 4180 compliant)
  - [x] Serialize `keywords` as pipe-separated string within the cell
- [x] `ExportRowCapExceededException` — message: "Export limit exceeded. Result contains N rows; maximum is 100,000.
  Please refine your filter."

### Backend — Controller

- [x] Remove `UnsupportedOperationException` stub in `GeneController.exportCsv()`
- [x] Inject `CsvExportService`; delegate call
- [x] Global exception handler maps `ExportRowCapExceededException → 413`

### Backend — Tests

- [x] `CsvExportServiceTest` — unit:
  - [x] Correct CSV headers in output
  - [x] Keywords serialized as pipe-separated
  - [x] Special characters (comma, quote) properly escaped
  - [x] Throws `ExportRowCapExceededException` when row count > 100,000
- [x] `GeneControllerIntegrationTest` (extend existing):
  - [x] `POST /api/genes/export-csv` with no filters returns 200 CSV
  - [x] `POST /api/genes/export-csv` over row cap returns 413

### Frontend — "Export CSV" button (`features/genes/genes-page/`)

- [x] Add "Export CSV" button to `GenesPageComponent`
- [x] Button `[disabled]` binding: `filtersStore().totalElements === 0`
- [x] Tooltip `"No data to export"` when disabled (using `title` attribute for simplicity)
- [x] Click handler: call `GenesService.exportCsv(currentFilters)`, trigger browser download via `<a>` element +
  `URL.createObjectURL(blob)`
- [x] Show loading indicator on button during download
- [x] Handle 413 error: show toast "Export limit exceeded. Please refine your filter."

### Frontend — "Export PNG" buttons (charts)

- [x] Each chart component exposes a `downloadPng()` method using the chart library's API (e.g.,
  `echartsInstance.getDataURL({ type: 'png', pixelRatio: 2 })`)
- [x] Minimum resolution: `pixelRatio: 2` (ensures ≥ 1200×600 on a 600×300 canvas)
- [x] Filename derived from chart title (e.g., `length_histogram.png`)
- [x] Export icon button added to each chart card header (accessible, aria-label "Export chart as PNG")

### General

- [x] CSV is RFC 4180 compliant
- [x] Code reviewed
- [ ] Coverage ≥ 80%
