import {Component, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ReactiveFormsModule, FormBuilder, Validators} from '@angular/forms';
import {FormsModule} from '@angular/forms';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {AdminProficiencyLevelsService} from './admin-proficiency-levels.service';
import {ProficiencyLevel, CreateProficiencyLevelRequest, UpdateProficiencyLevelRequest} from '@shared/models';
import {I18nService} from '@shared/i18n/i18n.service';
import {finalize} from 'rxjs/operators';

type SortDir = 'asc' | 'desc';
type SortProp = 'code' | 'name' | 'orderNumber';
type ToastType = 'success' | 'error';

@Component({
  selector: 'app-admin-proficiency-levels',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, TPipe],
  templateUrl: './proficiency-levels.component.html',
  styleUrls: ['./proficiency-levels.component.scss'],
})
export class AdminProficiencyLevelsComponent {
  private service = inject(AdminProficiencyLevelsService);
  private fb = inject(FormBuilder);
  private i18n = inject(I18nService);

  loading = signal(false);

  private data = signal<ProficiencyLevel[]>([]);
  rowsAll = computed(() => this.data());

  filter = signal<string>('');

  sortProp = signal<SortProp>('orderNumber');
  sortDir = signal<SortDir>('asc');

  page = signal(0);
  size = signal(10);

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  totalElements = computed(() => this.sortedRows().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
  });

  private editing = signal<string | null>(null);
  editingCode = computed(() => this.editing());
  isCreate = computed(() => this.editing() === '');

  form = this.fb.group({
    code: this.fb.control<string>('', {
      validators: [Validators.required, Validators.pattern(/^(A1|A2|B1|B2|C1|C2)$/)],
      nonNullable: true,
    }),
    name: this.fb.control<string>('', {
      validators: [Validators.required, Validators.maxLength(50)],
      nonNullable: true,
    }),
    orderNumber: this.fb.control<number | null>(null, {
      validators: [Validators.required, Validators.min(1)],
    }),
  });

  ngOnInit() {
    this.load();
  }

  private applyModeValidators() {
    const create = this.isCreate();
    const codeCtrl = this.form.controls.code;
    const nameCtrl = this.form.controls.name;
    const orderCtrl = this.form.controls.orderNumber;

    codeCtrl.setValidators(create ? [Validators.required, Validators.pattern(/^(A1|A2|B1|B2|C1|C2)$/)] : [Validators.pattern(/^(A1|A2|B1|B2|C1|C2)$/)]);
    nameCtrl.setValidators(create ? [Validators.required, Validators.maxLength(50)] : [Validators.maxLength(50)]);
    orderCtrl.setValidators(create ? [Validators.required, Validators.min(1)] : [Validators.min(1)]);

    codeCtrl.updateValueAndValidity();
    nameCtrl.updateValueAndValidity();
    orderCtrl.updateValueAndValidity();
  }

  load() {
    this.loading.set(true);
    this.service.list().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: res => {
        this.data.set(res || []);
        this.page.set(0);
      },
      error: e => {
        const msg = this.i18n.getServerErrorMessage(e, 'admin.proficiencyLevels.errors.load') || '';
        if (msg) this.showError(msg);
        this.data.set([]);
      }
    });
  }

  private compareValues(a: unknown, b: unknown, _prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (typeof a === 'number' && typeof b === 'number') return a - b;
    const as = String(a).toLowerCase();
    const bs = String(b).toLowerCase();
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

  private filteredRows = computed(() => {
    const term = this.filter().trim().toLowerCase();
    if (!term) return this.rowsAll();
    return this.rowsAll().filter(r =>
      r.code.toLowerCase().includes(term) || (r.name || '').toLowerCase().includes(term)
    );
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

  onFilterChange(v: any) {
    this.filter.set(v ?? '');
    this.page.set(0);
  }

  clearFilters() {
    this.filter.set('');
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

  addNew() {
    this.editing.set('');
    this.form.reset();
    this.form.patchValue({code: '', name: '', orderNumber: 1});
    this.applyModeValidators();
  }

  edit(item: ProficiencyLevel) {
    this.editing.set(item.code);
    this.form.reset();
    this.form.patchValue({
      code: item.code,
      name: item.name,
      orderNumber: item.orderNumber,
    });
    this.applyModeValidators();
  }

  cancel() {
    this.editing.set(null);
    this.form.reset();
  }

  save() {
    if (this.form.invalid) return;

    const code = this.form.value.code ?? '';
    const name = this.form.value.name ?? '';
    const orderNumber = this.form.value.orderNumber as number;

    if (this.isCreate()) {
      const body: CreateProficiencyLevelRequest = {code, name, orderNumber};
      this.service.create(body).subscribe({
        next: () => {
          this.showSuccess(this.i18n.t('admin.proficiencyLevels.alerts.created'));
          this.load();
          this.cancel();
        },
        error: err => {
          const applied = this.i18n.applyFieldErrors(this.form, err);
          if (!applied) {
            const msg = this.i18n.getServerErrorMessage(err, 'admin.proficiencyLevels.errors.create') || '';
            if (msg) this.showError(msg);
          }
        },
      });
    } else {
      const patch: UpdateProficiencyLevelRequest = {name, orderNumber};
      const codeParam = this.editing() as string;
      this.service.update(codeParam, patch).subscribe({
        next: () => {
          this.showSuccess(this.i18n.t('admin.proficiencyLevels.alerts.updated'));
          this.load();
          this.cancel();
        },
        error: err => {
          const applied = this.i18n.applyFieldErrors(this.form, err);
          if (!applied) {
            const msg = this.i18n.getServerErrorMessage(err, 'admin.proficiencyLevels.errors.update') || '';
            if (msg) this.showError(msg);
          }
        },
      });
    }
  }

  remove(code: string) {
    const ok = confirm(this.i18n.t('admin.proficiencyLevels.confirmDelete'));
    if (!ok) return;
    this.service.delete(code).subscribe({
      next: () => {
        this.showSuccess(this.i18n.t('admin.proficiencyLevels.alerts.deleted'));
        this.load();
      },
      error: err => {
        const msg = this.i18n.getServerErrorMessage(err, 'admin.proficiencyLevels.errors.delete') || '';
        if (msg) this.showError(msg);
      },
    });
  }

  trackByCode(_: number, item: ProficiencyLevel) {
    return item.code;
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
