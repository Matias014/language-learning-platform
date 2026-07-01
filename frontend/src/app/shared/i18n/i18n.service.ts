import {Injectable, inject, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {AbstractControl, FormGroup} from '@angular/forms';

type Dict = Record<string, any>;

@Injectable({providedIn: 'root'})
export class I18nService {
  private http = inject(HttpClient);
  private readonly KEY = 'lang';
  private readonly langs = ['pl', 'en'] as const;

  private langSig = signal<string>(this.initialLang());
  private dictSig = signal<Dict>({});

  constructor() {
    this.setHtmlLang(this.currentLang);
    this.load(this.currentLang);
  }

  get currentLang(): string {
    return this.langSig();
  }

  setLang(next: string): void {
    const lang = this.sanitize(next);
    if (lang === this.langSig()) return;
    this.langSig.set(lang);
    localStorage.setItem(this.KEY, lang);
    this.setHtmlLang(lang);
    this.load(lang);
  }

  t(path: string, params?: Record<string, unknown>): string {
    const dict = this.dictSig();
    const parts = path.split('.');
    let cur: any = dict;
    for (const p of parts) cur = cur?.[p];
    let val = typeof cur === 'string' ? cur : path;
    if (params && typeof val === 'string') {
      for (const [k, v] of Object.entries(params)) {
        val = val.replace(new RegExp(`{${k}}`, 'g'), String(v));
      }
    }
    return val;
  }

  getServerErrorCode(error: any): string | null {
    return this.extractServerErrorCode(error);
  }

  getServerErrorMessage(error: any, fallbackKey?: string): string | null {
    const code = this.extractServerErrorCode(error);
    if (code) {
      const key = `server.${code}`;
      const translated = this.t(key);
      return translated !== key ? translated : fallbackKey ? this.t(fallbackKey) : null;
    }
    return fallbackKey ? this.t(fallbackKey) : this.t('errors.default');
  }

  getFieldErrors(error: any): Record<string, string[]> {
    const out: Record<string, string[]> = {};
    const fields = this.extractProblemFields(error);
    for (const [field, codes] of Object.entries(fields)) {
      const arr = Array.isArray(codes) ? codes : [codes];
      const msgs = arr
        .map(c => String(c))
        .map(c => this.translateServerCode(c));
      out[field] = msgs.length ? msgs : [this.t('errors.badRequest')];
    }
    return out;
  }

  applyFieldErrors(form: FormGroup, error: any, alias?: Record<string, string>): boolean {
    const fieldErrors = this.getFieldErrors(error);
    let applied = false;
    for (const [field, messages] of Object.entries(fieldErrors)) {
      const key = alias?.[field] ?? field;
      const ctrl = form.get(key);
      if (ctrl) {
        const prev = ctrl.errors || {};
        (prev as any).server = messages;
        ctrl.setErrors(prev);
        ctrl.markAsTouched();
        applied = true;
      }
    }
    return applied;
  }

  load(lang: string = this.currentLang): void {
    const l = this.sanitize(lang);
    this.http.get<Dict>(`/assets/i18n/${l}.json`).subscribe({
      next: d => this.dictSig.set(d),
      error: () => this.dictSig.set({}),
    });
  }

  private initialLang(): string {
    const saved = localStorage.getItem(this.KEY);
    if (saved) return this.sanitize(saved);
    const nav = typeof navigator !== 'undefined' ? navigator.language : 'en';
    return this.sanitize(nav.startsWith('pl') ? 'pl' : 'en');
  }

  private sanitize(lang: string): string {
    const lc = String(lang).toLowerCase().slice(0, 2);
    return (this.langs as readonly string[]).includes(lc) ? lc : 'en';
  }

  private setHtmlLang(lang: string): void {
    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('lang', lang);
    }
  }

  private extractServerErrorCode(error: any): string | null {
    const raw = error?.error ?? error;
    const code = raw?.code;
    return code ? String(code).toUpperCase() : null;
  }

  private extractProblemFields(error: any): Record<string, string | string[]> {
    const raw = error?.error ?? error;
    const f = raw?.fields;
    if (!f || typeof f !== 'object') return {};
    return f as Record<string, string | string[]>;
  }

  private translateServerCode(code: string): string {
    const key = `server.${String(code || '').toUpperCase()}`;
    const translated = this.t(key);
    return translated !== key ? translated : code;
  }
}
