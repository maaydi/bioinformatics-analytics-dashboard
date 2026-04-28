# Data Validation Rules

> This document specifies every application-level validation rule that the backend must enforce.
> Rules are the source of truth for DTO annotations, `@Valid` constraints, service-layer checks,
> and the `400` / `422` error responses documented in api-contract.md.

---

## 1. `protein_entry` Field Rules

These rules apply on **import** (ItemProcessor) and on any future write endpoints.

| Field | Rule | Error message |
|---|---|---|
| `accession` | Required. Must match `[OPQ][0-9][A-Z0-9]{3}[0-9]` or `[A-NR-Z][0-9][A-Z][A-Z0-9]{2}[0-9]` (UniProt primary accession format). Max 20 chars. | "Invalid accession format" |
| `entry_name` | Required. Max 50 chars. Allowed pattern: `[A-Z0-9_]+` | "Invalid entry name format" |
| `reviewed` | Required. Boolean. | — |
| `protein_full_name` | Optional. Max 1,000 chars. | "Protein full name too long (max 1000)" |
| `gene_name_primary` | Optional. Max 100 chars. Allowed characters: letters, digits, hyphen, underscore. | "Invalid gene name" |
| `gene_name_synonyms` | Optional array. Each element max 100 chars. Max 20 elements. | "Gene synonym too long (max 100)" |
| `gene_orf_names` | Optional array. Each element max 100 chars. Max 50 elements. | — |
| `organism_name` | Required. Max 300 chars. | — |
| `taxid` | Required. Must be a positive integer (> 0). | "taxid must be a positive integer" |
| `length` | Required. Must be > 0 and ≤ 100,000. | "length must be between 1 and 100000" |
| `molecular_weight` | Optional. Must be > 0 if present. | "molecular_weight must be positive" |
| `evidence_level` | Required. Must be an integer in [1, 5]. | "evidence_level must be between 1 and 5" |
| `sequence` | Optional. If present, may only contain standard amino acid characters `[ACDEFGHIKLMNPQRSTVWY]` plus `X`, `B`, `Z`, `J`, `U`, `O`. | "Sequence contains invalid characters" |
| `sequence_checksum` | Optional. Max 20 chars. | — |
| `protein_ec_number` | Optional. If present, must match `\d+\.\d+\.\d+\.\d+` or end with `-`. | "Invalid EC number format" |
| `integrated_date`, `sequence_date`, `updated_date` | Optional. Must be valid ISO-8601 dates (YYYY-MM-DD). | "Invalid date format" |
| `lineage` | Optional array. Each element max 200 chars. Max 50 elements. | — |

---

## 2. Search / Filter Request Rules (`POST /api/genes/search`)

These rules apply to every search request body.

| Field | Rule | HTTP Status | Error Message |
|---|---|---|---|
| `lengthMin` | Must be ≥ 1 if provided. | `400` | "lengthMin must be ≥ 1" |
| `lengthMax` | Must be ≤ 100,000 if provided. | `400` | "lengthMax must be ≤ 100000" |
| `lengthMin`, `lengthMax` | If both provided, `lengthMin ≤ lengthMax`. | `400` | "lengthMin must be ≤ lengthMax" |
| `molecularWeightMin` | Must be ≥ 1 if provided. | `400` | "molecularWeightMin must be ≥ 1" |
| `molecularWeightMin`, `molecularWeightMax` | If both provided, `molecularWeightMin ≤ molecularWeightMax`. | `400` | "molecularWeightMin must be ≤ molecularWeightMax" |
| `evidenceLevels` | Each element must be in [1, 5]. | `400` | "evidenceLevels values must be between 1 and 5" |
| `goTermId` | Must match `GO:\d{7}` if provided. | `400` | "Invalid GO term ID format (expected GO:0000000)" |
| `goAspect` | Must be `P`, `F`, or `C` if provided. | `400` | "goAspect must be P (Process), F (Function), or C (Component)" |
| `taxid` | Must be > 0 if provided. | `400` | "taxid must be a positive integer" |
| `page` | Must be ≥ 0. | `400` | "page must be ≥ 0" |
| `size` | Must be in [1, 500]. | `400` | "size must be between 1 and 500" |
| `globalSearch` | Max 200 characters. | `400` | "Search query too long (max 200 characters)" |
| `accession` | Max 20 characters. | `400` | "accession filter too long (max 20)" |
| `geneNamePrimary` | Max 100 characters. | `400` | "gene name filter too long (max 100)" |
| `organism` | Max 300 characters. | `400` | "organism filter too long (max 300)" |
| `keywords` | Max 10 elements. Each element max 100 characters. | `400` | "Too many keyword filters (max 10)" |

