import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {AdminLlmLogsService} from './admin-llm-logs.service';
import {finalize} from 'rxjs';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';
import {LlmLog, InteractionType} from '@shared/models';

type SortDir = 'asc' | 'desc';
type SortProp =
  'id'
  | 'userId'
  | 'lessonId'
  | 'exerciseAttemptId'
  | 'chatSessionId'
  | 'interactionType'
  | 'model'
  | 'status'
  | 'tokensIn'
  | 'tokensOut'
  | 'latencyMs'
  | 'createdAt';

type ToastType = 'success' | 'error';

@Component({
  selector: 'app-admin-llm-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, TPipe],
  templateUrl: './llm-logs.component.html',
  styleUrls: ['./llm-logs.component.scss']
})
export class AdminLlmLogsComponent implements OnInit {
  private api = inject(AdminLlmLogsService);
  private i18n = inject(I18nService);

  loading = signal(false);
  error = signal<string | null>(null);

  baseRows = signal<LlmLog[]>([]);
  filterUserId = signal<string>('');
  filterLessonId = signal<string>('');
  filterAttemptId = signal<string>('');
  filterSessionId = signal<string>('');
  filterType = signal<InteractionType | ''>('');

  page = signal(0);
  size = signal(10);
  sortProp = signal<SortProp>('createdAt');
  sortDir = signal<SortDir>('desc');

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  ngOnInit(): void {
    this.applyFilters();
  }

  private parseNum(v: string): number | null {
    const t = (v || '').trim();
    if (!t) return null;
    const n = Number(t);
    return Number.isFinite(n) && n > 0 ? n : null;
  }

  onUserIdChange(v: any) {
    this.filterUserId.set(String(v ?? ''));
    this.page.set(0);
    this.applyFilters();
  }

  onLessonIdChange(v: any) {
    this.filterLessonId.set(String(v ?? ''));
    this.page.set(0);
    this.applyFilters();
  }

  onAttemptIdChange(v: any) {
    this.filterAttemptId.set(String(v ?? ''));
    this.page.set(0);
    this.applyFilters();
  }

  onSessionIdChange(v: any) {
    this.filterSessionId.set(String(v ?? ''));
    this.page.set(0);
    this.applyFilters();
  }

  onTypeChange(v: any) {
    this.filterType.set((v ?? '') as InteractionType | '');
    this.page.set(0);
  }

  clearFilters() {
    this.filterUserId.set('');
    this.filterLessonId.set('');
    this.filterAttemptId.set('');
    this.filterSessionId.set('');
    this.filterType.set('');
    this.page.set(0);
    this.applyFilters();
  }

