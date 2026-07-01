import {Component, inject, signal, computed} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule, FormBuilder, Validators} from '@angular/forms';
import {AdminCoursesService} from '../courses/admin-courses.service';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {AdminLessonsService} from './admin-lessons.service';
import type {Lesson} from '@app/shared/models/lesson.model';
import {Course} from '@app/shared/models/course.model';
import {I18nService} from '@app/shared/i18n/i18n.service';
import {UpdateLessonRequest, CreateLessonRequest} from '@app/shared/models/lesson.model';

type SortDir = 'asc' | 'desc';
type SortProp = 'id' | 'title' | 'orderNumber';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-lessons',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TPipe],
  templateUrl: './lessons.component.html',
  styleUrls: ['./lessons.component.scss'],
})
export class AdminLessonsComponent {
  private fb = inject(FormBuilder);
  private apiCourses = inject(AdminCoursesService);
  private api = inject(AdminLessonsService);
  private i18n = inject(I18nService);

  courses = signal<Course[]>([]);
  selectedCourseId = signal<number | null>(null);

  lessonsAll = signal<Lesson[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  filterId = signal<string>('');
  filterTitle = signal<string>('');
  filterOrder = signal<string>('');

  page = signal(0);
  size = signal(10);
  sortProp = signal<SortProp>('orderNumber');
  sortDir = signal<SortDir>('asc');

  editingId = signal<number | null>(null);
  isEdit = computed(() => (this.editingId() ?? 0) > 0);

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  form = this.fb.nonNullable.group({
    title: ['', [Validators.required]],
    description: [''],
    orderNumber: this.fb.nonNullable.control(1, [Validators.required, Validators.min(1)]),
  });

  ngOnInit() {
    this.apiCourses.list().subscribe({
      next: c => this.courses.set(c ?? []),
      error: e => this.showError(this.i18n.getServerErrorMessage(e, 'common.loadFailed') || ''),
    });
  }

  reload() {
    const cid = this.selectedCourseId();
    this.error.set(null);
    this.lessonsAll.set([]);
    if (!cid) return;
    this.loading.set(true);
    this.api.listByCourse(cid).subscribe({
      next: l => {
        this.lessonsAll.set(l ?? []);
        this.page.set(0);
        this.loading.set(false);
      },
      error: e => {
        this.lessonsAll.set([]);
        this.loading.set(false);
        this.error.set('common.loadFailed');
        this.showError(this.i18n.getServerErrorMessage(e, 'common.loadFailed') || '');
      },
    });
  }

  onFilterIdChange(v: any) {
    this.filterId.set((v ?? '').toString());
    this.page.set(0);
  }

  onFilterTitleChange(v: any) {
    this.filterTitle.set((v ?? '').toString());
    this.page.set(0);
  }

  onFilterOrderChange(v: any) {
    this.filterOrder.set((v ?? '').toString());
    this.page.set(0);
  }

  clearFilters() {
    this.filterId.set('');
    this.filterTitle.set('');
    this.filterOrder.set('');
    this.page.set(0);
  }

  private compareValues(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'id' || prop === 'orderNumber') {
      const an = Number(a);
      const bn = Number(b);
      if (Number.isFinite(an) && Number.isFinite(bn)) return an - bn;
    }
    const as = String(a).toLowerCase();
    const bs = String(b).toLowerCase();
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

  private filteredRows = computed(() => {
    const idRaw = this.filterId().trim();
    const titleRaw = this.filterTitle().trim().toLowerCase();
    const orderRaw = this.filterOrder().trim();
    const id = idRaw ? Number(idRaw) : null;
    const order = orderRaw ? Number(orderRaw) : null;
    return this.lessonsAll().filter(l => {
      if (id && l.id !== id) return false;
      if (titleRaw && !String(l.title || '').toLowerCase().includes(titleRaw)) return false;
      if (order && l.orderNumber !== order) return false;
      return true;
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

  totalElements = computed(() => this.filteredRows().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
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

  addNew() {
    this.editingId.set(0);
    this.form.reset({title: '', description: '', orderNumber: 1});
    this.error.set(null);
  }

  edit(l: Lesson) {
    this.editingId.set(l.id);
    this.form.reset({
      title: l.title ?? '',
      description: l.description ?? '',
      orderNumber: l.orderNumber ?? 1,
    });
    this.error.set(null);
  }

  cancel() {
    this.editingId.set(null);
    this.form.reset();
    this.error.set(null);
  }

  save() {
    if (this.form.invalid || !this.selectedCourseId()) return;
    const id = this.editingId();
    const cid = this.selectedCourseId()!;
    const v = this.form.getRawValue();
    const base = {
      courseId: cid,
      title: String(v.title).trim(),
      description: v.description?.trim() || null,
      orderNumber: Number(v.orderNumber),
    };

    if (!id || id === 0) {
      const body: CreateLessonRequest = base;
      this.api.create(body).subscribe({
        next: () => {
          this.showSuccess(this.i18n.t('admin.lessons.alerts.saved'));
          this.cancel();
          this.reload();
        },
        error: e => {
          const applied = this.i18n.applyFieldErrors(this.form, e);
          if (!applied) {
            const msg = this.i18n.getServerErrorMessage(e, 'common.saveFailed') || '';
            this.error.set('common.saveFailed');
            this.showError(msg || this.i18n.t('common.saveFailed'));
          }
        },
      });
    } else {
      const patch: UpdateLessonRequest = {
        title: base.title,
        description: base.description,
        orderNumber: base.orderNumber,
      };
      this.api.update(id, patch).subscribe({
        next: () => {
          this.showSuccess(this.i18n.t('admin.lessons.alerts.saved'));
          this.cancel();
          this.reload();
        },
        error: e => {
          const applied = this.i18n.applyFieldErrors(this.form, e);
          if (!applied) {
            const msg = this.i18n.getServerErrorMessage(e, 'common.saveFailed') || '';
            this.error.set('common.saveFailed');
            this.showError(msg || this.i18n.t('common.saveFailed'));
          }
        },
      });
    }
  }

  remove(id: number) {
    if (!confirm(this.i18n.t('admin.lessons.confirmDelete'))) return;
    this.loading.set(true);
    this.error.set(null);
    this.api.delete(id).subscribe({
      next: () => {
        this.loading.set(false);
        this.showSuccess(this.i18n.t('admin.lessons.alerts.deleted'));
        this.reload();
      },
      error: e => {
        this.loading.set(false);
        const msg = this.i18n.getServerErrorMessage(e, 'common.deleteFailed') || '';
        this.error.set('common.deleteFailed');
        this.showError(msg || this.i18n.t('common.deleteFailed'));
      },
    });
  }

  fieldErrors(name: string): string[] {
    const c = this.form.get(name);
    if (!c) return [];
    const e = c.errors || {};
    const out: string[] = [];
    if (e['required']) out.push(this.i18n.t('validation.required'));
    if (e['min']) out.push(this.i18n.t('validation.min', {min: e['min'].min}));
    const server = e['server'] as string[] | undefined;
    if (Array.isArray(server)) out.push(...server);
    return out;
  }

  isInvalid(name: string): boolean {
    const c = this.form.get(name);
    return !!c && c.touched && c.invalid;
  }

  trackById = (_: number, l: Lesson) => l.id;

  private showToast(text: string, type: ToastType) {
    if (!text) return;
    const id = ++this.toastSeq;
    this.toasts.set([{id, text, type}, ...this.toasts()]);
    setTimeout(() => this.dismissToast(id), 4000);
  }

  showError(text: string) {
    this.showToast(text, 'error');
  }

  showSuccess(text: string) {
    this.showToast(text, 'success');
  }

  dismissToast(id: number) {
    this.toasts.set(this.toasts().filter(t => t.id !== id));
  }

  trackToast(_i: number, t: { id: number }) {
    return t.id;
  }
}
