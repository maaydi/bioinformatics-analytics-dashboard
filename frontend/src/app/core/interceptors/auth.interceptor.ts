import {inject} from '@angular/core';
import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {AuthService} from '../services/auth.service';
import {catchError, switchMap, throwError} from 'rxjs';

/**
 * Functional HTTP interceptor — attaches the JWT Bearer token to every
 * outgoing request that targets /api.
 *
 * On 401 responses the user is redirected to /login via AuthService.
 *
 * Spec: documentation/api-contract.md — Authentication:
 * "Authorization: Bearer <JWT> on all protected endpoints"
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getAccessToken();

  if (token && req.url.includes('/api')) {
    req = req.clone({
      setHeaders: {Authorization: `Bearer ${token}`},
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/api/auth/refresh')) {
        return authService.refresh().pipe(
          switchMap((tokenResponse) => {
            const retriedReq = req.clone({
              setHeaders: {Authorization: `Bearer ${tokenResponse.accessToken}`},
            });
            return next(retriedReq);
          }),
          catchError((refreshError) => {
            authService.logout();
            return throwError(() => refreshError);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
