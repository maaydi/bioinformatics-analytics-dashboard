# Public API Snapshot Inventory

This inventory follows the current controller mappings. No response bodies are captured yet: the full service stack and a stable database fixture were unavailable during P0 baseline work. Capture raw status, headers, and body for each route with a fixed dataset and record auth/profile details; do not normalize dynamic IDs/timestamps without retaining the raw response too.

## Gene API

| Method | Path | Capture case |
|--------|------|--------------|
| `GET` | `/api/genes?page=0&size=10&sort=id&direction=asc` | Paged list with a deterministic seeded entry. |
| `POST` | `/api/genes/search` | Unfiltered request and one representative filtered request from `ac7-search-cases.json`. |
| `GET` | `/api/genes/{accession}` | Detail response for the seeded accession. |
| `POST` | `/api/genes/count` | Count for the same stable filter fixture. |
| `POST` | `/api/genes/export-csv` | Record the current response, including the current not-implemented failure; this route is not treated as a successful export contract. |

## Analytics API

| Method | Path | Capture case |
|--------|------|--------------|
| `GET` | `/api/v1/analytics/dashboard-kpis` | Unfiltered KPI DTO. |
| `GET` | `/api/v1/analytics/length-histogram` | Unfiltered histogram. |
| `GET` | `/api/v1/analytics/by-organism?limit=50` | Default-limit organism counts. |
| `GET` | `/api/v1/analytics/reviewed-ratio` | Reviewed/unreviewed distribution. |
| `GET` | `/api/v1/analytics/evidence-levels` | Evidence distribution. |
| `GET` | `/api/v1/analytics/keyword-frequency?limit=100` | Default-limit keyword frequencies. |
| `POST` | `/api/v1/analytics/filters/dashboard-kpis` | Filtered KPI DTO. |
| `POST` | `/api/v1/analytics/filters/length-histogram` | Filtered histogram. |
| `POST` | `/api/v1/analytics/filters/by-organism?limit=50` | Filtered organism counts. |
| `POST` | `/api/v1/analytics/filters/reviewed-ratio` | Filtered reviewed ratio. |
| `POST` | `/api/v1/analytics/filters/evidence-levels` | Filtered evidence distribution. |
| `POST` | `/api/v1/analytics/filters/keyword-frequency?limit=100` | Filtered keyword frequencies. |
| `POST` | `/api/v1/analytics/filters/length-weight` | Filtered length/weight aggregation. |
| `POST` | `/api/v1/analytics/filters/compare` | Comparison response for two deterministic filter sets. |

## Import Administration API

| Method | Path | Capture case |
|--------|------|--------------|
| `POST` | `/api/v1/admin/import/uniprot` | Accepted job summary for a deterministic minimal input; capture response before asynchronous job completion. |
| `POST` | `/api/v1/admin/import/uniprot/remote?filterId={id}` | Accepted remote-job summary using a seeded saved filter. |
| `GET` | `/api/v1/admin/import/status?page=0&size=20` | Paged import-job summaries. |
| `GET` | `/api/v1/admin/import/status/{jobId}` | Progress response for a deterministic completed and failed job. |

## Saved Filters

| Method | Path | Capture case |
|--------|------|--------------|
| `GET` | `/api/saved-filters` | Current user's saved-filter list. |
| `POST` | `/api/saved-filters` | Created filter response with a stable request body. |
| `GET` | `/api/saved-filters/{id}` | Saved-filter detail for the current user. |

Use the auth mechanism active at capture time. The current controller integration tests use `X-User-*` test headers and mock some downstream dependencies, so those test responses are not substitutes for complete public API goldens.
