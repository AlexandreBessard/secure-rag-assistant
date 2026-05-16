import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { KeycloakService } from './keycloak.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloak = inject(KeycloakService);

  return from(keycloak.updateToken()).pipe(
    switchMap(() => {
      const token = keycloak.token;
      if (!token) return next(req);

      return next(
        req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }),
      );
    }),
  );
};
