import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {I18nService} from '@app/shared/i18n/i18n.service';
import {AdminUserAchievementsService} from './admin-user-achievements.service';
import {UserAchievement} from '@app/shared/models';
import {finalize} from 'rxjs/operators';

type SortDir = 'asc' | 'desc';
type SortProp = 'id' | 'userId' | 'achievementId' | 'earnedAt';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-user-achievements',
  imports: [CommonModule, FormsModule, TPipe],
  templateUrl: './user-achievements.component.html',
  styleUrls: ['./user-achievements.component.scss'],
})
export class AdminUserAchievementsComponent implements OnInit {
  private api = inject(AdminUserAchievementsService);
  private i18n = inject(I18nService);

  loading = signal(false);
  error = signal<string | null>(null);

  rows = signal<UserAchievement[]>([]);

  filterUserId = signal<string>('');
  filterAchievementId = signal<string>('');

  page = signal(0);
  size = signal(10);

  sortProp = signal<SortProp>('id');
  sortDir = signal<SortDir>('desc');

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  totalElements = computed(() => this.rows().length);
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
        this.rows.set([]);
        this.page.set(0);
        this.error.set(null);
        const msg = this.i18n.getServerErrorMessage(e, 'admin.userAchievements.errors.load') || '';
        if (msg) this.showError(msg);
      }
    });
  }

  private compareValues(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'earnedAt') {
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
    return [...this.rows()].sort((x, y) => mul * this.compareValues((x as any)[prop], (y as any)[prop], prop));
  });

  pageRows = computed(() => {
    const start = this.page() * this.size();
    const end = start + this.size();
    return this.sortedRows().slice(start, end);
  });

  onUserIdChange(v: any): void {
    this.filterUserId.set(v ?? '');
    this.page.set(0);
    this.applyFilters();
  }

  onAchievementIdChange(v: any): void {
    this.filterAchievementId.set(v ?? '');
    this.page.set(0);
    this.applyFilters();
  }

  applyFilters(): void {
    const uRaw = this.filterUserId().trim();
    const aRaw = this.filterAchievementId().trim();
    const u = uRaw ? Number(uRaw) : null;
    const a = aRaw ? Number(aRaw) : null;

    if (!u && !a) {
      this.loadAll();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    if (u && a) {
      this.api.getByUserAndAchievement(u, a).pipe(finalize(() => this.loading.set(false))).subscribe({
        next: dto => {
          this.rows.set(dto ? [dto] : []);
          this.page.set(0);
        },
        error: e => {
          if (e?.status === 404) {
            this.rows.set([]);
            this.page.set(0);
            this.error.set(null);
            return;
          }
          this.rows.set([]);
          this.page.set(0);
          this.error.set(null);
          const msg = this.i18n.getServerErrorMessage(e, 'admin.userAchievements.errors.load') || '';
          if (msg) this.showError(msg);
        }
      });
      return;
    }

    const req$ = u ? this.api.listByUser(u) : this.api.listByAchievement(a as number);
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
        this.rows.set([]);
        this.page.set(0);
        this.error.set(null);
        const msg = this.i18n.getServerErrorMessage(e, 'admin.userAchievements.errors.load') || '';
        if (msg) this.showError(msg);
      }
    });
  }

  clearFilters(): void {
    this.filterUserId.set('');
    this.filterAchievementId.set('');
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

  remove(id: number) {
    if (!confirm(this.i18n.t('admin.userAchievements.confirmDelete'))) return;
    this.loading.set(true);
    this.error.set(null);
    this.api.delete(id).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        this.showSuccess(this.i18n.t('admin.userAchievements.alerts.deleted'));
        this.applyFilters();
      },
      error: e => {
        if (e?.status === 404) {
          this.applyFilters();
          return;
        }
        const msg = this.i18n.getServerErrorMessage(e, 'admin.userAchievements.errors.delete') || '';
        if (msg) this.showError(msg);
      }
    });
  }

  trackById = (_: number, ua: UserAchievement) => ua.id;

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
