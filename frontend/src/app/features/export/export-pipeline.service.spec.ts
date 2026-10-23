import {HttpErrorResponse, provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';
import {environment} from '@env/environment';
import {firstValueFrom} from 'rxjs';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {ExportPipelineService} from './export-pipeline.service';

describe('ExportPipelineService', () => {
  let service: ExportPipelineService;
  let http: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/v1/exports`;

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    service = TestBed.inject(ExportPipelineService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends optional status and pagination to the list endpoint', async () => {
    const result = firstValueFrom(service.listPipelines('FAILED', 2, 10));
    const request = http.expectOne(request => request.url === `${baseUrl}/pipelines`);
    expect(request.request.params.get('status')).toBe('FAILED');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    request.flush({content: [], totalElements: 0, totalPages: 0, page: 2, size: 10});
    expect((await result).content).toEqual([]);
  });

  it('downloads actual binary data rather than decoding it as JSON', async () => {
    const blob = new Blob(['accession\nP12345'], {type: 'text/csv'});
    const result = firstValueFrom(service.downloadFile(7));
    const request = http.expectOne(`${baseUrl}/pipelines/7/download-file`);
    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');
    request.flush(blob);
    expect(await result).toBe(blob);
  });

  it('decodes blob error bodies without mutating HTTP errors', async () => {
    const result = firstValueFrom(service.downloadFile(7));
    const assertion = expect(result).rejects.toMatchObject({status: 410, error: {message: 'File expired'}});
    http.expectOne(`${baseUrl}/pipelines/7/download-file`).flush(
      new Blob([JSON.stringify({message: 'File expired'})], {type: 'application/json'}),
      {status: 410, statusText: 'Gone'},
    );
    await assertion;
  });

  it('preserves non-JSON download failures as HTTP errors', async () => {
    const result = firstValueFrom(service.downloadFile(7));
    const assertion = expect(result).rejects.toBeInstanceOf(HttpErrorResponse);
    http.expectOne(`${baseUrl}/pipelines/7/download-file`).flush(
      new Blob(['Unavailable']), {status: 503, statusText: 'Unavailable'},
    );
    await assertion;
  });
});
