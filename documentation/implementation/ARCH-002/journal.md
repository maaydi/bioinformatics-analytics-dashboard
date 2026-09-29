# ARCH-002 — Implementation Journal

## Chronological Log

---

### 2026-09-29 — Ticket analysis

**Action:** Created `overview.md` (problem statement C1–C12, goals, AC-1 … AC-10) and `analyse.md` (inventory,
decisions D1–D11, open questions Q1–Q7).

**Outcome:** Implementation blocked until the 🔴 questions (Q2, Q3, Q5) are answered.

---

### 2026-09-29 — Answers to Q1–Q7 received

| Q  | Answer                                                                       |
|----|------------------------------------------------------------------------------|
| Q1 | Schema `gene_data` (dashboard will be renamed `gene-service`)                |
| Q2 | Stage B deferred to ARCH-001 Phase 10                                        |
| Q3 | Merge: new data updates existing data (light file, then detailed API import) |
| Q4 | No delete — add or merge only                                                |
| Q5 | Configurable freshness, default 60 s                                         |
| Q6 | Gateway header-trust hardening in scope (traceability)                       |
| Q7 | Legacy view refresh kept in parallel until parity is proven                  |

**Outcome:** All blockers lifted.

---

### 2026-09-29 — Plan rev. 1 drafted

**Action:** First phased `plan.md` (phases 0–8).

---

### 2026-09-29 — Plan rev. 2 (review against answers and codebase)

**Action:** Reviewed rev. 1 against Q1–Q7 and the code. Verified in the code:

- `domain-model.md` / `ProteinEntry`: `entry_version` is the UniProt `DT` version, so it cannot be the event aggregate
  version.
- `JwtGatewayFilter.shouldNotFilter`: public-endpoint match with `path::contains` and no stripping of inbound
  `X-User-*`.
- `GatewayUserAuthenticationFilter`: every service authenticates from `X-User-*` headers.
- `SavedFilterClient` (import-service): forges `X-User-Id` / `X-User-Role` on a public route.
- Dashboard Flyway history lives in `public` (no `default-schema`); other services use their own schema (`auth`,
  `analytics`, `import_batch`, `export_data`).
- `analytics-service` local `application.yml`: `spring.flyway.default-schema: auth` (wrong fallback).
- `KafkaProducerConfig`: `ADD_TYPE_INFO_HEADERS = true`.
- Dashboard migrations are mostly unqualified; V21 is `public`-qualified (fresh-install leftover in `gene_data`);
  functions `fts_match` (V3) and `trg_protein_entry_search_vector` (V1) must be moved explicitly.

**Outcome:** `plan.md` rev. 2 with 15 findings (F1–F15), normative specs (schema/roles, merge rules, internal API,
events, trust model, configuration), 10 phases (P0–P9), new AC-11 (trust model) and AC-12 (merge), and a corrected
rollout order (import writer switch before analytics backfill).

**Next steps:** P0 — align `analyse.md` and `overview.md` (tasks 0.1, 0.2), update `domain-model.md`, create
`event-catalog.md`, capture the baseline.

---

## Coverage Tracking

| Component                         | Target | Current | Status      |
|-----------------------------------|--------|---------|-------------|
| dashboard (catalog, outbox)       | ≥ 80 % | —       | Not started |
| import-service (writer, ACL)      | ≥ 80 % | —       | Not started |
| analytics-service (projection)    | ≥ 80 % | —       | Not started |
| api-gateway (trust filters)       | ≥ 80 % | —       | Not started |
| common-starter (auto-config, S2S) | ≥ 80 % | —       | Not started |

