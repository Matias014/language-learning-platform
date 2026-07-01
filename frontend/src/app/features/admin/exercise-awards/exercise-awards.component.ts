import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {I18nService} from '@app/shared/i18n/i18n.service';
import {AdminExerciseAwardsService} from './admin-exercise-awards.service';
import {ExerciseAward} from '@app/shared/models/exercise-award.model';
import {finalize} from 'rxjs/operators';

type SortDir = 'asc' | 'desc';
type SortProp = 'id' | 'attemptId' | 'awardedXp' | 'awardedAt';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-exercise-awards',
  imports: [CommonModule, FormsModule, TPipe],
  templateUrl: './exercise-awards.component.html',
  styleUrls: ['./exercise-awards.component.scss'],
})
export class AdminAwardsComponent implements OnInit {
  private api = inject(AdminExerciseAwardsService);
  private i18n = inject(I18nService);

  loading = signal(false);
  error = signal<string | null>(null);

  rows = signal<ExerciseAward[]>([]);
  filterUserId = signal<string>('');
  filterAttemptId = signal<string>('');

  page = signal(0);
  size = signal(10);
  sortProp = signal<SortProp>('id');
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
        const msg = this.i18n.getServerErrorMessage(e, 'admin.awards.errors.load') || '';
        if (msg) this.showError(msg);
        this.rows.set([]);
        this.page.set(0);
        this.error.set(null);
      }
    });
  }

  private filteredRows = computed(() => {
    const aRaw = String(this.filterAttemptId() ?? '').trim();
    const aNum = aRaw ? Number(aRaw) : NaN;
    if (!aRaw || !Number.isFinite(aNum) || aNum < 1) return this.rows();
    return this.rows().filter(x => x.attemptId === aNum);
  });

  private compareValues(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'awardedAt') {
      const an = typeof a === 'string' ? Date.parse(a) : 0;
      const bn = typeof b === 'string' ? Date.parse(b) : 0;
      return an - bn;
    }
    if (typeof a === 'number' && typeof b === 'number') return a - b;
    const as = String(a);
    const bs = String(b);
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

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

  onUserIdChange(v: any): void {
    this.filterUserId.set(String(v ?? ''));
    this.page.set(0);
    this.applyFilters();
  }

  onAttemptIdChange(v: any): void {
    this.filterAttemptId.set(String(v ?? ''));
    this.page.set(0);
  }

  applyFilters(): void {
    const uRaw = String(this.filterUserId() ?? '').trim();
    const uNum = uRaw ? Number(uRaw) : NaN;

    if (!uRaw || !Number.isFinite(uNum) || uNum < 1) {
      this.loadAll();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.api.listByUser(uNum).pipe(finalize(() => this.loading.set(false))).subscribe({
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
        const msg = this.i18n.getServerErrorMessage(e, 'admin.awards.errors.load') || '';
        if (msg) this.showError(msg);
        this.rows.set([]);
        this.page.set(0);
        this.error.set(null);
      }
    });
  }

  clearFilters(): void {
    this.filterUserId.set('');
    this.filterAttemptId.set('');
    this.page.set(0);
    this.size.set(10);
    this.sortProp.set('id');
    this.sortDir.set('desc');
    this.loadAll();
  }

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

  trackById = (_: number, r: ExerciseAward) => r.id;

  private showToast(text: string, type: ToastType) {
    const id = ++this.toastSeq;
    this.toasts.set([{id, text, type}, ...this.toasts()]);
    setTimeout(() => this.dismissToast(id), 4000);
  }

  showError(text: string) {
    if (!text) return;
    this.showToast(text, 'error');
  }

  showSuccess(text: string) {
    if (!text) return;
    this.showToast(text, 'success');
  }

  dismissToast(id: number) {
    this.toasts.set(this.toasts().filter(t => t.id !== id));
  }

  trackToast(_i: number, t: { id: number }) {
    return t.id;
  }
}