  private fetchAll() {
    this.error.set(null);
    this.loading.set(true);
    this.api.listAll().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: data => {
        this.baseRows.set(data ?? []);
      },
      error: e => {
        const msg = this.i18n.getServerErrorMessage(e, 'admin.llmLogs.errors.load') || '';
        this.error.set('admin.llmLogs.errors.load');
        if (msg) this.showError(msg);
        this.baseRows.set([]);
      }
    });
  }

  private applyFilters() {
    const uid = this.parseNum(this.filterUserId());
    const lid = this.parseNum(this.filterLessonId());
    const aid = this.parseNum(this.filterAttemptId());
    const sid = this.parseNum(this.filterSessionId());

    this.error.set(null);

    if (aid != null) {
      this.loading.set(true);
      this.api.byAttempt(aid).pipe(finalize(() => this.loading.set(false))).subscribe({
        next: data => this.baseRows.set(data ?? []),
        error: e => {
          const msg = this.i18n.getServerErrorMessage(e, 'admin.llmLogs.errors.load') || '';
          this.error.set('admin.llmLogs.errors.load');
          if (msg) this.showError(msg);
          this.baseRows.set([]);
        }
      });
      return;
    }

    if (sid != null) {
      this.loading.set(true);
      this.api.bySession(sid).pipe(finalize(() => this.loading.set(false))).subscribe({
        next: data => this.baseRows.set(data ?? []),
        error: e => {
          const msg = this.i18n.getServerErrorMessage(e, 'admin.llmLogs.errors.load') || '';
          this.error.set('admin.llmLogs.errors.load');
          if (msg) this.showError(msg);
          this.baseRows.set([]);
        }
      });
      return;
    }

    if (lid != null) {
      this.loading.set(true);
      this.api.byLesson(lid).pipe(finalize(() => this.loading.set(false))).subscribe({
        next: data => this.baseRows.set(data ?? []),
        error: e => {
          const msg = this.i18n.getServerErrorMessage(e, 'admin.llmLogs.errors.load') || '';
          this.error.set('admin.llmLogs.errors.load');
          if (msg) this.showError(msg);
          this.baseRows.set([]);
        }
      });
      return;
    }

    if (uid != null) {
      this.loading.set(true);
      this.api.byUser(uid).pipe(finalize(() => this.loading.set(false))).subscribe({
        next: data => this.baseRows.set(data ?? []),
        error: e => {
          const msg = this.i18n.getServerErrorMessage(e, 'admin.llmLogs.errors.load') || '';
          this.error.set('admin.llmLogs.errors.load');
          if (msg) this.showError(msg);
          this.baseRows.set([]);
        }
      });
      return;
    }

    this.fetchAll();
  }

  private compare(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'createdAt') {
      const an = typeof a === 'string' ? Date.parse(a) : 0;
      const bn = typeof b === 'string' ? Date.parse(b) : 0;
      return an - bn;
    }
    if (typeof a === 'number' && typeof b === 'number') return a - b;
    const as = String(a);
    const bs = String(b);
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

  private filtered = computed(() => {
    const typ = this.filterType();
    return this.baseRows().filter(r => {
      if (typ && r.interactionType !== typ) return false;
      return true;
    });
  });

  private sorted = computed(() => {
    const prop = this.sortProp();
    const dir = this.sortDir();
    const mul = dir === 'asc' ? 1 : -1;
    return [...this.filtered()].sort((x, y) => mul * this.compare((x as any)[prop], (y as any)[prop], prop));
  });

  totalElements = computed(() => this.sorted().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
  });

  pageRows = computed(() => {
    const start = this.page() * this.size();
    const end = start + this.size();
    return this.sorted().slice(start, end);
  });

  toggleSort(prop: SortProp) {
    if (this.sortProp() === prop) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortProp.set(prop);
      this.sortDir.set('asc');
    }
    this.page.set(0);
  }

  sortArrow(prop: SortProp) {
    if (this.sortProp() !== prop) return '';
    return this.sortDir() === 'asc' ? '▲' : '▼';
  }

  canPrev() {
    return this.page() > 0;
  }

  canNext() {
    return this.page() + 1 < this.totalPages();
  }

  prev() {
    if (!this.canPrev()) return;
    this.page.set(this.page() - 1);
  }

  next() {
    if (!this.canNext()) return;
    this.page.set(this.page() + 1);
  }

  changeSize(size: number) {
    const s = Number(size);
    this.size.set([5, 10, 20, 50].includes(s) ? s : 10);
    this.page.set(0);
  }

  trackById = (_: number, r: LlmLog) => r.id;

  selected: LlmLog | null = null;

  showDetails(r: LlmLog) {
    this.selected = r;
    const modal = document.getElementById('logModal');
    if (modal && (window as any).bootstrap?.Modal) {
      new (window as any).bootstrap.Modal(modal).show();
    }
  }

  private showToast(text: string, type: ToastType) {
    const id = ++this.toastSeq;
    this.toasts.set([{id, text, type}, ...this.toasts()]);
    setTimeout(() => this.dismissToast(id), 4000);
  }

  showError(text: string) {
    if (!text) return;
    this.showToast(text, 'error');
  }

  dismissToast(id: number) {
    this.toasts.set(this.toasts().filter(t => t.id !== id));
  }

  trackToast(_i: number, t: { id: number }) {
    return t.id;
  }
}
