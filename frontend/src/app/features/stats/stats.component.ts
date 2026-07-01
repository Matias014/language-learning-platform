import {Component, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {NgChartsModule} from 'ng2-charts';
import {ChartConfiguration, ChartType} from 'chart.js';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {StatsService} from './stats.service';
import {I18nService} from '@shared/i18n/i18n.service';
import {User, XpPoint, XpBreakdownItem, SrsThisWeek, SrsSummary} from '@shared/models';
import {EffectivenessStats} from '@shared/models/effectiveness-stats.model';
import {ActivityStats} from '@shared/models/activity-stats.model';

type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-stats',
  imports: [CommonModule, RouterModule, NgChartsModule, TPipe],
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss'],
})
export class StatsComponent {
  private svc = inject(StatsService);
  private i18n = inject(I18nService);

  private readonly TZ = 'Europe/Warsaw';

  loading = signal(true);
  error = signal<string | null>(null);
  me = signal<User | null>(null);

  rangeDays = signal<7 | 30 | 90>(7);
  data = signal<XpPoint[]>([]);
  totals = signal({lessons: 0, awards: 0, xp: 0});

  breakType = signal<XpBreakdownItem[]>([]);
  breakDiff = signal<XpBreakdownItem[]>([]);
  srsWeek = signal<SrsThisWeek | null>(null);
  srsSummary = signal<SrsSummary | null>(null);

  eff = signal<EffectivenessStats | null>(null);
  activity = signal<ActivityStats | null>(null);

