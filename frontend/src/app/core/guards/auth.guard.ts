import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs';

/**
 * Route guard for authenticated routes.
 * Redirects to /login if no valid JWT is present.
 *
 * Applied to all routes inside the MainLayoutComponent shell (app.routes.ts).
 */
export const authGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  return auth.isAuthenticated$.pipe(
    map(authenticated => authenticated || router.createUrlTree(['/login'])),
  );
};
