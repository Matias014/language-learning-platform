import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule, FormBuilder, Validators} from '@angular/forms';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {AdminLevelsService} from './admin-levels.service';
import {Level} from '@shared/models/user-level.model';
import {I18nService} from '@shared/i18n/i18n.service';
import {finalize} from 'rxjs/operators';

type SortDir = 'asc' | 'desc';
type SortProp = 'level' | 'requiredXp';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-levels',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TPipe],
  templateUrl: './levels.component.html',
  styleUrls: ['./levels.component.scss'],
})
export class AdminLevelsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(AdminLevelsService);
  private i18n = inject(I18nService);

  loading = signal(false);
  error = signal<string | null>(null);

  baseRows = signal<Level[]>([]);
  filterMinLevel = signal<number | null>(null);
  filterMinXp = signal<number | null>(null);

  editingLevel = signal<number | null>(null);
  isCreate = computed(() => this.editingLevel() === 0);

  form = this.fb.nonNullable.group({
    level: [1, [Validators.required, Validators.min(1)]],
    requiredXp: [0, [Validators.required, Validators.min(0)]],
  });

  sortProp = signal<SortProp>('level');
  sortDir = signal<SortDir>('asc');

  page = signal(0);
  size = signal(10);

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  ngOnInit() {
    this.loadAll();
  }

  private loadAll() {
    this.error.set(null);
    this.loading.set(true);
    this.api.list().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: list => {
        this.baseRows.set(list ?? []);
        this.page.set(0);
      },
      error: err => {
        const msg = this.i18n.getServerErrorMessage(err, 'common.loadFailed') || '';
        this.error.set('common.loadFailed');
        if (msg) this.showError(msg);
      },
    });
  }

  private filteredRows = computed(() => {
    const minL = this.filterMinLevel();
    const minXp = this.filterMinXp();
    return this.baseRows().filter(l => {
      const byL = minL == null || l.level >= minL;
      const byXp = minXp == null || l.requiredXp >= minXp;
      return byL && byXp;
    });
  });

  private sortedRows = computed(() => {
    const prop = this.sortProp();
    const dir = this.sortDir();
    const mul = dir === 'asc' ? 1 : -1;
    return [...this.filteredRows()].sort((a, b) => {
      const va = (a as any)[prop] as number;
      const vb = (b as any)[prop] as number;
      return mul * (va - vb);
    });
  });

  totalElements = computed(() => this.sortedRows().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
  });

  pageRows = computed(() => {
    const start = this.page() * this.size();
    const end = start + this.size();
    return this.sortedRows().slice(start, end);
  });

  onMinLevelChange(v: any) {
    const n = v === '' || v == null ? null : Number(v);
    this.filterMinLevel.set(Number.isFinite(n as number) ? (n as number) : null);
    this.page.set(0);
  }

  onMinXpChange(v: any) {
    const n = v === '' || v == null ? null : Number(v);
    this.filterMinXp.set(Number.isFinite(n as number) ? (n as number) : null);
    this.page.set(0);
  }

  clearFilters() {
    this.filterMinLevel.set(null);
    this.filterMinXp.set(null);
    this.page.set(0);
  }

  addNew() {
    this.editingLevel.set(0);
    this.form.enable();
    this.form.reset({level: 1, requiredXp: 0});
  }

  edit(l: Level) {
    this.editingLevel.set(l.level);
    this.form.reset({level: l.level, requiredXp: l.requiredXp});
    this.form.controls.level.disable();
  }

  cancel() {
    this.editingLevel.set(null);
    this.form.enable();
    this.form.reset();
    this.error.set(null);
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.editingLevel();
    const v = this.form.getRawValue();
    if (id === 0) {
      this.api.create({level: Number(v.level), requiredXp: Number(v.requiredXp)}).subscribe({
        next: () => {
          this.showSuccess(this.i18n.t('admin.levels.alerts.created'));
          this.cancel();
          this.loadAll();
        },
        error: err => {
          if (!this.i18n.applyFieldErrors(this.form, err)) {
            const msg = this.i18n.getServerErrorMessage(err, 'common.saveFailed') || '';
            this.error.set('common.saveFailed');
            if (msg) this.showError(msg);
          }
        },
      });
    } else if (id) {
      this.api.update(id, {requiredXp: Number(v.requiredXp)}).subscribe({
        next: () => {
          this.showSuccess(this.i18n.t('admin.levels.alerts.updated'));
          this.cancel();
          this.loadAll();
        },
        error: err => {
          if (!this.i18n.applyFieldErrors(this.form, err)) {
            const msg = this.i18n.getServerErrorMessage(err, 'common.saveFailed') || '';
            this.error.set('common.saveFailed');
            if (msg) this.showError(msg);
          }
        },
      });
    }
  }

  remove(l: Level) {
    const msg = this.i18n.t('admin.levels.confirmDelete');
    if (!confirm(msg)) return;
    this.api.delete(l.level).subscribe({
      next: () => {
        this.showSuccess(this.i18n.t('admin.levels.alerts.deleted'));
        this.loadAll();
      },
      error: err => {
        const m = this.i18n.getServerErrorMessage(err, 'common.deleteFailed') || '';
        this.error.set('common.deleteFailed');
        if (m) this.showError(m);
      },
    });
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

  trackByLevel = (_: number, l: Level) => l.level;

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
