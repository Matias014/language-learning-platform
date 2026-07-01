import {Component, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {CoursesService} from '../courses.service';
import {Course, Lesson, CourseEnrollment} from '@shared/models';
import {AuthService} from '@core/auth/auth.service';
import {TPipe} from '@shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';

type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-course-detail',
  imports: [CommonModule, RouterLink, TPipe],
  templateUrl: './course-detail.component.html',
  styleUrls: ['./course-detail.component.scss']
})
export class CourseDetailComponent {
  private api = inject(CoursesService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  private i18n = inject(I18nService);

  course = signal<Course | null>(null);
  lessons = signal<Lesson[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  myEnroll: CourseEnrollment | null = null;
  progressPercent = signal<number>(0);

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  constructor() {
    this.load();
  }

  isAuth() {
    return this.auth.isAuthenticated();
  }

  private load() {
    this.loading.set(true);
    this.error.set(null);
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);
    if (!Number.isFinite(id) || id <= 0) {
      this.router.navigateByUrl('/404');
      return;
    }
    this.api.getCourse(id).subscribe({
      next: c => {
        this.course.set(c);
        this.loadLessons(id);
        if (this.auth.isAuthenticated()) {
          this.api.getMyEnrollmentForCourse(id).subscribe({
            next: en => {
              this.myEnroll = en;
              this.fetchProgress(id);
            },
            error: () => {
              this.myEnroll = null;
            }
          });
        }
      },
      error: (e: HttpErrorResponse) => {
        if (e.status === 404) {
          this.router.navigateByUrl('/404');
        } else {
          const toastText =
            this.i18n.getServerErrorMessage(e, 'courseDetail.notFound') ||
            this.i18n.t('courseDetail.notFound');
          this.loading.set(false);
          this.error.set('courseDetail.notFound');
          this.showError(toastText);
        }
      }
    });
  }

  private fetchProgress(courseId: number) {
    this.api.getMyCourseProgress(courseId).subscribe({
      next: p => this.progressPercent.set(this.clampPercent(p?.progressPercent ?? 0)),
      error: () => this.progressPercent.set(0)
    });
  }

  private loadLessons(id: number) {
    this.api.listLessonsForCourse(id).subscribe({
      next: ls => {
        this.lessons.set(ls || []);
        this.loading.set(false);
      },
      error: e => {
        this.lessons.set([]);
        this.loading.set(false);
        const toastText =
          this.i18n.getServerErrorMessage(e, 'courseDetail.lessonsError') ||
          this.i18n.t('courseDetail.lessonsError');
        this.error.set('courseDetail.lessonsError');
        this.showError(toastText);
      }
    });
  }

  enroll() {
    if (!this.course()) {
      return;
    }
    this.error.set(null);
    this.api.enroll(this.course()!.id).subscribe({
      next: en => {
        this.myEnroll = en;
        this.showSuccess(this.i18n.t('courseDetail.enrollSuccess'));
        this.fetchProgress(this.course()!.id);
      },
      error: (e: HttpErrorResponse) => {
        if (e.status === 409 || e.status === 400) {
          this.api.getMyEnrollmentForCourse(this.course()!.id).subscribe({
            next: en => {
              this.myEnroll = en;
              this.showSuccess(this.i18n.t('courseDetail.enrollSuccess'));
              this.fetchProgress(this.course()!.id);
            },
            error: err => {
              const toastText =
                this.i18n.getServerErrorMessage(err, 'courseDetail.enrollError') ||
                this.i18n.t('courseDetail.enrollError');
              this.error.set('courseDetail.enrollError');
              this.showError(toastText);
            }
          });
        } else {
          const toastText =
            this.i18n.getServerErrorMessage(e, 'courseDetail.enrollError') ||
            this.i18n.t('courseDetail.enrollError');
          this.error.set('courseDetail.enrollError');
          this.showError(toastText);
        }
      }
    });
  }

  unenroll() {
    if (!this.myEnroll) {
      return;
    }
    this.error.set(null);
    this.api.unenroll(this.myEnroll.id).subscribe({
      next: () => {
        this.myEnroll = null;
        this.progressPercent.set(0);
        this.showSuccess(this.i18n.t('courseDetail.unenrollSuccess'));
      },
      error: (e: HttpErrorResponse) => {
        const toastText =
          this.i18n.getServerErrorMessage(e, 'courseDetail.unenrollError') ||
          this.i18n.t('courseDetail.unenrollError');
        this.error.set('courseDetail.unenrollError');
        this.showError(toastText);
      }
    });
  }

  formatOrder(l: Lesson): string {
    return l.orderNumber != null ? `${l.orderNumber}. ` : '';
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

  showSuccess(text: string) {
    if (!text) {
      return;
    }
    this.showToast(text, 'success');
  }

  dismissToast(id: number) {
    this.toasts.set(this.toasts().filter(t => t.id !== id));
  }

  trackToast(_i: number, t: { id: number }) {
    return t.id;
  }

  trackByLessonId = (_: number, l: Lesson) => l.id;
}
