import {TestBed} from '@angular/core/testing';
import {HttpErrorResponse, HttpInterceptorFn, HttpRequest, HttpEvent, HttpHandlerFn} from '@angular/common/http';
import {authInterceptor} from './auth.interceptor';
import {AuthService} from '../services/auth.service';
import {vi} from 'vitest';
import {of, throwError, Observable} from 'rxjs';

describe('authInterceptor', () => {
  let authServiceMock: any;
  let nextMock: HttpHandlerFn;

  beforeEach(() => {
    authServiceMock = {
      getAccessToken: vi.fn(),
      refresh: vi.fn(),
      logout: vi.fn()
    };

    nextMock = vi.fn((req: HttpRequest<unknown>) => of({} as HttpEvent<unknown>));

    TestBed.configureTestingModule({
      providers: [
        {provide: AuthService, useValue: authServiceMock}
      ]
    });
  });

  const runInterceptor = (req: HttpRequest<unknown>): Observable<HttpEvent<unknown>> => {
    return TestBed.runInInjectionContext(() => authInterceptor(req, nextMock));
  };

  it('should not add Authorization header if url does not contain /api', () => {
    authServiceMock.getAccessToken.mockReturnValue('fake-token');
    const req = new HttpRequest('GET', '/other');

    runInterceptor(req).subscribe();

    expect(nextMock).toHaveBeenCalledWith(req);
  });

  it('should not add Authorization header if token is missing', () => {
    authServiceMock.getAccessToken.mockReturnValue(null);
    const req = new HttpRequest('GET', '/api/data');

    runInterceptor(req).subscribe();

    expect(nextMock).toHaveBeenCalledWith(req);
  });

  it('should add Authorization header if token exists and url contains /api', () => {
    authServiceMock.getAccessToken.mockReturnValue('fake-token');
    const req = new HttpRequest('GET', '/api/data');

    runInterceptor(req).subscribe();

    expect(nextMock).toHaveBeenCalled();
    const clonedReq = (nextMock as any).mock.calls[0][0];
    expect(clonedReq.headers.get('Authorization')).toBe('Bearer fake-token');
  });

  it('should handle 401 error and refresh token', () => {
    authServiceMock.getAccessToken.mockReturnValue('fake-token');
    authServiceMock.refresh.mockReturnValue(of({accessToken: 'new-token'}));
    const req = new HttpRequest('GET', '/api/data');

    nextMock = vi.fn().mockReturnValueOnce(throwError(() => new HttpErrorResponse({status: 401})))
                      .mockReturnValueOnce(of({} as HttpEvent<unknown>));

    runInterceptor(req).subscribe();

    expect(authServiceMock.refresh).toHaveBeenCalled();
    expect(nextMock).toHaveBeenCalledTimes(2);
    const retriedReq = (nextMock as any).mock.calls[1][0];
    expect(retriedReq.headers.get('Authorization')).toBe('Bearer new-token');
  });

  it('should logout if refresh token fails', () => {
    authServiceMock.getAccessToken.mockReturnValue('fake-token');
    authServiceMock.refresh.mockReturnValue(throwError(() => new Error('Refresh failed')));
    const req = new HttpRequest('GET', '/api/data');

    nextMock = vi.fn().mockReturnValueOnce(throwError(() => new HttpErrorResponse({status: 401})));

    runInterceptor(req).subscribe({
      error: (err) => {
        expect(err).toBeInstanceOf(Error);
      }
    });

    expect(authServiceMock.refresh).toHaveBeenCalled();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });

  it('should pass through errors other than 401', () => {
    authServiceMock.getAccessToken.mockReturnValue('fake-token');
    const req = new HttpRequest('GET', '/api/data');

    const errorResponse = new HttpErrorResponse({status: 403});
    nextMock = vi.fn().mockReturnValueOnce(throwError(() => errorResponse));

    runInterceptor(req).subscribe({
      error: (err) => {
        expect(err).toBe(errorResponse);
      }
    });

    expect(authServiceMock.refresh).not.toHaveBeenCalled();
  });

  it('should not refresh token if 401 is on refresh endpoint itself', () => {
    authServiceMock.getAccessToken.mockReturnValue('fake-token');
    const req = new HttpRequest('POST', '/api/auth/refresh', null);

    const errorResponse = new HttpErrorResponse({status: 401});
    nextMock = vi.fn().mockReturnValueOnce(throwError(() => errorResponse));

    runInterceptor(req).subscribe({
      error: (err) => {
        expect(err).toBe(errorResponse);
      }
    });

    expect(authServiceMock.refresh).not.toHaveBeenCalled();
  });
});

