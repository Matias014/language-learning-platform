import {environment} from 'environments/environment';

function pathnameOf(u: string): string {
  if (/^https?:\/\//i.test(u)) {
    try {
      const p = new URL(u).pathname || '/';
      return p.length > 1 && p.endsWith('/') ? p.slice(0, -1) : p;
    } catch {
      return '/';
    }
  }
  if (!u) return '/';
  const q = u.indexOf('?');
  const h = u.indexOf('#');
  const end = [q, h].filter(i => i >= 0).reduce((m, i) => Math.min(m, i), u.length);
  let p = u.slice(0, end) || '/';
  while (p.startsWith('./')) p = p.slice(2);
  if (!p.startsWith('/')) p = '/' + p;
  if (p.length > 1 && p.endsWith('/')) p = p.slice(0, -1);
  return p;
}

function basePath(u: string | undefined, fallback: string): string {
  const raw = u && u.trim().length ? u : fallback;
  return pathnameOf(raw);
}

const API_BASE = basePath(environment.apiUrl, '/api');
const MEDIA_BASE = basePath(environment.mediaUrl, '/api/media');

export function isStatic(u: string): boolean {
  const p = pathnameOf(u);
  return p.startsWith('/assets/');
}

export function isMedia(u: string): boolean {
  const p = pathnameOf(u);
  return p.startsWith(MEDIA_BASE);
}

export function isApi(u: string): boolean {
  const p = pathnameOf(u);
  return p.startsWith(API_BASE);
}

export function isAuth(u: string): boolean {
  const p = pathnameOf(u);
  return p.startsWith(API_BASE + '/auth/');
}
