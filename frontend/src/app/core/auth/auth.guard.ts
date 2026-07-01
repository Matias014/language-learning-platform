import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {AuthService} from '@core/auth/auth.service';
import {map} from 'rxjs';

export const canActivateAuth: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    return true;
  }
  const returnUrl = router.routerState.snapshot.url || '/';
  return auth.refreshAccessToken().pipe(
    map(ok => ok ? true : router.createUrlTree(['/login'], {queryParams: {returnUrl}}))
  );
};

export const canActivateGuest: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    return true;
  }
  return router.parseUrl('/dashboard');
};
