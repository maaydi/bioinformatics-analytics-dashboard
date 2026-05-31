import {PLATFORM_ID} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {firstValueFrom, Observable, of} from 'rxjs';
import {vi} from 'vitest';
import {authGuard} from './auth.guard';
import {AuthService} from '../services/auth.service';

describe('authGuard', () => {
  const loginUrlTree = {} as UrlTree;
  const routeSnapshot = {} as ActivatedRouteSnapshot;
  const stateSnapshot = {} as RouterStateSnapshot;

  const setup = (authenticated: boolean, platformId: unknown) => {
    const routerMock = {
      createUrlTree: vi.fn(() => loginUrlTree),
    };

    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: {
            isAuthenticated$: of(authenticated),
          },
        },
        {provide: Router, useValue: routerMock},
        {provide: PLATFORM_ID, useValue: platformId},
      ],
    });

    return {routerMock};
  };

  it('redirects to /login on server-side execution to avoid rendering protected routes', () => {
    const {routerMock} = setup(false, 'server');

    const result = TestBed.runInInjectionContext(() => authGuard(routeSnapshot, stateSnapshot));

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/login']);
    expect(result).toBe(loginUrlTree);
  });

  it('redirects to /login in browser when user is not authenticated', async () => {
    const {routerMock} = setup(false, 'browser');

    const result = TestBed.runInInjectionContext(() => authGuard(routeSnapshot, stateSnapshot));
    const resolved = await firstValueFrom(result as Observable<boolean | UrlTree>);

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/login']);
    expect(resolved).toBe(loginUrlTree);
  });

  it('allows navigation in browser when user is authenticated', async () => {
    const {routerMock} = setup(true, 'browser');

    const result = TestBed.runInInjectionContext(() => authGuard(routeSnapshot, stateSnapshot));
    const resolved = await firstValueFrom(result as Observable<boolean | UrlTree>);

    expect(routerMock.createUrlTree).not.toHaveBeenCalled();
    expect(resolved).toBe(true);
  });
});


