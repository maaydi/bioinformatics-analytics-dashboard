# STRUCT-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Resolve ambiguities (viewer library, CIF vs PDB format, SIFTS dependency)
3. Implement backend DTOs and MapStruct mappers
4. Implement `AlphaFoldApiClient` and `PdbDataApiClient`
5. Implement `StructureService` (availability lookup + feature mapping builder)
6. Implement `StructureController` (REST endpoints)
7. Add Redis caching annotations for structure metadata
8. Implement Angular models (`structure.model.ts`, `feature-mapping.model.ts`)
9. Implement `StructureViewerComponent` (Mol* dynamic wrapper)
10. Implement `StructureTabComponent` (tab shell + model selector + feature table)
11. Wire bidirectional selection (viewer ↔ feature table)
12. Implement loading, error, and empty states
13. Write backend unit tests (`StructureServiceTest`, client tests)
14. Write frontend unit tests (`StructureTabComponent`, `StructureViewerComponent`)
15. Write integration tests (`StructureControllerIntegrationTest`)
16. Update documentation and journal

## Status

- [x] Requirements analyzed
- [x] Ambiguities resolved (see analyse.md)
- [ ] Backend DTOs implemented
- [ ] REST clients implemented
- [ ] StructureService implemented
- [ ] StructureController implemented
- [ ] Redis caching wired
- [ ] Angular models defined
- [ ] StructureViewerComponent implemented
- [ ] StructureTabComponent implemented
- [ ] Bidirectional wiring complete
- [ ] UI states implemented
- [ ] Unit tests written (backend)
- [ ] Unit tests written (frontend)
- [ ] Integration tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80%

---

## Detailed Checklist

### Backend — DTOs (`dto/structure/`)

- [ ] `StructureAvailabilityDto` — `{ accession, alphaFold: AlphaFoldEntryDto|null, pdbEntries: PdbEntryDto[] }`
- [ ] `AlphaFoldEntryDto` — `{ modelUrl, cifUrl, plddtUrl, latestVersion, createdAt }`
- [ ] `PdbEntryDto` — `{ pdbId, chainId, resolution, method, uniprotStart, uniprotEnd, entityId }`
- [ ] `StructureFeatureMappingDto` —
  `{ featureId, featureType, uniprotStart, uniprotEnd, authStart, authEnd, chainId, colorHex, note }`
- [ ] `StructureErrorResponse` — `{ status, error, message, timestamp }` (reuses existing `ErrorResponse` schema)

### Backend — REST Clients

- [ ] `AlphaFoldApiClient` (`infrastructure/client/`):
    - [ ] `getPrediction(String accession): Optional<AlphaFoldEntryDto>`
    - [ ] Base URL configurable via `application.yml` (`app.alphafold.api-url`)
    - [ ] Read timeout = 10 s; connect timeout = 5 s
    - [ ] `@Cacheable(value = "structures", key = "'alphafold:' + #accession")`
- [ ] `PdbDataApiClient` (`infrastructure/client/`):
    - [ ] `getUniprotMappings(String accession): List<PdbEntryDto>`
    - [ ] `getEntrySummary(String pdbId): PdbEntryDto` (resolution, method)
    - [ ] Base URL configurable (`app.pdb.api-url`)
    - [ ] `@Cacheable(value = "structures", key = "'pdb:' + #accession")`

### Backend — Service

- [ ] `StructureService` (`service/StructureService.java`):
    - [ ] `getAvailability(String accession): StructureAvailabilityDto`
        - [ ] Calls both clients in parallel (`CompletableFuture` or virtual threads if Java 21)
        - [ ] Returns empty PDB list if PDBe API is unreachable (graceful degradation)
    - [ ] `getFeatureMappings(String accession, String source): List<StructureFeatureMappingDto>`
        - [ ] `source` = `"ALPHAFOLD"` | `"PDB"`
        - [ ] Reads local `protein_feature` rows via `ProteinFeatureRepository.findByProteinEntry_Accession(accession)`
        - [ ] For AlphaFold: direct 1:1 residue mapping
        - [ ] For PDB: applies SIFTS offset if available; falls back to uniprotStart/uniprotEnd from PDB mapping if
          SIFTS call fails
        - [ ] Color palette per feature type (preserves existing evidence-level colors where applicable):
            - CHAIN → `#1a1a1a`
            - DOMAIN → `var(--mat-sys-primary)`
            - SIGNAL → `#e65100`
            - ZINC_FINGER → `#f59e0b`
            - BINDING → `#c62828`
            - VARIANT → `#7c3aed`
            - Default → `#6b7280`

### Backend — Controller

