import {Component, inject, signal, computed} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgChartsModule} from 'ng2-charts';
import {ChartConfiguration, ChartType} from 'chart.js';
import {TPipe} from '@shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';
import {AdminReportsService} from './admin-reports.service';
import {AdminHardestExercise, AdminLlmStats, InteractionType, LlmStatus} from '@shared/models';

@Component({
  standalone: true,
  selector: 'app-admin-reports',
  imports: [CommonModule, FormsModule, NgChartsModule, TPipe],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class AdminReportsComponent {
  private svc = inject(AdminReportsService);
  private i18n = inject(I18nService);

  loading = signal(false);
  error = signal<string | null>(null);

  llm = signal<AdminLlmStats | null>(null);
  hardest = signal<AdminHardestExercise[]>([]);
  limit = signal(10);

  doughData = signal<ChartConfiguration['data']>({labels: [], datasets: [{data: []}]});
  doughOpts: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {legend: {position: 'bottom'}}
  };
  doughType: ChartType = 'doughnut';

  statusData = signal<ChartConfiguration['data']>({labels: [], datasets: [{data: []}]});
  statusOpts: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {legend: {position: 'bottom'}}
  };
  statusType: ChartType = 'doughnut';

  totalCalls = computed(() => this.llm()?.calls ?? 0);
  tokensIn = computed(() => this.llm()?.tokensIn ?? 0);
  tokensOut = computed(() => this.llm()?.tokensOut ?? 0);
  avgLatency = computed(() => {
    const v = this.llm()?.averageLatencyMs;
    return v == null ? 0 : Math.round(v);
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);

    this.svc.getLlmStats().subscribe({
      next: s => {
        this.llm.set(s);

        const typeMap = s.callsByInteractionType || {};
        const typeEntries: [InteractionType, number][] = Object.entries(typeMap) as any;
        const typeLabels = typeEntries.map(([k]) => this.i18n.t(`admin.llmLogs.type.${k}`));
        const typeValues = typeEntries.map(([, v]) => v || 0);
        this.doughData.set({labels: typeLabels, datasets: [{data: typeValues}]});

        const statusMap = s.callsByStatus || {};
        const statusEntries: [LlmStatus, number][] = Object.entries(statusMap) as any;
        const statusLabels = statusEntries.map(([k]) => this.i18n.t(`admin.llmLogs.status.${k}`));
        const statusValues = statusEntries.map(([, v]) => v || 0);
        this.statusData.set({labels: statusLabels, datasets: [{data: statusValues}]});

        this.loading.set(false);
      },
      error: err => {
        this.llm.set(null);
        this.doughData.set({labels: [], datasets: [{data: []}]});
        this.statusData.set({labels: [], datasets: [{data: []}]});
        this.error.set(this.i18n.getServerErrorMessage(err, 'errors.default'));
        this.loading.set(false);
      }
    });

    this.reloadHardest();
  }

  reloadHardest() {
    this.svc.getHardestExercises(this.limit()).subscribe({
      next: rows => this.hardest.set(rows || []),
      error: err => {
        this.hardest.set([]);
        this.error.set(this.i18n.getServerErrorMessage(err, 'errors.default'));
      }
    });
  }

  onLimitChange(v: string | number | null) {
    const n = typeof v === 'string' ? parseInt(v || '10', 10) : (v ?? 10);
    this.limit.set(Number.isFinite(n) ? Math.min(Math.max(n, 1), 100) : 10);
  }

  trackByExerciseId(i: number, r: AdminHardestExercise): number {
    return r.exerciseId;
  }

  formatAccuracy(a: number | null): string {
    if (a == null) return '—';
    return `${Math.round(a * 100)}%`;
  }

  formatDuration(sec: number | null): string {
    if (sec == null) return '—';
    const s = Math.max(0, Math.floor(sec));
    const m = Math.floor(s / 60);
    const r = s % 60;
    return `${m}:${r.toString().padStart(2, '0')}`;
  }

  exportLlmCsv() {
    this.svc.downloadLlmCsv().subscribe(resp => this.saveBlob(resp, 'llm-stats.csv'));
  }

  exportLlmPdf() {
    this.svc.downloadLlmPdf().subscribe(resp => this.saveBlob(resp, 'llm-stats.pdf'));
  }

  exportHardestCsv() {
    this.svc.downloadHardestCsv(this.limit()).subscribe(resp => this.saveBlob(resp, 'hardest-exercises.csv'));
  }

  exportHardestPdf() {
    this.svc.downloadHardestPdf(this.limit()).subscribe(resp => this.saveBlob(resp, 'hardest-exercises.pdf'));
  }

  private saveBlob(resp: any, fallbackName: string) {
    const blob: Blob | null = resp?.body instanceof Blob ? resp.body : null;
    if (!blob) return;
    const cd = resp.headers?.get?.('content-disposition') ?? null;
    const name = this.extractFilename(cd) || fallbackName;
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  }

  private extractFilename(cd: string | null): string | null {
    if (!cd) return null;
    const m1 = /filename\*\s*=\s*(?:UTF-8''|utf-8'')?([^;]+)/i.exec(cd);
    if (m1 && m1[1]) {
      const raw = m1[1].trim().replace(/^"(.*)"$/, '$1');
      try {
        return decodeURIComponent(raw);
      } catch {
        return raw;
      }
    }
    const m2 = /filename\s*=\s*"?([^";]+)"?/i.exec(cd);
    if (m2 && m2[1]) return m2[1].trim();
    return null;
  }
}
