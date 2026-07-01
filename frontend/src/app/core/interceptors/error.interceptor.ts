import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {Router} from '@angular/router';
import {AuthService} from '@core/auth/auth.service';
import {catchError, switchMap, throwError} from 'rxjs';
import {isApi, isAuth} from '@core/http/url.util';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((e: HttpErrorResponse) => {
      const status = e.status;
      const url = e.url || req.url || '';
      const api = isApi(url);
      const authUrl = isAuth(url);
      const retried = req.headers.has('X-Retry');

      if (status === 401 && api && !authUrl && !retried) {
        return auth.refreshAccessToken().pipe(
          switchMap(ok => {
            if (ok) {
              const retry = req.clone({setHeaders: {'X-Retry': '1'}});
              return next(retry);
            }
            const returnUrl = router.routerState.snapshot.url || '/';
            router.navigate(['/login'], {queryParams: {returnUrl}});
            return throwError(() => e);
          })
        );
      }

      if (status === 403) {
        router.navigate(['/403']);
        return throwError(() => e);
      }

      return throwError(() => e);
    })
  );
};
