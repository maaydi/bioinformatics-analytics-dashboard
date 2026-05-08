import {TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {ImportAdminService} from './import-admin.service';
import {environment} from '@env/environment';
import {provideHttpClient} from '@angular/common/http';

describe('ImportAdminService', () => {

  let service: ImportAdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [ImportAdminService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(ImportAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should POST form data when triggering import', () => {
    const fakeFile = new File(['abc'], 'u.fasta', {type: 'text/plain'});

    let called = false;
    service.triggerImport(fakeFile, 'OVERWRITE').subscribe({
      next: () => {
        called = true;
      },
      error: () => {
        throw new Error('should not error');
      }
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/import/uniprot`);
    expect(req.request.method).toBe('POST');
    // body should be FormData
    expect(req.request.body instanceof FormData).toBe(true);

    req.flush({id: '1'});
    expect(called).toBe(true);
  });

  it('should GET paged jobs', () => {
    let called = false;
    service.listImportJobs(0, 10).subscribe({
      next: (res) => {
        expect(res).toBeDefined();
        called = true;
      },
      error: () => {
        throw new Error('should not error');
      }
    });

    const req = httpMock.expectOne(req => req.method === 'GET' && req.url === `${environment.apiBaseUrl}/admin/import/status`);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({content: [], number: 0, size: 10, totalElements: 0, totalPages: 0});
    expect(called).toBe(true);
  });

  it('should GET job progress by id', () => {
    const id = 'abc';
    let called = false;
    service.getJobProgress(id).subscribe({
      next: (res) => {
        expect(res).toBeDefined();
        called = true;
      },
      error: () => {
        throw new Error('should not error');
      }
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/import/status/${id}`);
    expect(req.request.method).toBe('GET');
    req.flush({id});
    expect(called).toBe(true);
  });

});

