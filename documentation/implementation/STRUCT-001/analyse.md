# STRUCT-001 — Ambiguities & Analysis

## Status: Resolved — implementation can proceed

---

## Resolved Decisions

### 1. Viewer Library: Mol* (MolStar) vs NGL vs Iframe

**Decision:** Use Mol* loaded via dynamic `import()`, with a PDBe iframe fallback.

**Rationale:**

| Library           | Bundle Size       | API Maturity                     | Angular Integration    | Maintenance              |
|-------------------|-------------------|----------------------------------|------------------------|--------------------------|
| **Mol***          | ~2.1 MB (gzipped) | High — full programmatic control | Moderate (wrap JS API) | Active (PDBe/RCSB)       |
| **NGL**           | ~1.4 MB           | Legacy — limited updates         | Moderate               | Maintenance mode         |
| **Iframe (PDBe)** | 0 KB              | None — read-only                 | Trivial                | Dependent on PDBe uptime |

- Mol* is the only option that supports bidirectional selection (programmatic highlight + click events).
- Dynamic import keeps the main bundle unaffected; the chunk is fetched only when the Structure tab opens.
- Iframe fallback is implemented as an
  `<iframe src="https://www.ebi.ac.uk/pdbe/entry-files/download/molstar.html?url={cifUrl}">` inside the error-state
  template.

---

### 2. Structure Data Source: AlphaFold CIF vs PDB CIF

**Decision:** Support both; default to AlphaFold when available.

**Rationale:**

- AlphaFold provides near-universal coverage (~200M predictions) but is computational.
- PDB provides experimental validation but coverage is ~200k entries.
- Users in drug discovery prefer PDB; users in metagenomics prefer AlphaFold.
- The UI offers a dropdown to switch; the backend fetches both metadata sets in parallel.

**Format:** Both sources serve mmCIF. Mol* natively consumes mmCIF without conversion.

---

### 3. Residue Numbering: UniProt vs PDB Author vs SIFTS

**Decision:** UniProt numbering is the canonical coordinate system for feature mapping.

**Rationale:**

- UniProt features (from `protein_feature`) use UniProt sequence coordinates (1-based).
- AlphaFold models are aligned 1:1 with UniProt sequences → direct mapping.
- PDB structures often use author numbering or have missing N-terminal residues → require offset translation.
- **SIFTS** (Structure Integration with Function, Taxonomy and Sequences) provides the UniProt ↔ PDB residue mapping.
- **Fallback:** If SIFTS is unreachable, use the `uniprotStart`/`uniprotEnd` range from the PDBe mapping API and apply a
  uniform offset. This may misalign insertions/deletions but preserves approximate feature location.

---

### 4. Cache Invalidation Strategy

**Decision:** Immutable caching with long TTL; no eviction hooks needed.

**Rationale:**

- AlphaFold models are versioned (e.g., v4). Once published, a model URL is immutable.
- PDB entries are immutable after deposition.
- Feature mappings depend only on `protein_feature` rows, which are static between imports.
- If a re-import updates features, the standard `PostImportCacheEvictionListener` (CACHE-001) can be extended to also
  clear `"structures:*"` keys. This is documented as a future task, not required for v1.

---

### 5. Scope Boundary: MSA / Structural Alignment

**Decision:** Out of scope for STRUCT-001.

**Rationale:**

- Structural alignment (superposition of two PDBs) and MSA viewers belong to `PHYLO-001` or a future `ALIGN-001` ticket.
- STRUCT-001 focuses on single-structure visualization + feature overlay.
- The component architecture (model selector + viewer + feature table) is designed to accommodate an alignment mode
  later by extending the model selector to multi-select.

---

## Open Questions (non-blocking)

| Question                                                      | Owner   | Priority | Resolution Path                                                                                                             |
|---------------------------------------------------------------|---------|----------|-----------------------------------------------------------------------------------------------------------------------------|
| Should we proxy CIF files through the backend to avoid CORS?  | Backend | Low      | Implement direct fetch first; add `/api/structures/{accession}/cif-proxy` only if CORS errors observed in integration tests |
| Should pLDDT (confidence) be visualized as B-factor coloring? | UX      | Low      | Defer to v1.1; default to uniform coloring by feature type                                                                  |
| Do we need a structure download button (CIF/PDB file)?        | Product | Low      | One-line addition using `<a download>` on the CIF URL; can be added during polish phase                                     |

---

**Last Updated:** 2026-08-12
