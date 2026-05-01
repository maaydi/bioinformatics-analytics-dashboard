import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, take } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Route guard for authenticated routes.
 * Redirects to /login if no valid JWT is present.
 *
 * Applied to all routes inside the MainLayoutComponent shell (app.routes.ts).
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  return auth.isAuthenticated$.pipe(
    take(1),
    map((authenticated) => authenticated || router.createUrlTree(['/login'])),
  );
};
