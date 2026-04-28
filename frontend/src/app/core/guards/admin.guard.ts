import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Route guard for ADMIN-only routes (/admin/import).
 * Redirects to / if the user lacks ROLE_ADMIN.
 *
 * Authorization matrix: documentation/overview.md §13
 */
export const adminGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  return auth.isAdmin() ? true : router.createUrlTree(['/']);
};
