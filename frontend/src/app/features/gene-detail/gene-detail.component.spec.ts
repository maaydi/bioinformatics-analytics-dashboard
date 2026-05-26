import {ComponentFixture, TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {of, throwError} from 'rxjs';
import {GeneDetailComponent} from './gene-detail.component';
import {GenesService} from '@features/genes/genes.service';
import {ProteinDetail} from '@core/models/protein.model';
import {provideRouter} from '@angular/router';

const proteinDetailFixture: ProteinDetail = {
  id: 42,
  accession: 'P12345',
  entryName: 'PROT_HUMAN',
  proteinFullName: 'Protein kinase test',
  geneNamePrimary: 'PKT1',
  organismName: 'Homo sapiens',
  taxid: 9606,
  reviewed: true,
  length: 350,
  molecularWeight: 41234,
  evidenceLevel: 2,
  keywords: ['Kinase'],
  proteinShortName: 'PKT',
  proteinEcNumber: null,
  geneNameSynonyms: [],
  geneOrfNames: [],
  geneOrderedLocus: [],
  organismCommonName: 'Human',
  lineage: ['Eukaryota'],
  integratedDate: '2024-05-01',
  sequenceDate: '2024-05-01',
  updatedDate: '2024-05-10',
  sequenceVersion: 1,
  entryVersion: 1,
  sequenceChecksum: 'abc123',
  sequence: 'AAAA',
  features: [],
  goTerms: [],
  crossReferences: [
    {
      source: 'RefSeq',
      identifier: 'YP_654604.1',
      secondaryId: 'NC_008187.1',
      tertiaryInfo: '',
    },
  ],
  comments: [],
  publications: [],
  hostOrganisms: [
    {
      taxid: 42431,
      name: 'Culex territans.',
    },
  ],
};

describe('GeneDetailComponent', () => {
  let fixture: ComponentFixture<GeneDetailComponent>;
  let component: GeneDetailComponent;
  let genesServiceMock: Pick<GenesService, 'getGeneById'>;

  beforeEach(async () => {
    vi.useFakeTimers();
    genesServiceMock = {
      getGeneById: vi.fn().mockReturnValue(of(proteinDetailFixture)),
    };

    await TestBed.configureTestingModule({
      imports: [GeneDetailComponent],
      providers: [
        {provide: GenesService, useValue: genesServiceMock},
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GeneDetailComponent);
    fixture.componentRef.setInput('id', 42);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('loads protein details on init', () => {
    expect(genesServiceMock.getGeneById).toHaveBeenCalledWith(42);
    expect(component.proteinDetails()?.accession).toBe('P12345');
    expect(component.loading()).toBe(false);
  });

  it('returns review/evidence classes aligned with table badges', () => {
    expect(component.reviewBadgeClass()).toBe('review-badge is-reviewed');
    expect(component.evidenceBadgeClass()).toBe('evidence-badge level-2');
    expect(component.evidenceLevelStr()).toContain('Evidence level 2');
  });

  it('copies accession to clipboard and resets feedback message', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(globalThis.navigator, 'clipboard', {
      value: {writeText},
      configurable: true,
    });

    await component.copyAccessionToClipboard();

    expect(writeText).toHaveBeenCalledWith('P12345');
    expect(component.copyFeedbackMessage()).toBe('Accession copied.');

    vi.advanceTimersByTime(1800);
    expect(component.copyFeedbackMessage()).toBeNull();
  });

  it('shows error message when loading fails', () => {
    genesServiceMock.getGeneById = vi.fn().mockReturnValue(throwError(() => new Error('failed')));

    const failingFixture = TestBed.createComponent(GeneDetailComponent);
    failingFixture.componentRef.setInput('id', 42);
    const failingComponent = failingFixture.componentInstance;
    failingFixture.detectChanges();

    expect(failingComponent.errorMessage()).toBe('Failed to load protein details.');
    expect(failingComponent.loading()).toBe(false);
  });

  it('builds provider-specific external URLs for cross references', () => {
    expect(component.crossReferenceUrl('RefSeq', 'YP_654604.1'))
      .toBe('https://www.ncbi.nlm.nih.gov/protein/YP_654604.1');
    expect(component.crossReferenceUrl('KEGG', 'vg:4156342'))
      .toBe('https://www.genome.jp/entry/vg%3A4156342');
  });

  it('returns null URL for unsupported sources or placeholder identifiers', () => {
    expect(component.crossReferenceUrl('UnknownSource', 'ABC123')).toBeNull();
    expect(component.crossReferenceUrl('EMBL', '-')).toBeNull();
    expect(component.crossReferenceUrl('EMBL', null)).toBeNull();
  });

  it('builds PubMed and DOI URLs for publication links', () => {
    const publication = {
      refNumber: 1,
      pubmedId: '16912294',
      doi: '10.1128/jvi.00464-06',
      authors: null,
      title: null,
      journal: null,
    };

    expect(component.publicationPubMedUrl(publication))
      .toBe('https://pubmed.ncbi.nlm.nih.gov/16912294');
    expect(component.publicationDoiUrl(publication))
      .toBe('https://doi.org/10.1128%2Fjvi.00464-06');
  });

  it('returns null publication URLs when identifiers are missing', () => {
    const publication = {
      refNumber: 1,
      pubmedId: null,
      doi: null,
      authors: null,
      title: null,
      journal: null,
    };

    expect(component.publicationPubMedUrl(publication)).toBeNull();
    expect(component.publicationDoiUrl(publication)).toBeNull();
  });
});