- [ ] `StructureController` (`controller/StructureController.java`):
    - [ ] `GET /api/structures/{accession}` → `200 OK` with `StructureAvailabilityDto`
    - [ ] `GET /api/structures/{accession}/features?source=ALPHAFOLD` → `200 OK` with `List<StructureFeatureMappingDto>`
    - [ ] `404` if accession not found in local DB (validate via `ProteinEntryRepository.existsByAccession`)
    - [ ] `401` without JWT (covered by existing `SecurityConfig`)
    - [ ] Thin controller — delegates all logic to `StructureService`

### Backend — Cache Config

- [ ] Extend `CacheConfig.java` (from CACHE-001) with `"structures"` bucket:
    - TTL = 24 hours for AlphaFold metadata
    - TTL = 7 days for PDB mappings (rarely change)
    - Serializer = `GenericJackson2JsonRedisSerializer`
- [ ] Cache eviction: none required (structure metadata is immutable per accession version)

### Frontend — Models (`core/models/structure.model.ts`)

- [ ] `StructureAvailability` — `{ accession: string; alphaFold: AlphaFoldEntry | null; pdbEntries: PdbEntry[] }`
- [ ] `AlphaFoldEntry` — `{ modelUrl: string; cifUrl: string; latestVersion: number; createdAt: string }`
- [ ] `PdbEntry` —
  `{ pdbId: string; chainId: string; resolution?: number; method: string; uniprotStart: number; uniprotEnd: number }`
- [ ] `StructureFeatureMapping` —
  `{ featureId: string; featureType: string; uniprotStart: number; uniprotEnd: number; authStart: number; authEnd: number; chainId?: string; colorHex: string; note?: string }`

### Frontend — Service (`features/gene-detail/services/structure.service.ts`)

- [ ] `getAvailability(accession: string): Observable<StructureAvailability>`
- [ ] `getFeatureMappings(accession: string, source: 'ALPHAFOLD' | 'PDB'): Observable<StructureFeatureMapping[]>`
- [ ] Inject `HttpClient` via `inject()`

### Frontend — `StructureViewerComponent` (`features/gene-detail/structure-viewer/`)

- [ ] `structure-viewer.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `structure-viewer.component.html` — external template
- [ ] `structure-viewer.component.scss` — viewer canvas fills container; min-height: 400px (HD), 500px (FHD)
- [ ] Inputs:
    - [ ] `cifUrl: string` (required)
    - [ ] `mappings: StructureFeatureMapping[]` (required)
- [ ] Outputs:
    - [ ] `residueClick = output<number>()` — emits UniProt residue index when user clicks a residue
- [ ] Mol* integration:
    - [ ] Dynamic import of `molstar/lib/mol-plugin-ui` inside `ngAfterViewInit`
    - [ ] Initialize `PluginContext` targeting a `<div>` referenced by `viewChild<ElementRef>('canvasContainer')`
    - [ ] Load structure from URL via `PluginCommands.State.ApplyAction`
    - [ ] Apply `Overpaint` representation for each mapping range using `colorHex`
    - [ ] Listen to `plugin.behaviors.interaction.click` → map 3D residue to UniProt index → emit `residueClick`
- [ ] Loading state: skeleton overlay while Mol* initializes and CIF downloads
- [ ] Error state: if Mol* fails to load, show fallback iframe to PDBe viewer + retry button

### Frontend — `StructureTabComponent` (`features/gene-detail/structure-tab/`)

- [ ] `structure-tab.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `structure-tab.component.html` — external template
- [ ] `structure-tab.component.scss` — uses design system tokens
- [ ] Layout (HD/FHD):
  ```
  ┌─ Model Selector (dropdown) ───────────────┐
  │  [AlphaFold · v4 ▾]  [PDB · 6XYZ ▾]       │
  ├─ Viewer (60 % height) ────────────────────┤
  │  <app-structure-viewer>                   │
  ├─ Feature Table (40 % height) ─────────────┤
  │  @for (feature of mappings(); track ...)  │
  └───────────────────────────────────────────┘
  ```
- [ ] Signals:
    - [ ] `availability = signal<StructureAvailability | null>(null)`
    - [ ] `selectedSource = signal<'ALPHAFOLD' | 'PDB'>('ALPHAFOLD')`
    - [ ] `selectedPdbId = signal<string | null>(null)`
    - [ ] `mappings = signal<StructureFeatureMapping[]>([])`
    - [ ] `selectedFeatureId = signal<string | null>(null)`
    - [ ] `highlightedResidue = signal<number | null>(null)`
- [ ] On init:
    - [ ] Call `StructureService.getAvailability(accession)`
    - [ ] If AlphaFold exists, auto-select it and fetch mappings
    - [ ] If no AlphaFold but PDBs exist, auto-select first PDB
