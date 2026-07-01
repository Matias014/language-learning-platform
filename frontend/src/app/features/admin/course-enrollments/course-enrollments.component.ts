import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ReactiveFormsModule, FormBuilder, Validators} from '@angular/forms';
import {FormsModule} from '@angular/forms';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {AdminCourseEnrollmentsService} from './admin-course-enrollments.service';
import {AdminUsersService} from '../users/admin-users.service';
import {AdminCoursesService} from '../courses/admin-courses.service';
import {Course} from '@app/shared/models/course.model';
import {User} from '@app/shared/models/user.model';
import type {CourseEnrollment} from '@app/shared/models/course-enrollment.model';
import {I18nService} from '@app/shared/i18n/i18n.service';
import {finalize} from 'rxjs/operators';
import {CourseStatus} from '@shared/models';

type SortDir = 'asc' | 'desc';
type SortProp =
  | 'id'
  | 'userId'
  | 'courseId'
  | 'status'
  | 'currentLessonId'
  | 'startedAt'
  | 'completedAt'
  | 'lastActivityAt';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-course-enrollments',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TPipe],
  templateUrl: './course-enrollments.component.html',
  styleUrls: ['./course-enrollments.component.scss'],
})
export class AdminCourseEnrollmentsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(AdminCourseEnrollmentsService);
  private apiUsers = inject(AdminUsersService);
  private apiCourses = inject(AdminCoursesService);
  private i18n = inject(I18nService);

  loading = signal(false);

  baseRows = signal<CourseEnrollment[]>([]);
  filterStatus = signal<string>('');

  users = signal<User[]>([]);
  courses = signal<Course[]>([]);

  filterUserId = signal<number | null>(null);
  filterCourseId = signal<number | null>(null);

  rows = computed(() => {
    const s = this.filterStatus();
    const list = this.baseRows();
    if (!s) return list;
    return list.filter(e => e.status === s);
  });

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

  statusOptions: CourseStatus[] = ['in_progress', 'completed'];

  editingId = signal<number | null>(null);

  form = this.fb.nonNullable.group({
    currentLessonId: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  ngOnInit() {
    this.apiUsers.list().subscribe({
      next: u => this.users.set(u ?? []),
      error: () => this.users.set([]),
    });
    this.apiCourses.list().subscribe({
      next: c => this.courses.set(c ?? []),
      error: () => this.courses.set([]),
    });
    this.load();
  }

  private compareValues(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'startedAt' || prop === 'completedAt' || prop === 'lastActivityAt') {
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

  onUserIdChange(v: number | null) {
    this.filterUserId.set(v ?? null);
    this.page.set(0);
    this.load();
  }

  onCourseIdChange(v: number | null) {
    this.filterCourseId.set(v ?? null);
    this.page.set(0);
    this.load();
  }

  onStatusChange(v: string) {
    this.filterStatus.set(v || '');
    this.page.set(0);
  }

  clearFilters() {
    this.filterUserId.set(null);
    this.filterCourseId.set(null);
    this.filterStatus.set('');
    this.page.set(0);
    this.size.set(10);
    this.sortProp.set('id');
    this.sortDir.set('desc');
    this.load();
  }

  private load() {
    this.loading.set(true);
    this.api
      .list({
        userId: this.filterUserId() ?? undefined,
        courseId: this.filterCourseId() ?? undefined,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: list => {
          this.baseRows.set(list ?? []);
          this.page.set(0);
        },
        error: err => {
          if (err?.status === 404) {
            this.baseRows.set([]);
            this.page.set(0);
            return;
          }
          const msg = this.i18n.getServerErrorMessage(err, 'admin.enrollments.errors.load') || '';
          if (msg) this.showError(msg);
          this.baseRows.set([]);
          this.page.set(0);
        },
      });
  }

  edit(e: CourseEnrollment) {
    this.editingId.set(e.id);
    this.form.reset({currentLessonId: e.currentLessonId ?? null});
  }

  cancel() {
    this.editingId.set(null);
    this.form.enable();
    this.form.reset();
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.editingId();
    if (!id) return;
    const v = this.form.getRawValue();
    const patch = {currentLessonId: Number(v.currentLessonId)};
    this.api.update(id, patch).subscribe({
      next: () => {
        this.cancel();
        this.load();
      },
      error: err => {
        if (err?.status === 404) return;
        const msg = this.i18n.getServerErrorMessage(err, 'admin.enrollments.errors.update') || '';
        if (msg) this.showError(msg);
      },
    });
  }

  remove(id: number) {
    const ok = confirm(this.i18n.t('admin.enrollments.confirmDelete'));
    if (!ok) return;
    this.api.delete(id).subscribe({
      next: () => this.load(),
      error: err => {
        if (err?.status === 404) {
          this.load();
          return;
        }
        const msg = this.i18n.getServerErrorMessage(err, 'admin.enrollments.errors.delete') || '';
        if (msg) this.showError(msg);
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

  trackById = (_: number, e: CourseEnrollment) => e.id;

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
