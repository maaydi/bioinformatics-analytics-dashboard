import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {DataProviderService} from '@core/provider/data-provider.service';

export const dataProviderInterceptor: HttpInterceptorFn = (req, next) => {
  const dataProviderService = inject(DataProviderService);
  const provider = dataProviderService.getProvider();
  if (['/api/genes', '/api/autocomplete'].some((path) => req.url.includes(path))) {
    req = req.clone({
      setHeaders: {
        'X-Data-Provider': provider,
      },
    });
  }

  return next(req);
};
