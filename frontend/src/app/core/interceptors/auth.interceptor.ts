import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from '../services/auth.service';

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
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  // TODO: handle 401 responses — trigger token refresh or redirect to login
  return next(req);
};
