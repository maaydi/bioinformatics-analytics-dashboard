# API Contract

> This document is the **authoritative specification** for all REST endpoints exposed by the Spring Boot backend.
> The frontend and backend must both conform to this contract. Any deviation is a defect.
>
> Base URL: `/api`  
> Content-Type: `application/json` (unless noted otherwise)  
> Authentication: `Authorization: Bearer <JWT>` on all protected endpoints (see authorization matrix in Overview.md §13)

---

## Shared Schemas

### `PagedResponse<T>`

Returned by all paginated list endpoints.

```json
{
  "content":       [ /* array of T */ ],
  "page":          0,
  "size":          50,
  "totalElements": 12345,
  "totalPages":    247
}
```

### `ErrorResponse`

```json
{
  "status":    422,
  "error":     "Unprocessable Entity",
  "message":   "Human-readable description",
  "timestamp": "2026-04-27T14:30:00Z"
}
```

### `ProteinSummary`

Used in paginated list responses.

```json
{
  "id":              1,
  "accession":       "Q6GZX4",
  "entryName":       "001R_FRG3G",
  "proteinFullName": "Putative transcription factor 001R",
  "geneNamePrimary": "FV3-001R",
  "organismName":    "Frog virus 3 (isolate Goorha)",
  "taxid":           654924,
  "reviewed":        false,
  "length":          256,
  "molecularWeight": 29735,
  "evidenceLevel":   4,
  "keywords":        ["Activator", "Transcription regulation"]
}
```

### `ProteinDetail`

Used in single-entry responses. Extends `ProteinSummary` with all relational data.

```json
{
  "id":                1,
  "accession":         "Q6GZX4",
  "entryName":         "001R_FRG3G",
  "proteinFullName":   "Putative transcription factor 001R",
  "proteinShortName":  null,
  "proteinEcNumber":   null,
  "geneNamePrimary":   "FV3-001R",
  "geneNameSynonyms":  [],
  "geneOrfNames":      ["FV3-001R"],
  "geneOrderedLocus":  [],
  "organismName":      "Frog virus 3 (isolate Goorha)",
  "organismCommonName": null,
  "taxid":             654924,
  "lineage":           ["Viruses", "Varidnaviria", "..."],
  "reviewed":          false,
  "integratedDate":    "2011-06-28",
  "sequenceDate":      "2004-07-19",
  "updatedDate":       "2026-01-28",
  "sequenceVersion":   1,
  "entryVersion":      46,
  "length":            256,
  "molecularWeight":   29735,
  "sequenceChecksum":  "B4840739BF7D4121",
  "sequence":          "MAFSAEDVLK...",
  "evidenceLevel":     4,
  "keywords":          ["Activator", "Transcription regulation"],
  "features":          [ /* FeatureItem[] */ ],
  "goTerms":           [ /* GoTermItem[] */ ],
  "crossReferences":   [ /* CrossReferenceItem[] */ ],
  "comments":          [ /* CommentItem[] */ ],
  "publications":      [ /* PublicationItem[] */ ],
  "hostOrganisms":     [ /* HostOrganismItem[] */ ]
}
```

### `FeatureItem`

```json
{
  "featureType": "CHAIN",
  "startPos": 1,
  "endPos": 256,
  "note": "Putative transcription factor 001R",
  "featureId": "PRO_0000410512"
}
```

### `GoTermItem`

```json
{ "goId": "GO:0046782", "aspect": "P", "description": "regulation of viral transcription", "evidenceCode": "IEA" }
```

### `CrossReferenceItem`

```json
{ "source": "EMBL", "identifier": "AY548484", "secondaryId": "AAT09660.1", "tertiaryInfo": "-" }
```

### `CommentItem`

```json
{
  "commentType": "FUNCTION",
  "text": "Transcription activation. {ECO:0000305}."
}
```

### `PublicationItem`

```json
{
  "refNumber": 1,
  "pubmedId":  "15165820",
  "doi":       "10.1016/j.virol.2004.02.019",
  "authors":   "Tan W.G., Barkman T.J., ...",
  "title":     "Comparative genomic analyses of frog virus 3...",
  "journal":   "Virology 323:70-84(2004)."
}
```

