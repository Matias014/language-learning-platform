import {Component, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterLink} from '@angular/router';
import {CoursesService} from './courses.service';
import {AuthService} from '@core/auth/auth.service';
import {Course, CourseEnrollment} from '@shared/models';
import {TPipe} from '@shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';
import {forkJoin} from 'rxjs';

type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-courses',
  imports: [CommonModule, RouterLink, TPipe],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss']
})
export class CoursesComponent {
  private api = inject(CoursesService);
  private auth = inject(AuthService);
  private i18n = inject(I18nService);

  rows = signal<Course[]>([]);
  loading = signal(true);
  private enrollmentsMap = signal<Map<number, CourseEnrollment>>(new Map());
  private progressMap = signal<Map<number, number>>(new Map());

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  constructor() {
    this.fetch();
  }

  private fetch() {
    this.loading.set(true);
    this.api.listCourses().subscribe({
      next: courses => {
        const list = courses || [];
        this.rows.set(list);

        if (this.auth.isAuthenticated() && list.length > 0) {
          this.loadMyData();
        } else {
          this.loading.set(false);
        }
      },
      error: e => {
        this.rows.set([]);
        const msg =
          this.i18n.getServerErrorMessage(e, 'coursesList.errorFetch') ||
          this.i18n.t('coursesList.errorFetch');
        this.showError(msg);
        this.loading.set(false);
      }
    });
  }

  private loadMyData() {
    forkJoin({
      enrollments: this.api.myEnrollments(),
      progress: this.api.listMyCourseProgress()
    }).subscribe({
      next: ({enrollments, progress}) => {
        const em = new Map<number, CourseEnrollment>();
        (enrollments || []).forEach(e => em.set(e.courseId, e));
        this.enrollmentsMap.set(em);

        const pm = new Map<number, number>();
        (progress || []).forEach(p =>
          pm.set(p.courseId, Math.max(0, Math.min(100, p.progressPercent ?? 0)))
        );
        this.progressMap.set(pm);

        this.loading.set(false);
      },
      error: e => {
        this.enrollmentsMap.set(new Map());
        this.progressMap.set(new Map());
        if (this.rows().length > 0) {
          const msg =
            this.i18n.getServerErrorMessage(e, 'coursesList.errorFetchMy') ||
            this.i18n.t('coursesList.errorFetchMy');
          this.showError(msg);
        }
        this.loading.set(false);
      }
    });
  }

  enrollmentFor(courseId: number): CourseEnrollment | null {
    return this.enrollmentsMap().get(courseId) ?? null;
  }

  progressFor(courseId: number): number | null {
    const v = this.progressMap().get(courseId);
    return typeof v === 'number' ? v : null;
  }

  clampPercent(x: number | null | undefined): number {
    const v = Number(x ?? 0);
    return v < 0 ? 0 : v > 100 ? 100 : v;
  }

  private showToast(text: string, type: ToastType) {
    const id = ++this.toastSeq;
    this.toasts.set(this.toasts().concat([{id, text, type}]));
    setTimeout(() => this.dismissToast(id), 4000);
  }

  showError(text: string) {
    if (!text) {
      return;
    }
    this.showToast(text, 'error');
  }

  dismissToast(id: number) {
    this.toasts.set(this.toasts().filter(t => t.id !== id));
  }

  trackToast(_i: number, t: { id: number }) {
    return t.id;
  }

  trackByCourseId = (_: number, c: Course) => c.id;
}
