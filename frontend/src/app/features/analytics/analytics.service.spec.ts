import {TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {AnalyticsService} from './analytics.service';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {provideHttpClient} from '@angular/common/http';

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
});