### `HostOrganismItem`

```json
{ "taxid": 30343, "name": "Dryophytes versicolor (chameleon treefrog)" }
```

---

## 1. Gene / Protein Endpoints

---

### `GET /api/genes`

List proteins with optional sorting and pagination.

**Authorization:** USER, ADMIN

**Query Parameters:**

| Name | Type | Default | Constraints | Description |
|---|---|---|---|---|
| `page` | integer | `0` | ≥ 0 | Zero-based page index |
| `size` | integer | `50` | 1–500 | Items per page |
| `sort` | string | `id` | See sortable fields | Column to sort by |
| `direction` | enum | `asc` | `asc`, `desc` | Sort direction |

**Sortable fields:** `id`, `accession`, `entryName`, `geneNamePrimary`, `proteinFullName`, `organismName`, `length`, `molecularWeight`, `evidenceLevel`, `reviewed`, `updatedDate`

**Response `200 OK`:**

```json
{
  "content":       [ /* ProteinSummary[] */ ],
  "page":          0,
  "size":          50,
  "totalElements": 570000,
  "totalPages":    11400
}
```

**Error Responses:**

| HTTP | Condition |
|---|---|
| `400` | `sort` value is not a sortable field |
| `401` | Missing or invalid JWT |

---

### `POST /api/genes/search`

Search and filter proteins with full filter support.

**Authorization:** USER, ADMIN

**Request Body:**

```json
{
  "globalSearch":       "string | null",
  "accession":          "string | null (max 20)",
  "entryName":          "string | null (max 50)",
  "geneNamePrimary":    "string | null (max 100)",
  "proteinFullName":    "string | null",
  "reviewed":           "boolean | null",
  "organism":           "string | null (max 300)",
  "taxid":              "integer | null (> 0)",
  "lineage":            "string | null",
  "lengthMin":          "integer | null (≥ 1)",
  "lengthMax":          "integer | null (≤ 100000)",
  "molecularWeightMin": "integer | null (≥ 1)",
  "molecularWeightMax": "integer | null",
  "evidenceLevels":     "integer[] | null (values 1–5)",
  "keywords":           "string[] | null",
  "goTermId":           "string | null (format GO:\\d{7})",
  "goAspect":           "enum P|F|C | null",
  "featureType":        "string | null",
  "crossRefSource":     "string | null",
  "page":               "integer (≥ 0, default 0)",
  "size":               "integer (1–500, default 50)",
  "sort":               "string (default id)",
  "direction":          "enum asc|desc (default asc)"
}
```

All filter fields are optional. Multiple fields are combined with AND logic.

**Response `200 OK`:** `PagedResponse<ProteinSummary>`

**Error Responses:**

| HTTP | Condition |
|---|---|
| `400` | `lengthMin > lengthMax` or `molecularWeightMin > molecularWeightMax` |
| `400` | `evidenceLevels` contains a value outside 1–5 |
| `400` | `goTermId` does not match `GO:\d{7}` |
| `400` | `goAspect` is not `P`, `F`, or `C` |
| `401` | Missing or invalid JWT |
| `422` | Request body fails schema validation |

---

### `GET /api/genes/{id}`

Retrieve the full detail of one protein entry.

**Authorization:** USER, ADMIN

**Path Parameters:**

| Name | Type | Description |
|---|---|---|
| `id` | long | Internal `protein_entry.id` |

**Response `200 OK`:** `ProteinDetail`

**Error Responses:**

| HTTP | Condition |
|---|---|
| `401` | Missing or invalid JWT |
| `404` | No `protein_entry` row with the given `id` |

---

### `POST /api/genes/export-csv`

Export all rows matching the given filter to a CSV file (no pagination — full result set).

**Authorization:** USER, ADMIN

**Request Body:** Same schema as `POST /api/genes/search` minus `page`, `size`, `sort`, `direction`.

**Response `200 OK`:**

```
Content-Type: text/csv
Content-Disposition: attachment; filename="proteins_2026-04-27.csv"
```

CSV columns match the `ProteinSummary` fields, in order:
`id, accession, entryName, proteinFullName, geneNamePrimary, organismName, taxid, reviewed, length, molecularWeight, evidenceLevel, keywords`

