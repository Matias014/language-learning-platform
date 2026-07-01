import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {FormsModule} from '@angular/forms';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {AdminLanguagesService} from './admin-languages.service';
import {Language} from '@shared/models';
import {I18nService} from '@shared/i18n/i18n.service';

type SortDir = 'asc' | 'desc';
type SortProp = 'code' | 'name';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-languages',
  imports: [CommonModule, ReactiveFormsModule, FormsModule, TPipe],
  templateUrl: './languages.component.html',
  styleUrls: ['./languages.component.scss'],
})
export class AdminLanguagesComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(AdminLanguagesService);
  private i18n = inject(I18nService);

  loading = signal(false);
  rows = signal<Language[]>([]);
  filter = signal<string>('');

  page = signal(0);
  size = signal(10);

  sortProp = signal<SortProp>('code');
  sortDir = signal<SortDir>('asc');

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  editingCode = signal<string | null>(null);
  isCreate = computed(() => this.editingCode() === '');

  private readonly LANG_RE = /^[a-z]{2,3}(-[A-Z]{2})?$/;

  form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(this.LANG_RE)]],
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: list => {
        this.rows.set(list ?? []);
        this.loading.set(false);
      },
      error: e => {
        this.loading.set(false);
        const msg = this.i18n.getServerErrorMessage(e, 'admin.languages.errors.load') || '';
        if (msg) this.showError(msg);
        this.rows.set([]);
      },
    });
  }

  trackByCode = (_: number, l: Language) => l.code;

  addNew(): void {
    this.editingCode.set('');
    this.form.reset({code: '', name: ''});
    this.form.markAsPristine();
  }

  edit(l: Language): void {
    this.editingCode.set(l.code);
    this.form.reset({code: l.code, name: l.name});
    this.form.get('code')?.disable({emitEvent: false});
  }

  cancel(): void {
    this.editingCode.set(null);
    this.form.reset();
    this.form.get('code')?.enable({emitEvent: false});
  }

  private normalizeCode(v: string): string {
    if (!v) return '';
    const parts = v.trim().split('-', 2);
    if (parts.length === 1) return parts[0].toLowerCase();
    return `${parts[0].toLowerCase()}-${parts[1].toUpperCase()}`;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const codeNorm = this.normalizeCode(raw.code);
    const nameNorm = String(raw.name).trim();

    const editing = this.editingCode();
    const reload = () => this.load();

    if (editing === '') {
      this.api.create({code: codeNorm, name: nameNorm}).subscribe({
        next: () => {
          this.showSuccess(this.i18n.t('admin.languages.alerts.created'));
          this.cancel();
          reload();
        },
        error: err => {
          const applied = this.i18n.applyFieldErrors(this.form, err);
          const msg = this.i18n.getServerErrorMessage(err, 'admin.languages.errors.create') || '';
          if (msg) this.showError(msg);
          if (!applied) this.form.setErrors({server: true});
        },
      });
    } else if (editing) {
      this.api.update(editing, {name: nameNorm}).subscribe({
        next: () => {
          this.showSuccess(this.i18n.t('admin.languages.alerts.updated'));
          this.cancel();
          reload();
        },
        error: err => {
          const applied = this.i18n.applyFieldErrors(this.form, err, {name: 'name'});
          const msg = this.i18n.getServerErrorMessage(err, 'admin.languages.errors.update') || '';
          if (msg) this.showError(msg);
          if (!applied) this.form.setErrors({server: true});
        },
      });
    }
  }

  remove(code: string): void {
    const msg = this.i18n.t('admin.languages.confirmDelete');
    if (!confirm(msg)) return;
    this.api.delete(code).subscribe({
      next: () => {
        this.showSuccess(this.i18n.t('admin.languages.alerts.deleted'));
        this.load();
      },
      error: e => {
        const msg = this.i18n.getServerErrorMessage(e, 'admin.languages.errors.delete') || '';
        if (msg) this.showError(msg);
        this.load();
      },
    });
  }

  onFilterChange(v: string) {
    this.filter.set(String(v || '').trim());
    this.page.set(0);
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

  changeSize(size: number) {
    const s = Number(size);
    this.size.set([5, 10, 20, 50].includes(s) ? s : 10);
    this.page.set(0);
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

  private compare(a: unknown, b: unknown) {
    const as = String(a ?? '');
    const bs = String(b ?? '');
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

  private matchesFilter(l: Language, q: string): boolean {
    if (!q) return true;
    const s = q.toLowerCase();
    return l.code.toLowerCase().includes(s) || l.name.toLowerCase().includes(s);
  }

  private filteredRows = computed(() => {
    const q = this.filter();
    return this.rows().filter(r => this.matchesFilter(r, q));
  });

  private sortedRows = computed(() => {
    const prop = this.sortProp();
    const dir = this.sortDir();
    const mul = dir === 'asc' ? 1 : -1;
    return [...this.filteredRows()].sort((x, y) => mul * this.compare((x as any)[prop], (y as any)[prop]));
  });

  pageRows = computed(() => {
    const start = this.page() * this.size();
    const end = start + this.size();
    return this.sortedRows().slice(start, end);
  });

  totalElements = computed(() => this.filteredRows().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
  });

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
