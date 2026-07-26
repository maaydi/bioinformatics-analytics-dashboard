import {TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {provideHttpClient} from '@angular/common/http';
import {environment} from '@env/environment';
import {GenesService} from './genes.service';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinDetail, ProteinSummary} from '@core/models/protein.model';

describe('GenesService', () => {
  let service: GenesService;
  let httpMock: HttpTestingController;

  const baseUrl = `${environment.apiBaseUrl}/genes`;

  const proteinSummary: ProteinSummary = {
    id: 1,
    accession: 'P12345',
    entryName: 'GENE_HUMAN',
    proteinFullName: 'Protein Name',
    geneNamePrimary: 'GENE1',
    organismName: 'Homo sapiens',
    taxid: 9606,
    reviewed: true,
    length: 350,
    molecularWeight: 38000,
    evidenceLevel: 1,
    keywords: ['Kinase']
  };

  const detailResponse: ProteinDetail = {
    ...proteinSummary,
    proteinShortName: 'PN',
    proteinEcNumber: null,
    geneNameSynonyms: ['GENE1A'],
    geneOrfNames: [],
    geneOrderedLocus: [],
    organismCommonName: 'Human',
    lineage: ['Eukaryota', 'Metazoa'],
    integratedDate: '2026-01-01',
    sequenceDate: '2026-01-02',
    updatedDate: '2026-01-03',
    sequenceVersion: 1,
    entryVersion: 1,
    sequenceChecksum: 'ABC123',
    sequence: 'MSEQUENCE',
    features: [],
    goTerms: [],
    crossReferences: [],
    comments: [],
    publications: [],
    hostOrganisms: []
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GenesService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(GenesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listGenes should call GET /genes with paging and sorting params', () => {
    const response: PagedResponse<ProteinSummary> = {
      content: [proteinSummary],
      page: 2,
      size: 25,
      totalElements: 1,
      totalPages: 1
    };

    service.listGenes(2, 25, 'accession', 'desc').subscribe((res) => {
      expect(res).toEqual(response);
    });

    const req = httpMock.expectOne((request) => request.method === 'GET' && request.url === baseUrl);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('25');
    expect(req.request.params.get('sort')).toBe('accession');
    expect(req.request.params.get('direction')).toBe('desc');
    req.flush(response);
  });

  it('listGenes should use default paging and sorting params when omitted', () => {
    const response: PagedResponse<ProteinSummary> = {
      content: [],
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0
    };

    service.listGenes().subscribe((res) => {
      expect(res).toEqual(response);
    });

    const req = httpMock.expectOne((request) => request.method === 'GET' && request.url === baseUrl);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('50');
    expect(req.request.params.get('sort')).toBe('id');
    expect(req.request.params.get('direction')).toBe('asc');
    req.flush(response);
  });

  it('searchGenes should call POST /genes/search with filter payload', () => {
    const filter: GeneFilterSnapshot & { page: number; size: number; sort: string; direction: 'asc' | 'desc' } = {
      globalSearch: 'kinase',
      taxid: 9606,
      keywords: ['Kinase'],
      page: 1,
      size: 50,
      sort: 'id',
      direction: 'asc'
    };

    const response: PagedResponse<ProteinSummary> = {
      content: [proteinSummary],
      page: 1,
      size: 50,
      totalElements: 1,
      totalPages: 1
    };

    service.searchGenes(filter).subscribe((res) => {
      expect(res).toEqual(response);
    });

    const req = httpMock.expectOne(`${baseUrl}/search`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(filter);
    req.flush(response);
  });

  it('getGeneById should call GET /genes/:id', () => {
    service.getByAccession(42).subscribe((res) => {
      expect(res).toEqual(detailResponse);
    });

    const req = httpMock.expectOne(`${baseUrl}/42`);
    expect(req.request.method).toBe('GET');
    req.flush(detailResponse);
  });

  it('exportCsv should call POST /genes/export-csv and request blob response type', () => {
    const filter: GeneFilterSnapshot = {
      globalSearch: 'membrane'
    };
    const blob = new Blob(['id,accession\n1,P12345'], {type: 'text/csv'});

    service.exportCsv(filter).subscribe((res) => {
      expect(res).toBeInstanceOf(Blob);
      expect(res.size).toBe(blob.size);
    });

    const req = httpMock.expectOne(`${baseUrl}/export-csv`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(filter);
    expect(req.request.responseType).toBe('blob');
    req.flush(blob);
  });

  it('loadKeywords should call GET /genes/keywords', () => {
    const keywords = ['Kinase', 'Signal'];

    service.loadKeywords().subscribe((res) => {
      expect(res).toEqual(keywords);
    });

    const req = httpMock.expectOne(`${baseUrl}/keywords`);
    expect(req.request.method).toBe('GET');
    req.flush(keywords);
  });
});

