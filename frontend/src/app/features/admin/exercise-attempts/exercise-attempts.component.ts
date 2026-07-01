import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {I18nService} from '@app/shared/i18n/i18n.service';
import {AdminExerciseAttemptsService} from './admin-exercise-attempts.service';
import {ExerciseAttempt} from '@shared/models';
import {finalize} from 'rxjs/operators';

type SortDir = 'asc' | 'desc';
type SortProp =
  'id'
  | 'userId'
  | 'exerciseId'
  | 'attemptNumber'
  | 'correct'
  | 'score'
  | 'submittedAt'
  | 'durationSeconds';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-exercise-attempts',
  imports: [CommonModule, FormsModule, TPipe, DatePipe],
  templateUrl: './exercise-attempts.component.html',
  styleUrls: ['./exercise-attempts.component.scss'],
})
export class AdminAttemptsComponent implements OnInit {
  private api = inject(AdminExerciseAttemptsService);
  private i18n = inject(I18nService);

  loading = signal(false);
  error = signal<string | null>(null);

  rows = signal<ExerciseAttempt[]>([]);

  filterUserId = signal<string>('');
  filterExerciseId = signal<string>('');
  filterCorrect = signal<string>('');

  page = signal(0);
  size = signal(10);

  sortProp = signal<SortProp>('submittedAt');
  sortDir = signal<SortDir>('desc');

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  totalElements = computed(() => this.filteredRows().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
  });

  ngOnInit(): void {
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.listAll().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: data => {
        this.rows.set(data ?? []);
        this.page.set(0);
      },
      error: e => {
        if (e?.status === 404) {
          this.rows.set([]);
          this.page.set(0);
          this.error.set(null);
          return;
        }
        const msg = this.i18n.getServerErrorMessage(e, 'common.loadFailed') || '';
        if (msg) this.showError(msg);
        this.rows.set([]);
        this.page.set(0);
        this.error.set(null);
      }
    });
  }

  private compareValues(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'submittedAt') {
      const an = typeof a === 'string' ? Date.parse(a) : 0;
      const bn = typeof b === 'string' ? Date.parse(b) : 0;
      return an - bn;
    }
    if (typeof a === 'number' && typeof b === 'number') return a - b;
    if (typeof a === 'boolean' && typeof b === 'boolean') return (a === b) ? 0 : a ? 1 : -1;
    const as = String(a);
    const bs = String(b);
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

  private filteredRows = computed(() => {
    const u = this.filterUserId().trim();
    const e = this.filterExerciseId().trim();
    const c = this.filterCorrect().trim();
    return this.rows().filter(r => {
      const byU = !u || r.userId === Number(u);
      const byE = !e || r.exerciseId === Number(e);
      const byC = !c || String(r.correct) === c;
      return byU && byE && byC;
    });
  });

  private sortedRows = computed(() => {
    const prop = this.sortProp();
    const dir = this.sortDir();
    const mul = dir === 'asc' ? 1 : -1;
    return [...this.filteredRows()].sort((x, y) => mul * this.compareValues((x as any)[prop], (y as any)[prop], prop));
  });

  pageRows = computed(() => {
    const start = this.page() * this.size();
    const end = start + this.size();
    return this.sortedRows().slice(start, end);
  });

  onUserIdChange(v: any) {
    this.filterUserId.set(String(v ?? '').trim());
    this.page.set(0);
    this.applyFilters();
  }

  onExerciseIdChange(v: any) {
    this.filterExerciseId.set(String(v ?? '').trim());
    this.page.set(0);
    this.applyFilters();
  }

  onCorrectChange(v: string) {
    this.filterCorrect.set(v ?? '');
    this.page.set(0);
  }

  applyFilters() {
    const eRaw = this.filterExerciseId().trim();
    const uRaw = this.filterUserId().trim();
    const eNum = eRaw ? Number(eRaw) : NaN;
    const uNum = uRaw ? Number(uRaw) : NaN;

    if (!eRaw || !Number.isFinite(eNum) || eNum < 1) {
      this.loadAll();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const req$ = Number.isFinite(uNum) && uNum >= 1
      ? this.api.listByUserAndExercise(uNum, eNum)
      : this.api.listByExercise(eNum);

    req$.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: data => {
        this.rows.set(data ?? []);
        this.page.set(0);
      },
      error: e => {
        if (e?.status === 404) {
          this.rows.set([]);
          this.page.set(0);
          this.error.set(null);
          return;
        }
        const msg = this.i18n.getServerErrorMessage(e, 'common.loadFailed') || '';
        if (msg) this.showError(msg);
        this.rows.set([]);
        this.page.set(0);
        this.error.set(null);
      }
    });
  }

  clearFilters() {
    this.filterUserId.set('');
    this.filterExerciseId.set('');
    this.filterCorrect.set('');
    this.page.set(0);
    this.size.set(10);
    this.sortProp.set('submittedAt');
    this.sortDir.set('desc');
    this.loadAll();
  }

  toggleSort(prop: SortProp) {
    if (this.sortProp() === prop) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortProp.set(prop);
      this.sortDir.set(prop === 'submittedAt' ? 'desc' : 'asc');
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

  remove(id: number) {
    if (!confirm(this.i18n.t('admin.attempts.confirmDelete'))) return;
    this.api.delete(id).subscribe({
      next: () => this.applyFilters(),
      error: e => {
        if (e?.status === 404) {
          this.applyFilters();
          return;
        }
        const msg = this.i18n.getServerErrorMessage(e, 'common.deleteFailed') || '';
        if (msg) this.showError(msg);
        this.applyFilters();
      }
    });
  }

  trackById = (_: number, r: ExerciseAttempt) => r.id;

  asPercent(score: number | null): string {
    return score === null ? '—' : `${Math.round(score)}%`;
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
