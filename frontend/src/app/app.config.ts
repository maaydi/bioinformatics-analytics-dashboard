import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withFetch,
  withInterceptors,
  withInterceptorsFromDi
} from '@angular/common/http';
import {ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners,} from '@angular/core';
import {provideRouter, withComponentInputBinding} from '@angular/router';

import {provideClientHydration, withEventReplay} from '@angular/platform-browser';
import {AuthService} from '@core/services/auth.service';
import {catchError, of} from 'rxjs';
import {routes} from './app.routes';
import {authInterceptor} from '@core/interceptors/auth.interceptor';
import {ErrorInterceptor} from '@shared/directive/error-interceptor';
import {dataProviderInterceptor} from '@core/provider/data-provider.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideClientHydration(withEventReplay()),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor, dataProviderInterceptor]), withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    },
    provideAppInitializer(() => {
      const authService = inject(AuthService);
      if (authService.getAccessToken()) {
        return authService.refresh().pipe(
          catchError(() => {
            authService.logout();
            return of(true);
          }),
        );
      }
      return Promise.resolve(true);
    }),
  ],
};
