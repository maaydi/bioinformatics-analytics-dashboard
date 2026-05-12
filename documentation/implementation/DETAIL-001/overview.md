# DETAIL-001 — Gene Detail Page

## Description

Implement the Gene Detail page in Angular (`/genes/{id}`):

- Header section with core protein metadata.
- Tabbed view: Overview, Sequence & Features, GO Terms, Cross References, Comments, Publications.
- Each tab loads its payload lazily on first activation.
- External cross-reference links open in new tabs (`rel="noopener noreferrer"`).
- 404 handling for non-existent protein IDs.

## Scope

| Layer     | Artifact                                                                                                                 |
|-----------|--------------------------------------------------------------------------------------------------------------------------|
| Component | `features/gene-detail/gene-detail.component` — routed page component                                                     |
| Service   | `features/genes/genes.service.ts` — `getById(id)` returning `Observable<ProteinDetail>`                                  |
| Model     | `ProteinDetail`, `FeatureItem`, `GoTermItem`, `CrossReferenceItem`, `CommentItem`, `PublicationItem`, `HostOrganismItem` |
| Routing   | `/genes/:id` lazy-loaded route behind `authGuard`                                                                        |

## Acceptance Criteria

- [ ] Navigating to `/genes/{id}` renders the detail page with: Accession, Entry Name, Protein Full Name, Organism,
  Reviewed badge, Evidence level badge, Length, Molecular Weight.
- [ ] The "Sequence" tab shows the full amino acid sequence in monospace FASTA-like format with length label.
- [ ] The "Features" tab renders a table: Type, Start, End, Note, Feature ID.
- [ ] The "GO Terms" tab renders: GO ID, Aspect (P/F/C), Description, Evidence Code.
- [ ] The "Cross References" tab renders: Source, Identifier with external link icon opening in new tab.
- [ ] The "Comments" tab renders: Type, Text.
- [ ] The "Publications" tab renders: Ref number, PubMed ID (linked), DOI, Authors, Title, Journal.
- [ ] Navigating to `/genes/999999` (non-existent) shows a "Protein not found" error message.
- [ ] Each tab triggers a single HTTP request only on first activation (no re-fetch on revisit).
- [ ] Loading state shown during fetch.
- [ ] `ChangeDetectionStrategy.OnPush` on the component.
- [ ] Unit tests for `GeneDetailComponent`.

## References

- `documentation/api-contract.md` §1 — `GET /api/genes/{id}` and `ProteinDetail` schema
- `documentation/plan.md` — US-15, US-16, US-17
