import {ApplicationConfig, provideZoneChangeDetection} from '@angular/core';
import {provideRouter} from '@angular/router';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {routes} from './app.routes';
import {jwtInterceptor} from '@core/interceptors/jwt.interceptor';
import {errorInterceptor} from '@core/interceptors/error.interceptor';
import {httpLoggingInterceptor} from '@core/interceptors/http-logging.interceptor';
import {environment} from 'environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({eventCoalescing: true}),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        jwtInterceptor,
        ...(environment.production ? [] : [httpLoggingInterceptor]),
        errorInterceptor,
      ])
    ),
  ],
};
