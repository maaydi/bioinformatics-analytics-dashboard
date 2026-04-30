import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';

/**
 * Application routes.
 *
 * UI module layout: documentation/overview.md §4
 *
 * Route → Feature component mapping:
 *   /login              → AuthLoginComponent        (public)
 *   /                   → DashboardComponent        (authenticated)
 *   /genes              → GenesPageComponent        (authenticated)
 *   /genes/:id          → GeneDetailComponent       (authenticated)
 *   /analytics          → AnalyticsComponent        (authenticated)
 *   /saved-filters      → SavedFiltersComponent     (authenticated)
 *   /admin/import       → ImportAdminComponent      (ADMIN only)
 */
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'genes',
        loadComponent: () =>
          import('./features/genes/genes-page/genes-page.component').then(
            (m) => m.GenesPageComponent,
          ),
      },
      {
        path: 'genes/:id',
        loadComponent: () =>
          import('./features/gene-detail/gene-detail.component').then((m) => m.GeneDetailComponent),
      },
      {
        path: 'analytics',
        loadComponent: () =>
          import('./features/analytics/analytics.component').then((m) => m.AnalyticsComponent),
      },
      {
        path: 'saved-filters',
        loadComponent: () =>
          import('./features/saved-filters/saved-filters.component').then(
            (m) => m.SavedFiltersComponent,
          ),
      },
      {
        path: 'admin/import',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/import-admin/import-admin.component').then(
            (m) => m.ImportAdminComponent,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