**Error Responses:**

| HTTP | Condition |
|---|---|
| `400` | Invalid filter values (same rules as search) |
| `401` | Missing or invalid JWT |
| `422` | Request body fails schema validation |

---

## 2. Analytics Endpoints

All analytics endpoints are served from pre-computed materialized views and return a complete result set (no pagination).

**Authorization (all analytics endpoints):** USER, ADMIN

---

### `GET /api/analytics/dashboard-kpis`

**Response `200 OK`:**

```json
{
  "totalProteins":     570000,
  "reviewedCount":     570196,
  "unreviewedCount":   195000000,
  "organismCount":     14822,
  "taxonCount":        14822,
  "avgLength":         360,
  "avgMolecularWeight": 40643,
  "minLength":         2,
  "maxLength":         35213
}
```

---

### `GET /api/analytics/length-histogram`

**Response `200 OK`:**

```json
[
  { "bucket": 1, "rangeMin": 0,   "rangeMax": 99,  "count": 12000 },
  { "bucket": 2, "rangeMin": 100, "rangeMax": 199, "count": 45000 }
]
```

---

### `GET /api/analytics/by-organism`

Returns the top 50 organisms by protein count.

**Query Parameters:**

| Name | Type | Default | Description |
|---|---|---|---|
| `limit` | integer | `50` | Maximum organisms to return (1–200) |

**Response `200 OK`:**

```json
[
  {
    "organismName":    "Homo sapiens (Human)",
    "taxid":          9606,
    "total":          20581,
    "reviewedCount":  20581,
    "unreviewedCount": 0,
    "avgLength":      480
  }
]
```

---

### `GET /api/analytics/reviewed-ratio`

**Response `200 OK`:**

```json
[
  { "reviewed": true,  "count": 570000 },
  { "reviewed": false, "count": 195000000 }
]
```

---

### `GET /api/analytics/evidence-levels`

**Response `200 OK`:**

```json
[
  { "evidenceLevel": 1, "label": "Protein level",    "count": 400000 },
  { "evidenceLevel": 2, "label": "Transcript level",  "count": 80000  },
  { "evidenceLevel": 3, "label": "Homology",          "count": 60000  },
  { "evidenceLevel": 4, "label": "Predicted",         "count": 25000  },
  { "evidenceLevel": 5, "label": "Uncertain",         "count": 5000   }
]
```

---

### `GET /api/analytics/keyword-frequency`

Returns the top 100 keywords by frequency.

**Query Parameters:**

| Name | Type | Default | Description |
|---|---|---|---|
| `limit` | integer | `100` | Maximum keywords to return (1–500) |

**Response `200 OK`:**

```json
[
  { "keyword": "Kinase",     "count": 18000 },
  { "keyword": "Activator",  "count": 12000 }
]
```

---

## 3. Import Admin Endpoints

**Authorization (all import endpoints):** ADMIN only

---

### `POST /api/admin/import/uniprot`

Trigger a UniProt import job.

**Request:** `multipart/form-data`

| Field | Type | Required | Description |
|---|---|---|---|
| `file` | binary | yes | `.dat` or `.tsv` file, max 2 GB |
| `strategy` | enum `OVERWRITE` | yes | Duplicate handling strategy (only `OVERWRITE` supported in v1) |

**Response `202 Accepted`:**

```json
{
  "jobId":     "a1b2c3d4-...",
  "status":    "RUNNING",
  "createdAt": "2026-04-27T14:00:00Z"
}
```

**Error Responses:**

| HTTP | Condition |
|---|---|
| `401` | Missing or invalid JWT |
| `403` | Authenticated user does not have ROLE_ADMIN |
| `409` | Another import job is already running |
| `413` | File exceeds 2 GB |
| `422` | Unsupported file type or strategy value |

---

### `POST /api/admin/import/uniprot/remote`

Trigger a remote UniProt API import job (no uploaded file).

**Request:** No body

**Response `202 Accepted`:**

```json
{
  "jobId": "a1b2c3d4-...",
  "status": "RUNNING",
  "fileName": "UNIPROT_API_REMOTE",
  "createdAt": "2026-04-27T14:00:00Z"
}
```

