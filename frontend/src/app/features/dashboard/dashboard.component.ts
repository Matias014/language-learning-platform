import {Component, inject, signal, computed} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterLink} from '@angular/router';
import {DashboardService} from './dashboard.service';
import {DashboardVm, XpPoint} from '@app/shared/models';
import {TPipe} from '@shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';

type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink, TPipe],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  private svc = inject(DashboardService);
  private router = inject(Router);
  private i18n = inject(I18nService);

  loading = signal(true);
  error = signal<string | null>(null);
  data = signal<DashboardVm | null>(null);

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  username = computed(() => this.data()?.me?.name || this.data()?.me?.login || '');
  totalXpDisplay = computed(() => this.data()?.totalXp ?? this.data()?.me?.totalXp ?? 0);
  srsDueToday = computed<number>(() => this.data()?.srsSummary?.dueTodayCount ?? 0);
  recommendation = computed(() => this.data()?.recommendation ?? null);

  levelDisplay = computed(() => this.data()?.currentLevel ?? 1);
  barPercent = computed(() => this.clampPercent(this.data()?.percentToNext ?? 0));

  ngOnInit() {
    this.loading.set(true);
    this.error.set(null);
    this.svc.load().subscribe({
      next: vm => {
        this.data.set(vm);
        this.loading.set(false);
      },
      error: err => {
        const toastText =
          this.i18n.getServerErrorMessage(err, 'dashboard.errors.fetch') ||
          this.i18n.t('dashboard.errors.fetch');
        this.error.set('dashboard.errors.fetch');
        this.showError(toastText);
        this.loading.set(false);
      }
    });
  }

  continue() {
    const nextId = this.data()?.nextLessonId;
    this.router.navigateByUrl(nextId ? `/lessons/${nextId}` : '/courses');
  }

  clampPercent(x: number | null | undefined): number {
    const v = Number(x ?? 0);
    return v < 0 ? 0 : v > 100 ? 100 : v;
  }

  trackByDate(_i: number, p: XpPoint) {
    return p.date;
  }

  trackByEnrollment(_i: number, e: DashboardVm['enrollments'][number]) {
    return e.id;
  }

  avatarSrc(): string {
    const path = this.data()?.me?.avatarPath || '';
    return path || 'assets/avatar.svg';
  }

  onAvatarError(e: Event) {
    (e.target as HTMLImageElement).src = 'assets/avatar.svg';
  }

  xpPercent(p: XpPoint): number {
    const arr = this.data()?.xp7d ?? [];
    const max = arr.reduce((m, it) => Math.max(m, it.xp), 0);
    if (!max) {
      return 0;
    }
    return this.clampPercent((p.xp / max) * 100);
  }

  private showToast(text: string, type: ToastType) {
    const id = ++this.toastSeq;
    this.toasts.set([{id, text, type}, ...this.toasts()]);
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
}
