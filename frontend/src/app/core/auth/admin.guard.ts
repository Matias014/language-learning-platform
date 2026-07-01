import {inject} from '@angular/core';
import {CanMatchFn, Route, Router, UrlSegment, UrlTree} from '@angular/router';
import {AuthService} from '@core/auth/auth.service';
import {catchError, map, of, switchMap} from 'rxjs';

function buildReturnUrl(router: Router, segments?: UrlSegment[]): string {
  if (segments && segments.length) {
    return '/' + segments.map(s => s.path).join('/');
  }
  return router.routerState.snapshot.url || '/admin';
}

function toLogin(router: Router, returnUrl: string): UrlTree {
  return router.createUrlTree(['/login'], {queryParams: {returnUrl}});
}

function checkAdmin(segments?: UrlSegment[]) {
  const auth = inject(AuthService);
  const router = inject(Router);
  const returnUrl = buildReturnUrl(router, segments);

  if (!auth.isAuthenticated()) {
    return auth.refreshAccessToken().pipe(
      switchMap(ok => ok ? auth.me(true).pipe(
        map(u => u ? (String(u.role).toLowerCase() === 'admin' ? true : router.createUrlTree(['/403'])) : toLogin(router, returnUrl)),
        catchError(() => of(toLogin(router, returnUrl)))
      ) : of(toLogin(router, returnUrl)))
    );
  }

  const role = auth.role();
  if (role === 'admin') {
    return true;
  }
  return auth.me(true).pipe(
    map(u => u ? (String(u.role).toLowerCase() === 'admin' ? true : router.createUrlTree(['/403'])) : toLogin(router, returnUrl)),
    catchError(() => of(toLogin(router, returnUrl)))
  );
}

export const adminCanMatch: CanMatchFn = (_route: Route, segments: UrlSegment[]) => checkAdmin(segments);