- [ ] Interaction wiring:
    - [ ] Click feature table row → `selectedFeatureId.set(id)` → call viewer method `zoomToRange(start, end)`
    - [ ] Click viewer residue → `highlightedResidue.set(index)` → scroll feature table to first overlapping row
- [ ] Responsive:
    - [ ] `@include ds.respond-up-to('md')`: stacked layout (viewer on top, table below)

### Frontend — Integration with Gene Detail

- [ ] Add "Structure" tab to `GeneDetailComponent` tab shell (after "Features", before "GO Terms")
- [ ] Tab is lazy-loaded via `@defer (on viewport)` to avoid downloading Mol* until needed
- [ ] Pass `protein().accession` to `StructureTabComponent`

### Tests — Backend

- [ ] `StructureServiceTest` — unit (mock clients + mock feature repo):
    - [ ] `getAvailability_returnsAlphaFoldAndPdbEntries`
    - [ ] `getAvailability_returnsEmptyWhenApisFail`
    - [ ] `getFeatureMappings_alphaFoldDirectMapping`
    - [ ] `getFeatureMappings_pdbWithSiftsOffset`
    - [ ] `getFeatureMappings_unknownAccession_throws`
- [ ] `AlphaFoldApiClientTest` — `MockRestServiceServer`:
    - [ ] `getPrediction_returnsDtoOn200`
    - [ ] `getPrediction_returnsEmptyOn404`
- [ ] `PdbDataApiClientTest` — `MockRestServiceServer`:
    - [ ] `getUniprotMappings_returnsEntries`
    - [ ] `getUniprotMappings_returnsEmptyOnFailure`
- [ ] `StructureControllerIntegrationTest` — Testcontainers:
    - [ ] `GET /api/structures/Q6GZX4` → `200` with availability
    - [ ] `GET /api/structures/INVALID` → `404`
    - [ ] `GET /api/structures/Q6GZX4/features?source=ALPHAFOLD` → `200` with mappings

### Tests — Frontend

- [ ] `StructureTabComponent` unit tests:
    - [ ] Shows loading skeleton during availability fetch
    - [ ] Shows empty state when no structures available
    - [ ] Auto-selects AlphaFold when present
    - [ ] Auto-selects first PDB when AlphaFold absent
    - [ ] Clicking feature row updates `selectedFeatureId`
    - [ ] Receiving residue click updates `highlightedResidue`
- [ ] `StructureViewerComponent` unit tests:
    - [ ] Initializes Mol* plugin on valid CIF URL
    - [ ] Emits `residueClick` on mock Mol* interaction event
    - [ ] Shows fallback iframe on dynamic import failure
- [ ] `StructureService` unit tests (HttpClientTestingModule):
    - [ ] `getAvailability` sends `GET /api/structures/{accession}`
    - [ ] `getFeatureMappings` sends correct query param `source`

### General

- [ ] No `ngClass` / `ngStyle` — use `class` / `style` bindings only
- [ ] Native control flow (`@if`, `@for`, `@defer`)
- [ ] `ChangeDetectionStrategy.OnPush` on all new components
- [ ] AXE checks pass (viewer canvas has `role="img"` + `aria-label`; focus order logical)
- [ ] Code reviewed
- [ ] Coverage ≥ 80 % (JaCoCo + Jest)

---

## Risk Register

| ID | Risk                                                | Probability | Mitigation                                                                                         |
|----|-----------------------------------------------------|-------------|----------------------------------------------------------------------------------------------------|
| R1 | Mol* bundle size (>2 MB) degrades initial page load | Medium      | Dynamic `import()` inside `@defer`; viewer loads only when tab is opened                           |
| R2 | AlphaFold API rate-limiting (429)                   | Medium      | Redis cache with 24 h TTL; exponential backoff in `AlphaFoldApiClient`                             |
| R3 | SIFTS API unavailable breaks PDB feature mapping    | Low         | Fallback to uniprotStart/uniprotEnd from PDBe mapping API; skip offset translation                 |
| R4 | CORS on CIF URLs (AlphaFold/RCSB)                   | Low         | Proxy CIF requests through backend (`/api/structures/{accession}/cif-proxy`) if direct fetch fails |
| R5 | Memory leak from Mol* PluginContext                 | Medium      | Call `plugin.dispose()` in `ngOnDestroy`; verify with Chrome DevTools heap snapshot                |

---

## Commands

```bash
# Run backend tests
cd backend
./mvnw -Dtest=com.bioinformatics.dashboard.structure.*Test test

# Run frontend tests
cd frontend
ng test --include='**/structure-*/*'

# Verify Redis cache keys after request
redis-cli KEYS "structures:*"
```