**Error Responses:**

| HTTP  | Condition                                   |
|-------|---------------------------------------------|
| `401` | Missing or invalid JWT                      |
| `403` | Authenticated user does not have ROLE_ADMIN |
| `409` | Another import job is already running       |

---

### `GET /api/admin/import/status`

List all import jobs (most recent first).

**Query Parameters:**

| Name | Type | Default | Description |
|---|---|---|---|
| `page` | integer | `0` | Page index |
| `size` | integer | `20` | Items per page (max 100) |

**Response `200 OK`:** `PagedResponse<ImportJobSummary>`

```json
{
  "id":             "a1b2c3d4-...",
  "status":         "COMPLETED",
  "fileName":       "uniprot_sprot.dat",
  "entryCount":     570000,
  "durationMs":     840000,
  "createdAt":      "2026-04-27T14:00:00Z",
  "completedAt":    "2026-04-27T14:14:00Z",
  "errorMessage":   null
}
```

---

### `GET /api/admin/import/status/{jobId}`

Get real-time status of a single import job. Used for polling every 5 seconds.

**Path Parameters:**

| Name | Type | Description |
|---|---|---|
| `jobId` | UUID string | Job identifier returned by POST |

**Response `200 OK`:**

```json
{
  "id":              "a1b2c3d4-...",
  "status":          "RUNNING",
  "fileName":        "uniprot_sprot.dat",
  "recordsProcessed": 200000,
  "totalEstimated":   570000,
  "progressPercent":  35,
  "elapsedMs":        300000,
  "errorMessage":     null
}
```

`status` values: `RUNNING`, `COMPLETED`, `FAILED`

**Error Responses:**

| HTTP | Condition |
|---|---|
| `401` | Missing or invalid JWT |
| `403` | Not ADMIN |
| `404` | No job with given `jobId` |

---

## 4. Saved Filters Endpoints

**Authorization:** USER, ADMIN (users may only access their own saved filters)

---

### `GET /api/saved-filters`

List saved filter sets for the authenticated user.

**Response `200 OK`:**

```json
[
  {
    "id":          1,
    "name":        "Human reviewed kinases",
    "filterJson":  { "organism": "Homo sapiens", "reviewed": true, "keywords": ["Kinase"] },
    "createdAt":   "2026-04-27T10:00:00Z"
  }
]
```

---

### `POST /api/saved-filters`

Save a new filter set.

**Request Body:**

```json
{
  "name":       "string (1–100 characters, required)",
  "filterJson": { /* same fields as POST /api/genes/search body */ }
}
```

**Response `201 Created`:** The created `SavedFilter` object.

**Error Responses:**

| HTTP | Condition |
|---|---|
| `400` | `name` is blank or exceeds 100 characters |
| `409` | A saved filter with the same name already exists for this user |
| `422` | `filterJson` fails validation |

---

### `DELETE /api/saved-filters/{id}`

Delete a saved filter set.

**Response `204 No Content`**

**Error Responses:**

| HTTP | Condition |
|---|---|
| `403` | The filter belongs to a different user (ADMIN may delete any) |
| `404` | No saved filter with the given `id` for this user |

---

## 5. Authentication Endpoints

---

### `POST /api/auth/login`

**Request Body:**

```json
{ "username": "string", "password": "string" }
```

**Response `200 OK`:**

```json
{
  "accessToken":  "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn":    3600,
  "tokenType":    "Bearer"
}
```

**Error Responses:**

| HTTP | Condition |
|---|---|
| `401` | Invalid credentials |

---

### `POST /api/auth/refresh`

**Request Body:**

```json
{ "refreshToken": "eyJ..." }
```

**Response `200 OK`:** Same as login response.

**Error Responses:**

| HTTP | Condition |
|---|---|
| `401` | Refresh token is expired or invalid |

---

## 6. Definition of Done

An endpoint is considered **done** when:

1. It returns the exact response schema described here for all success cases.
2. It returns the documented HTTP error codes for all failure cases listed.
3. Authorization rules are enforced (401 / 403 responses as specified).
4. The endpoint is covered by at least one integration test per success case and per listed error case.
5. Response time meets the NFR targets defined in Overview.md §12.1.
