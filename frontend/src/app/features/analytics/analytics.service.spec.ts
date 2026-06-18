import {TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {AnalyticsService} from './analytics.service';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {provideHttpClient} from '@angular/common/http';
import {CompareRequest, CompareResponse} from '@core/models/analytics.model';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AnalyticsService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST dashboard kpis with provided filter', () => {
    const filter = {} as GeneFilterSnapshot;
    const mockResponse = {totalProteins: 10} as any;

    service.getDashboardKpis(filter).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const baseUrl = (service as any).baseUrl as string;
    const req = httpMock.expectOne(`${baseUrl}/dashboard-kpis`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBe(filter);
    req.flush(mockResponse);
  });

  it('should POST by-organism with limit param', () => {
    const filter = {} as GeneFilterSnapshot;
    const mockResponse = [{organism: 'Homo sapiens', total: 42}];

    service.getByOrganism(25, filter).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const baseUrl = (service as any).baseUrl as string;
    const req = httpMock.expectOne(r => r.url === `${baseUrl}/by-organism` && r.method === 'POST');
    expect(req.request.params.get('limit')).toBe('25');
    expect(req.request.body).toBe(filter);
    req.flush(mockResponse);
  });

  it('should POST compare with CompareRequest containing setA and setB', () => {
    const filterA = {globalSearch: 'kinase'} as GeneFilterSnapshot;
    const filterB = {globalSearch: 'phosphatase'} as GeneFilterSnapshot;
    const compareRequest: CompareRequest = {
      setA: filterA,
      setB: filterB,
    };
    const mockResponse: CompareResponse = {
      subsetA: {
        count: 100,
        avgLength: 350.5,
        reviewedCount: 80,
        reviewedRatio: 0.8,
        lengthDistribution: [],
        evidenceDistribution: [],
      },
      subsetB: {
        count: 250,
        avgLength: 425.3,
        reviewedCount: 150,
        reviewedRatio: 0.6,
        lengthDistribution: [],
        evidenceDistribution: [],
      },
    };

    service.compare(compareRequest).subscribe(res => {
      expect(res).toEqual(mockResponse);
      expect(res.subsetA.count).toBe(100);
      expect(res.subsetB.count).toBe(250);
    });

    const baseUrl = (service as any).baseUrl as string;
    const req = httpMock.expectOne(`${baseUrl}/compare`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(compareRequest);
    expect(req.request.body.setA).toBe(filterA);
    expect(req.request.body.setB).toBe(filterB);
    req.flush(mockResponse);
  });

  it('should return Observable of CompareResponse from compare()', () => {
    const compareRequest = {
      setA: {} as GeneFilterSnapshot,
      setB: {} as GeneFilterSnapshot,
    };
    const mockResponse = {
      subsetA: {
        count: 0,
        avgLength: 0,
        reviewedCount: 0,
        reviewedRatio: 0,
        lengthDistribution: [],
        evidenceDistribution: []
      },
      subsetB: {
        count: 0,
        avgLength: 0,
        reviewedCount: 0,
        reviewedRatio: 0,
        lengthDistribution: [],
        evidenceDistribution: []
      },
    } as CompareResponse;

    let resultReceived: CompareResponse | null = null;
    service.compare(compareRequest).subscribe(res => {
      resultReceived = res;
    });

    const baseUrl = (service as any).baseUrl as string;
    const req = httpMock.expectOne(`${baseUrl}/compare`);
    req.flush(mockResponse);

    expect(resultReceived).toEqual(mockResponse);
  });
});

