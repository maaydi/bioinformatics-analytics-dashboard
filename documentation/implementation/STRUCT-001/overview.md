# STRUCT-001 — Structural Context Module (3D Protein Viewer)

## Description

Integrate an interactive 3D protein structure viewer into the Gene Detail page, enabling users to inspect
AlphaFold-predicted models and experimentally solved PDB structures with UniProt sequence features mapped directly onto
the 3D coordinates.

The module delivers **bidirectional interaction**: clicking a sequence feature (e.g., CHAIN, DOMAIN, ZINC_FINGER) in the
Features tab highlights the corresponding residues in the 3D viewer; conversely, clicking a residue in the 3D viewer
scrolls the Features table to the overlapping annotation and reveals the feature metadata.

This transforms the application from a sequence-centric catalog into a structural-biology workbench, a capability that
currently requires users to juggle three separate browser tabs (UniProt + AlphaFold DB + PDB).

---

## Scope

| Layer                    | Artifact                                                                                     | Description                                                                   |
|--------------------------|----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| **Backend — Controller** | `StructureController`                                                                        | `GET /api/structures/{accession}`, `GET /api/structures/{accession}/features` |
| **Backend — Service**    | `StructureService`                                                                           | Orchestrates AlphaFold and PDB lookups; builds feature-to-residue mappings    |
| **Backend — Client**     | `AlphaFoldApiClient`                                                                         | REST client for `https://alphafold.ebi.ac.uk/api`                             |
| **Backend — Client**     | `PdbDataApiClient`                                                                           | REST client for `https://data.rcsb.org/rest/v1` + SIFTS mapping               |
| **Backend — DTOs**       | `StructureAvailabilityDto`, `AlphaFoldEntryDto`, `PdbEntryDto`, `StructureFeatureMappingDto` | Response schemas                                                              |
| **Frontend — Component** | `structure-tab.component`                                                                    | New tab shell inside Gene Detail (lazy-loaded)                                |
| **Frontend — Component** | `structure-viewer.component`                                                                 | Mol* viewer wrapper with dynamic import                                       |
| **Frontend — Service**   | `structure-viewer.service.ts`                                                                | Bridges Angular signals to Mol* plugin commands                               |
| **Frontend — Model**     | `structure.model.ts`, `feature-mapping.model.ts`                                             | TypeScript interfaces                                                         |
| **Cache**                | Redis `"structures"` bucket                                                                  | TTL = 24 h for AlphaFold metadata; TTL = 7 d for PDB mappings                 |
| **Design System**        | `structure-tab.component.scss`                                                               | Uses `ds.$spacing-*`, `ds.text-body-sm`, `ds.shadow-sm`                       |

---

## Acceptance Criteria

### AC-1 — Structure Availability Discovery

```
Given the Gene Detail page is open for accession Q6GZX4
When the user clicks the "Structure" tab
Then a loading skeleton appears
And the backend calls AlphaFold DB API and RCSB PDB mapping service
And the tab header shows available models: "AlphaFold (1) · PDB (0)"
```

### AC-2 — AlphaFold Model Loading

```
Given structure metadata has loaded
When the user selects the AlphaFold model
Then the 3D viewer renders the CIF structure in cartoon representation
And the viewport is centered on the full model
And a sequence track below the viewer shows the UniProt sequence with feature regions colored
```

### AC-3 — Feature-to-Structure Highlighting (Sequence → 3D)

```
Given the Features table and 3D viewer are both visible
When the user clicks the "Zinc finger" feature row (positions 120–145)
Then the viewer rotates to center on residues 120–145
And those residues are rendered in ball-and-stick with a gold color overlay
And all other residues fade to 30 % opacity
```

### AC-4 — Residue-to-Feature Selection (3D → Sequence)

```
Given the 3D viewer is focused on the model
When the user clicks residue 132 in the 3D viewport
Then the Features table auto-scrolls to the "Zinc finger" row
And that row receives a surface-hover highlight background (#f3f4f6)
And a tooltip appears in the viewer: "Zinc finger (120–145) — PRO_0000410512"
```

