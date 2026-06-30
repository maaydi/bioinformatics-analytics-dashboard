import {TestBed} from '@angular/core/testing';
import {ErrorInterceptor} from './error-interceptor';
import {HttpErrorResponse, HttpHandler, HttpRequest} from '@angular/common/http';
import {NotificationService} from '@shared/directive/notification.service';
import {throwError} from 'rxjs';

describe('ErrorInterceptor', () => {
  let interceptor: ErrorInterceptor;
  let mockNotificationService: { warning: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    mockNotificationService = {
      warning: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        ErrorInterceptor,
        {provide: NotificationService, useValue: mockNotificationService}
      ]
    });
    interceptor = TestBed.inject(ErrorInterceptor);
  });

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });

  it('should handle 429 error and trigger warning notification', () => {
    const errorResponse = new HttpErrorResponse({status: 429});
    const mockRequest = new HttpRequest('GET', '/api/test');
    const mockHandler = {
      handle: vi.fn().mockReturnValue(throwError(() => errorResponse))
    } as unknown as HttpHandler;

    interceptor.intercept(mockRequest, mockHandler).subscribe({
      error: (err) => {
        expect(err).toBe(errorResponse);
        expect(mockNotificationService.warning).toHaveBeenCalledWith(
          'You have made too many requests. Please slow down and try again.'
        );
      }
    });

    expect(mockHandler.handle).toHaveBeenCalledWith(mockRequest);
  });

  it('should pass through non-429 errors without warning', () => {
    const errorResponse = new HttpErrorResponse({status: 500});
    const mockRequest = new HttpRequest('GET', '/api/test');
    const mockHandler = {
      handle: vi.fn().mockReturnValue(throwError(() => errorResponse))
    } as unknown as HttpHandler;

    interceptor.intercept(mockRequest, mockHandler).subscribe({
      next: () => expect.fail('should have failed with 500 error'),
      error: (err) => {
        expect(err).toBe(errorResponse);
        expect(mockNotificationService.warning).not.toHaveBeenCalled();
      }
    });
  });
});

