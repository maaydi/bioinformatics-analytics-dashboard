import {TestBed} from '@angular/core/testing';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {vi} from 'vitest';
import {adminGuard} from './admin.guard';
import {AuthService} from '../services/auth.service';
import {Observable} from 'rxjs';

describe('adminGuard', () => {
  const rootUrlTree = {} as UrlTree;
  const routeSnapshot = {} as ActivatedRouteSnapshot;
  const stateSnapshot = {} as RouterStateSnapshot;

  const setup = (isAdmin: boolean) => {
    const routerMock = {
      createUrlTree: vi.fn(() => rootUrlTree),
    };

    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: {
            isAdmin: vi.fn(() => isAdmin)
          },
        },
        {provide: Router, useValue: routerMock},
      ],
    });

    return {routerMock};
  };

  it('redirects to / when user is not admin', () => {
    const {routerMock} = setup(false);

    const result = TestBed.runInInjectionContext(() => adminGuard(routeSnapshot, stateSnapshot));

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/']);
    expect(result).toBe(rootUrlTree);
  });

  it('allows navigation when user is admin', () => {
    const {routerMock} = setup(true);

    const result = TestBed.runInInjectionContext(() => adminGuard(routeSnapshot, stateSnapshot));

    expect(routerMock.createUrlTree).not.toHaveBeenCalled();
    expect(result).toBe(true);
  });
});

