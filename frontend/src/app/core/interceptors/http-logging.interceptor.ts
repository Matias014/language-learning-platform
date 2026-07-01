import {HttpErrorResponse, HttpResponse, HttpInterceptorFn} from '@angular/common/http';
import {throwError} from 'rxjs';
import {catchError, finalize, tap} from 'rxjs/operators';
import {environment} from 'environments/environment';

const SENSITIVE_KEYS = new Set([
  'password',
  'newPassword',
  'currentPassword',
  'pwd',
  'pwdCurrent',
  'pwdNew',
  'pwdConfirm',
  'token',
  'accessToken',
  'refreshToken'
]);

function isObject(v: any): v is Record<string, any> {
  return v && typeof v === 'object' && !Array.isArray(v);
}

function truncateString(s: string, max = 2000): string {
  const str = String(s);
  return str.length > max ? str.slice(0, max) + '…' : str;
}

function sanitizeBody(body: any): any {
  if (body == null) return body;
  if (body instanceof FormData) return '[FormData]';
  if (body instanceof Blob) return '[Blob]';
  if (ArrayBuffer.isView(body) || body instanceof ArrayBuffer) return '[Binary]';
  if (typeof body === 'string') return truncateString(body);
  if (Array.isArray(body)) return body.map(sanitizeBody);
  if (!isObject(body)) return body;
  const out: any = {};
  for (const [k, v] of Object.entries(body)) {
    out[k] = SENSITIVE_KEYS.has(k) ? '[REDACTED]' : sanitizeBody(v);
  }
  return out;
}

export const httpLoggingInterceptor: HttpInterceptorFn = (req, next) => {
  if (environment.production) {
    return next(req);
  }

  const started = performance.now();
  console.groupCollapsed(`[HTTP] ${req.method} ${req.urlWithParams}`);
  console.debug('→ request', {
    url: req.urlWithParams,
    method: req.method,
    headers: {Authorization: req.headers.has('Authorization') ? 'REDACTED' : 'none'},
    body: sanitizeBody(req.body)
  });

  return next(req).pipe(
    tap(evt => {
      if (evt instanceof HttpResponse) {
        console.debug('← response', {
          status: evt.status,
          statusText: evt.statusText,
          body: sanitizeBody(evt.body)
        });
      }
    }),
    catchError((err: HttpErrorResponse) => {
      console.error('✖ HTTP error', {
        status: err.status,
        statusText: err.statusText,
        url: err.url,
        code: err?.error?.code ?? null,
        detail: typeof err?.error?.detail === 'string' ? truncateString(err.error.detail, 500) : null
      });
      return throwError(() => err);
    }),
    finalize(() => {
      const ms = Math.round(performance.now() - started);
      console.debug(`⏱ ${ms} ms`);
      console.groupEnd();
    })
  );
};