---

## 3. Import Request Rules

| Parameter | Rule | HTTP Status | Error Message |
|---|---|---|---|
| `file` | Required. Max size 2 GB (2,147,483,648 bytes). | `413` | "File exceeds maximum allowed size of 2 GB" |
| `file` | MIME type must be `text/plain`, `text/tab-separated-values`, or `application/octet-stream`. File extension must be `.dat` or `.tsv`. | `422` | "Unsupported file type. Only .dat and .tsv files are accepted." |
| `file` | File must not be empty (0 bytes). | `422` | "Uploaded file is empty" |
| `strategy` | Required. Must be `OVERWRITE`. | `422` | "Unsupported import strategy. Supported values: OVERWRITE" |

---

## 4. Authentication Rules

| Field | Rule | Error |
|---|---|---|
| `username` | Required. Min 3, max 50 characters. | `400` |
| `password` | Required. Min 8 characters. Must contain at least one uppercase letter, one lowercase letter, one digit. | `400` |
| JWT Access Token | Expiry: 1 hour. Algorithm: HS256. Signed with server secret. | `401` on expiry |
| JWT Refresh Token | Expiry: 24 hours. Single-use (invalidated on first use). | `401` on expiry |

---

## 5. Saved Filter Rules

| Field | Rule | HTTP Status | Error Message |
|---|---|---|---|
| `name` | Required. Min 1, max 100 characters after trimming. | `400` | "Name is required" / "Name too long (max 100)" |
| `name` | Must be unique per user. | `409` | "A saved filter with this name already exists" |
| `filterJson` | Must be a valid filter object conforming to the rules in §2 above. | `422` | Field-level messages from §2 |

---

## 6. `keyword` Table Rules

| Field | Rule |
|---|---|
| `name` | Required. Max 100 characters. Must be unique (enforced at DB level). |

---

## 7. `go_term` Table Rules

| Field | Rule |
|---|---|
| `go_id` | Required. Must match `GO:\d{7}`. Must be unique. |
| `aspect` | Required. Must be `P`, `F`, or `C`. |
| `description` | Required. Not blank. |

---

## 8. `protein_feature` Table Rules

| Field | Rule |
|---|---|
| `feature_type` | Required. Max 30 characters. |
| `start_pos` | Optional. If present, must be ≥ 1. |
| `end_pos` | Optional. If present, must be ≥ `start_pos`. |

---

## 9. `cross_reference` Table Rules

| Field | Rule |
|---|---|
| `source` | Required. Max 30 characters. |
| `identifier` | Required. Max 100 characters. |

---

## 10. Validation Implementation Notes

- All DTO validations must be implemented using **Jakarta Bean Validation** (`@NotNull`, `@Size`, `@Min`, `@Max`, `@Pattern`, `@Valid`).
- A global `@RestControllerAdvice` must catch `MethodArgumentNotValidException` and `ConstraintViolationException` and return an `ErrorResponse` with HTTP `400` or `422`.
- Import-specific validation (format, file size) is enforced in the Spring Batch `ItemProcessor` step before any record is written.
- Regex patterns must be validated server-side; client-side validation is an enhancement only.
