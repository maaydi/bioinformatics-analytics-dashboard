import {TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {provideHttpClient} from '@angular/common/http';
import {DashboardService} from './dashboard.service';
import {
  DashboardKpis,
  EvidenceLevelItem,
  KeywordFrequencyItem,
  LengthHistogramBucket,
  OrganismCount,
  ReviewedRatioItem,
} from '@core/models/analytics.model';
import {environment} from '@env/environment';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/analytics`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DashboardService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getDashboardKpis', () => {
    it('should fetch dashboard KPIs', () => {
      const mockKpis: DashboardKpis = {
        totalProteins: 570000,
        reviewedCount: 300000,
        unreviewedCount: 270000,
        organismCount: 50000,
        taxonCount: 10000,
        avgLength: 360,
        avgMolecularWeight: 38500,
        minLength: 10,
        maxLength: 35000,
      };

      service.getDashboardKpis().subscribe((result) => {
        expect(result).toEqual(mockKpis);
      });

      const req = httpMock.expectOne(`${baseUrl}/dashboard-kpis`);
      expect(req.request.method).toBe('GET');
      req.flush(mockKpis);
    });

    it('should handle getDashboardKpis errors', () => {
      service.getDashboardKpis().subscribe(
        () => expect.fail('should have failed'),
        (error) => {
          expect(error.status).toBe(404);
        }
      );

      const req = httpMock.expectOne(`${baseUrl}/dashboard-kpis`);
      req.flush('Not found', {status: 404, statusText: 'Not Found'});
    });
  });

  describe('getLengthHistogram', () => {
    it('should fetch length histogram data', () => {
      const mockHistogram: LengthHistogramBucket[] = [
        {bucket: 1, rangeMin: 0, rangeMax: 100, count: 5000},
        {bucket: 2, rangeMin: 100, rangeMax: 200, count: 10000},
      ];

      service.getLengthHistogram().subscribe((result) => {
        expect(result).toEqual(mockHistogram);
      });

      const req = httpMock.expectOne(`${baseUrl}/length-histogram`);
      expect(req.request.method).toBe('GET');
      req.flush(mockHistogram);
    });

    it('should return empty array on error', () => {
      service.getLengthHistogram().subscribe(
        () => expect.fail('should have failed'),
        (error) => {
          expect(error.status).toBe(500);
        }
      );

      const req = httpMock.expectOne(`${baseUrl}/length-histogram`);
      req.flush('Server error', {status: 500, statusText: 'Internal Server Error'});
    });
  });

  describe('getByOrganism', () => {
    it('should fetch organism counts with default limit', () => {
      const mockOrganisms: OrganismCount[] = [
        {
          organismName: 'Homo sapiens',
          taxid: 9606,
          total: 50000,
          reviewedCount: 40000,
          unreviewedCount: 10000,
          avgLength: 360
        },
        {
          organismName: 'Mus musculus',
          taxid: 10090,
          total: 30000,
          reviewedCount: 25000,
          unreviewedCount: 5000,
          avgLength: 350
        },
      ];

      service.getByOrganism().subscribe((result) => {
        expect(result).toEqual(mockOrganisms);
      });

      const req = httpMock.expectOne(`${baseUrl}/by-organism?limit=50`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrganisms);
    });

    it('should fetch organism counts with custom limit', () => {
      const mockOrganisms: OrganismCount[] = [
        {
          organismName: 'Homo sapiens',
          taxid: 9606,
          total: 50000,
          reviewedCount: 40000,
          unreviewedCount: 10000,
          avgLength: 360
        },
      ];

      service.getByOrganism(10).subscribe((result) => {
        expect(result).toEqual(mockOrganisms);
      });

      const req = httpMock.expectOne(`${baseUrl}/by-organism?limit=10`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrganisms);
    });
  });

  describe('getReviewedRatio', () => {
    it('should fetch reviewed/unreviewed ratio', () => {
      const mockRatio: ReviewedRatioItem[] = [
        {reviewed: true, count: 300000},
        {reviewed: false, count: 270000},
      ];

      service.getReviewedRatio().subscribe((result) => {
        expect(result).toEqual(mockRatio);
      });

      const req = httpMock.expectOne(`${baseUrl}/reviewed-ratio`);
      expect(req.request.method).toBe('GET');
      req.flush(mockRatio);
    });
  });

  describe('getEvidenceLevels', () => {
    it('should fetch evidence level distribution', () => {
      const mockEvidence: EvidenceLevelItem[] = [
        {evidenceLevel: 1, label: 'EXPERIMENTAL', count: 100000},
        {evidenceLevel: 2, label: 'PREDICTED', count: 200000},
      ];

      service.getEvidenceLevels().subscribe((result) => {
        expect(result).toEqual(mockEvidence);
      });

      const req = httpMock.expectOne(`${baseUrl}/evidence-levels`);
      expect(req.request.method).toBe('GET');
      req.flush(mockEvidence);
    });
  });

  describe('getKeywordFrequency', () => {
    it('should fetch keyword frequency with default limit', () => {
      const mockKeywords: KeywordFrequencyItem[] = [
        {keyword: 'transmembrane', count: 50000},
        {keyword: 'signal peptide', count: 40000},
      ];

      service.getKeywordFrequency().subscribe((result) => {
        expect(result).toEqual(mockKeywords);
      });

      const req = httpMock.expectOne(`${baseUrl}/keyword-frequency?limit=100`);
      expect(req.request.method).toBe('GET');
      req.flush(mockKeywords);
    });

    it('should fetch keyword frequency with custom limit', () => {
      const mockKeywords: KeywordFrequencyItem[] = [
        {keyword: 'transmembrane', count: 50000},
      ];

      service.getKeywordFrequency(50).subscribe((result) => {
        expect(result).toEqual(mockKeywords);
      });

      const req = httpMock.expectOne(`${baseUrl}/keyword-frequency?limit=50`);
      expect(req.request.method).toBe('GET');
      req.flush(mockKeywords);
    });
  });
});