  math = Math;

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  lineData = signal<ChartConfiguration['data']>({labels: [], datasets: [{data: [], label: 'XP'}]});
  lineOpts: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {legend: {display: false}},
    scales: {x: {ticks: {maxRotation: 0, autoSkip: true}}, y: {beginAtZero: true}}
  };
  lineType: ChartType = 'line';

  doughTypeData = signal<ChartConfiguration['data']>({labels: [], datasets: [{data: []}]});
  doughDiffData = signal<ChartConfiguration['data']>({labels: [], datasets: [{data: []}]});
  doughOpts: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {legend: {position: 'bottom'}}
  };
  doughType: ChartType = 'doughnut';
  doughDiff: ChartType = 'doughnut';

  accTypeData = signal<ChartConfiguration['data']>({labels: [], datasets: [{data: []}]});
  accDiffData = signal<ChartConfiguration['data']>({labels: [], datasets: [{data: []}]});
  accType: ChartType = 'doughnut';
  accDiff: ChartType = 'doughnut';

  ngOnInit() {
    this.loadAll();
  }

  chartHeight(): number {
    const d = this.rangeDays();
    if (d === 7) return 260;
    if (d === 30) return 340;
    return 420;
  }

  private loadAll() {
    this.loading.set(true);
    this.error.set(null);

    this.svc.getMe().subscribe({
      next: u => this.me.set(u),
      error: err => {
        const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || this.i18n.t('errors.default');
        this.error.set(msg);
        this.showError(msg);
      }
    });

    this.svc.getXpSummary().subscribe({
      next: s => this.totals.set({...this.totals(), xp: (s as any).totalXp, awards: (s as any).awardsCount}),
      error: err => {
        const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || this.i18n.t('errors.default');
        this.error.set(msg);
        this.showError(msg);
      }
    });

    this.svc.countCompletedLessons().subscribe({
      next: cnt => this.totals.set({...this.totals(), lessons: cnt}),
      error: err => {
        const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || this.i18n.t('errors.default');
        this.error.set(msg);
        this.showError(msg);
      }
    });

    this.loadRange(this.rangeDays());
    this.loadSrsWeek();
    this.loadSrsSummary();
    this.loadEffectiveness();
    this.loadActivity();
  }

  changeRange(d: 7 | 30 | 90) {
    this.rangeDays.set(d);
    this.loadRange(d);
  }

  private loadRange(days: 7 | 30 | 90) {
    this.loading.set(true);

    this.svc.getTimeseries(days).subscribe({
      next: arr => {
        this.data.set(arr);
        this.lineData.set({
          labels: arr.map(x => (x as any).date),
          datasets: [{data: arr.map(x => (x as any).xp), label: 'XP'}]
        });
        this.loading.set(false);
      },
      error: err => {
        this.data.set([]);
        const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || this.i18n.t('errors.default');
        this.error.set(msg);
        this.showError(msg);
        this.loading.set(false);
      }
    });

    this.svc.getXpBreakdowns(days).subscribe({
      next: b => {
        this.breakType.set((b as any).type);
        this.breakDiff.set((b as any).difficulty);
        const typeLabels = (b as any).type.map((i: any) => this.i18n.t(`exercise.type.${i.key}`));
        const typeValues = (b as any).type.map((i: any) => i.xp);
        const diffLabels = (b as any).difficulty.map((i: any) => this.i18n.t(`difficulty.${i.key}`));
        const diffValues = (b as any).difficulty.map((i: any) => i.xp);
        this.doughTypeData.set({labels: typeLabels, datasets: [{data: typeValues}]});
        this.doughDiffData.set({labels: diffLabels, datasets: [{data: diffValues}]});
      },
      error: err => {
        this.breakType.set([]);
        this.breakDiff.set([]);
        const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || this.i18n.t('errors.default');
        this.showError(msg);
      }
    });
  }

  private loadSrsWeek() {
    this.svc.getSrsThisWeek().subscribe({
      next: s => this.srsWeek.set(s),
      error: err => {
        this.srsWeek.set({dueTotal: 0, byDay: []} as any);
        const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || this.i18n.t('errors.default');
        this.showError(msg);
      }
    });
  }

  private loadSrsSummary() {
    this.svc.getSrsSummary().subscribe({
      next: s => this.srsSummary.set(s),
      error: err => {
        this.srsSummary.set({dueTodayCount: 0, dueNext7DaysCount: 0} as any);
        const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || this.i18n.t('errors.default');
        this.showError(msg);
      }
    });
  }

  private loadEffectiveness() {
    this.svc.getEffectivenessStats().subscribe({
      next: e => {
        this.eff.set(e);
        const byType = (e as any).accuracyByType || {};
        const byDiff = (e as any).accuracyByDifficulty || {};
        const typeEntries = Object.entries(byType);
        const diffEntries = Object.entries(byDiff);
        const typeLabels = typeEntries.map(([k]) => this.i18n.t(`exercise.type.${k}`));
        const typeValues = typeEntries.map(([, v]) => Math.round(((v as any) || 0) * 100));
        const diffLabels = diffEntries.map(([k]) => this.i18n.t(`difficulty.${k}`));
        const diffValues = diffEntries.map(([, v]) => Math.round(((v as any) || 0) * 100));
        this.accTypeData.set({labels: typeLabels, datasets: [{data: typeValues}]});
        this.accDiffData.set({labels: diffLabels, datasets: [{data: diffValues}]});
      },
      error: err => {
        this.eff.set(null);
        this.accTypeData.set({labels: [], datasets: [{data: []}]});
        this.accDiffData.set({labels: [], datasets: [{data: []}]});
        const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || this.i18n.t('errors.default');
        this.showError(msg);
      }
    });
  }

  private loadActivity() {
    this.svc.getActivityStats().subscribe({
      next: a => this.activity.set(a),
      error: err => {
        this.activity.set({streakDays: 0, activeDaysCount: 0, lastActivityAt: null} as any);
        const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || this.i18n.t('errors.default');
        this.showError(msg);
      }
    });
  }

  srsPercent(count: number): number {
    const max = (this.srsWeek() as any)?.byDay?.reduce((m: number, d: any) => Math.max(m, d.count), 0) || 0;
    if (max <= 0) return 0;
    return Math.round((count / max) * 100);
  }

  private todayIso(): string {
    return new Intl.DateTimeFormat('en-CA', {
      timeZone: this.TZ,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(new Date());
  }

  todayCount(): number {
    const byDay = (this.srsWeek() as any)?.byDay || [];
    return byDay.find((d: any) => d.date === this.todayIso())?.count ?? 0;
  }

  avgPerDay(): number {
    const arr = this.data();
    if (!arr.length) return 0;
    const sum = arr.reduce((s, x: any) => s + (x.xp || 0), 0);
    return Math.round(sum / arr.length);
  }

  exportPdf() {
    this.svc.exportUserPdf().subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'langschool-stats.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: err => {
        const msg = this.i18n.getServerErrorMessage(err, 'stats.errors.export') || this.i18n.t('stats.errors.export');
        this.showError(msg);
      }
    });
  }

  globalAccuracyPercent(): number {
    const g = (this.eff() as any)?.globalAccuracy || 0;
    return Math.round(g * 100);
  }

  avgOpenScore(): number | null {
    const v = (this.eff() as any)?.averageOpenScore;
    if (v == null) return null;
    return Math.round(v * 100) / 100;
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