### AC-5 — PDB Structure Switching

```
Given the protein has mapped PDB entries (e.g., 6XYZ, 6YYZ)
When the user selects "PDB · 6XYZ" from the model selector dropdown
Then the viewer loads the PDB CIF file from RCSB
And the feature mapping recalculates using the PDB → UniProt SIFTS alignment
And the viewport resets to the full structure
```

### AC-6 — Fallback & Empty States

```
Given the protein has no AlphaFold model and no PDB entries
When the Structure tab loads
Then an empty-state illustration is shown with text:
  "No experimental or predicted structures available for this protein."
And a link to submit the sequence to the AlphaFold Server is provided
```

### AC-7 — Keyboard Accessibility

```
Given the Structure tab is active
When the user presses Tab
Then focus moves sequentially: model selector → viewer canvas → feature table → back link
And focus rings use ds.focus-ring() styling
```

### AC-8 — Performance & Caching

```
Given 50 users request the AlphaFold metadata for P53_HUMAN within 1 hour
Then only 1 outbound HTTP call is made to the AlphaFold API
And subsequent requests are served from Redis in ≤ 10 ms
```

---

## Key Design Decisions

### Viewer Library: Mol* (MolStar)

- **Rationale:** Official successor to NGL and LiteMol; actively maintained by PDBe/RCSB; supports CIF/BCIF;
  programmable selection API; permissive MIT license.
- **Loading strategy:** Dynamic `import('molstar/lib/mol-plugin-ui')` inside `StructureViewerComponent` to avoid
  bloating the main bundle.
- **Fallback:** If dynamic import fails (network/firewall), display an iframe embedding the PDBe Mol* viewer with a
  pre-computed URL.

### Data Sources

- **AlphaFold DB:** `https://alphafold.ebi.ac.uk/api/prediction/{accession}` → CIF coordinates + pLDDT scores.
- **PDB Mapping:** `https://www.ebi.ac.uk/pdbe/api/mappings/uniprot/{accession}` → PDB IDs + chain mappings.
- **SIFTS (optional v1.1):** Used to translate PDB residue numbering to UniProt numbering for accurate feature overlay.

### Feature Mapping Algorithm

1. Read `protein_feature` rows for the accession (startPos, endPos, featureType).
2. For AlphaFold models: numbering is 1:1 with UniProt sequence → direct range mapping.
3. For PDB models: use SIFTS `residueMapping` to translate PDB chain residue indices to UniProt indices.
4. Build `StructureFeatureMappingDto` with `uniprotStart`, `uniprotEnd`, `authStart`, `authEnd`, `chainId`, `color`,
   `featureType`.

### Provider Architecture Compatibility

- `StructureService` is **orthogonal** to the `GeneService` provider layer (`postgres` vs `uniprotKb`).
- It always operates on accession strings, which are stable identifiers across both providers.
- Future extension: a `StructureProvider` interface could allow plugging in ColabFold or SWISS-MODEL sources without
  touching the frontend.

---

## References

- `documentation/api-contract.md` §1 — `GET /api/genes/{id}` (accession stability)
- `documentation/domain-model.md` §7 — `protein_feature` schema (feature_type, start_pos, end_pos)
- `documentation/implementation/REFACTOR-001/overview.md` — pluggable provider architecture (orthogonal service pattern)
- `documentation/implementation/DETAIL-001/overview.md` — Gene Detail page tab shell
- Mol* Documentation: https://molstar.org/docs/
- AlphaFold DB API: https://alphafold.ebi.ac.uk/api-docs
- PDBe Mapping API: https://www.ebi.ac.uk/pdbe/api/doc/

---

**Ticket Created**: 2026-08-12  
**Target Release**: Phase 4 (post REFACTOR-001 / REMOTE-001)  
**Estimated Effort**: L (4–5 weeks)
