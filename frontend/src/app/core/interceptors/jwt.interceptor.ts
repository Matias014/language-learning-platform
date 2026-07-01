import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {isApi, isAuth, isMedia, isStatic} from '@core/http/url.util';
import {AuthService} from '@core/auth/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const url = req.url || '';
  if (isStatic(url) || isMedia(url) || !isApi(url) || isAuth(url)) {
    return next(req);
  }

  const auth = inject(AuthService);
  const token = auth.getToken();

  if (token && auth.isAuthenticated()) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(req);
};
