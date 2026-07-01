import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router, ParamMap} from '@angular/router';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {forkJoin} from 'rxjs';
import {LessonService} from './lesson.service';
import {
  Achievement,
  Course,
  Exercise,
  ExerciseAttempt,
  GradeResponse,
  HintResponse,
  Lesson,
  Level,
  UiExercise,
  XpSummary
} from '@shared/models';
import {QuizExerciseComponent} from './renderers/quiz-exercise.component';
import {FillInExerciseComponent} from './renderers/fill-in-exercise.component';
import {WritingExerciseComponent} from './renderers/writing-exercise.component';
import {AttemptHistoryComponent} from './renderers/attempt-history.component';
import {TPipe} from '@shared/i18n/t.pipe';
import {XpService} from '@shared/services/xp.service';
import {LevelsService} from '@shared/services/levels.service';
import {AchievementsService} from '@shared/services/achievements.service';
import {SrsService} from '../srs/srs.service';

type ToastType = 'success' | 'error' | 'info';

interface ToastParams extends Record<string, unknown> {
  title?: string | null;
  description?: string | null;
  iconPath?: string | null;
  level?: number | string;
}

interface ToastItem {
  id: number;
  key: string;
  type: ToastType;
  params?: ToastParams;
}

@Component({
  standalone: true,
  selector: 'app-lesson',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    QuizExerciseComponent,
    FillInExerciseComponent,
    WritingExerciseComponent,
    AttemptHistoryComponent,
    TPipe
  ],
  templateUrl: './lesson.component.html',
  styleUrls: ['./lesson.component.scss'],
})
export class LessonComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(LessonService);
  private xp = inject(XpService);
  private levelsApi = inject(LevelsService);
  private achApi = inject(AchievementsService);
  private srs = inject(SrsService);

  private attemptStart = new Map<number, number>();
  private durationSpent = new Map<number, number>();
  private attemptsCount = new Map<number, number>();
  private routeWatchInitialized = false;

  private srsMode = signal(false);

  lessonId = signal<number>(0);
  lessonTitle = signal<string>('');
  course = signal<Course | null>(null);

  loading = signal(true);
  error = signal<string | null>(null);

  exercises = signal<UiExercise[]>([]);
  currentIdx = signal(0);
  progressPercent = signal(0);

  toasts = signal<ToastItem[]>([]);
  private toastSeq = 0;

  private levelsCache: Level[] = [];
  private achievementsMap = new Map<number, Achievement>();
  private myAchievementIds = new Set<number>();
  private prevTotalXp = 0;

  current = computed(() => this.exercises()[this.currentIdx()] ?? null);
  allCompleted = computed(() =>
    this.exercises().length > 0 && this.exercises().every(e => e.result?.correct === true)
  );

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.showErrorKey('errors.badRequest');
      this.loading.set(false);
      return;
    }

    this.srsMode.set(this.route.snapshot.queryParamMap.has('focus'));

    this.lessonId.set(id);
    this.loading.set(true);
    this.error.set(null);

    this.loadGamificationBaseline();

    this.api.getLesson(id).subscribe({
      next: (l: Lesson) => {
        this.lessonTitle.set(l.title);
        if (l.courseId) {
          this.api.getCourse(l.courseId).subscribe({
            next: c => this.course.set(c),
            error: e => this.showErrorKey(this.errKey(e))
          });
        }
        this.api.ensureMyProgress(id).subscribe({
          next: () => {
            this.refreshLessonProgress();
            this.loadExercises(id);
          },
          error: e => {
            const k = this.errKey(e);
            this.showErrorKey(k);
            this.loading.set(false);
          }
        });
      },
      error: e => {
        const k = this.errKey(e);
        this.showErrorKey(k);
        this.loading.set(false);
      }
    });
  }

  private loadGamificationBaseline(): void {
    forkJoin({
      levels: this.levelsApi.list(),
      myAch: this.achApi.listMine(),
      allAch: this.achApi.listAll(),
      summary: this.xp.getSummaryForUser('me')
    }).subscribe({
      next: ({levels, myAch, allAch, summary}) => {
        this.levelsCache = (levels ?? []).slice().sort((a, b) => a.requiredXp - b.requiredXp);
        this.achievementsMap.clear();
        (allAch ?? []).forEach(a => this.achievementsMap.set(a.id as unknown as number, a));
        this.myAchievementIds = new Set((myAch ?? []).map(a => a.achievementId as unknown as number));
        this.prevTotalXp = (summary as XpSummary | undefined)?.totalXp ?? 0;
      },
      error: e => this.showErrorKey(this.errKey(e))
    });
  }

  private loadExercises(id: number) {
    this.api.getExercisesWithOptions(id).subscribe({
      next: (rows) => {
        const base: UiExercise[] = (rows ?? []).map(e => ({
          ...e,
          result: null,
          hintsOnly: null,
          busy: false,
          hintBusy: false,
          validationError: null,
          history: null,
          hintCount: 0
        }));
        this.exercises.set(base);
        this.applyExerciseFromUrlOrDefault();
        this.initQueryParamsWatcherOnce();
        const cur = this.current();
        if (cur) this.markStart(cur.id);
        this.loading.set(false);
      },
      error: (err) => {
        const k = this.errKey(err);
        this.showErrorKey(k);
        this.loading.set(false);
      }
    });
  }

  private applyExerciseFromUrlOrDefault(): void {
    const idFromUrl = this.getExerciseIdFromUrl();
    if (idFromUrl) {
      const idx = this.exercises().findIndex(x => x.id === idFromUrl);
      if (idx >= 0) {
        this.currentIdx.set(idx);
        return;
      }
    }
    this.currentIdx.set(0);
  }

  private initQueryParamsWatcherOnce(): void {
    if (this.routeWatchInitialized) return;
    this.routeWatchInitialized = true;
    this.route.queryParams.subscribe(p => {
      if (p['focus'] != null) {
        this.srsMode.set(true);
      }
      const raw = p['exerciseId'] ?? p['focus'];
      const id = Number(raw);
      if (!Number.isFinite(id) || id <= 0) return;
      const idx = this.exercises().findIndex(x => x.id === id);
      if (idx < 0) return;
      if (idx !== this.currentIdx()) {
        this.currentIdx.set(idx);
        const cur = this.current();
        if (cur) this.markStart(cur.id);
      }
    });
  }

  private getExerciseIdFromUrl(): number | null {
    const qp: ParamMap = this.route.snapshot.queryParamMap;
    const raw = qp.get('exerciseId') ?? qp.get('focus');
    const id = Number(raw);
    return Number.isFinite(id) && id > 0 ? id : null;
  }

  private updateExerciseIdInUrl(id: number): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {exerciseId: id},
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  private refreshLessonProgress() {
    const id = this.lessonId();
    if (!id) return;
    this.api.getMyLessonProgressPercent(id).subscribe({
      next: pct => this.progressPercent.set(this.clampPercent(pct)),
      error: e => {
        this.progressPercent.set(0);
        this.showErrorKey(this.errKey(e));
      }
    });
  }

  private clampPercent(x: number | null | undefined): number {
    const v = Number(x ?? 0);
    return v < 0 ? 0 : v > 100 ? 100 : v;
  }

  private markStart(exerciseId: number) {
    this.attemptStart.set(exerciseId, Date.now());
  }

  private takeDuration(exerciseId: number): number {
    const start = this.attemptStart.get(exerciseId) ?? Date.now();
    const diff = Math.max(1, Math.round((Date.now() - start) / 1000));
    this.attemptStart.set(exerciseId, Date.now());
    return diff;
  }

  private addDuration(exerciseId: number, seconds: number): void {
    const prev = this.durationSpent.get(exerciseId) ?? 0;
    this.durationSpent.set(exerciseId, prev + Math.max(0, seconds));
  }

  private incAttempts(exerciseId: number): void {
    const prev = this.attemptsCount.get(exerciseId) ?? 0;
    this.attemptsCount.set(exerciseId, prev + 1);
  }

  private errKey(e: any, fallback: string = 'errors.default'): string {
    const code = (e?.error?.code || e?.code || '').toString().toUpperCase();
    if (code) return `server.${code}`;
    const s = e?.status;
    if (s === 400) return 'errors.badRequest';
    if (s === 401) return 'errors.unauthorized';
    if (s === 403) return 'errors.forbidden';
    if (s === 404) return 'errors.notFound';
    if (s === 503) return 'errors.serviceUnavailable';
    return fallback;
  }

  private isBackendErrorKey(key: string): boolean {
    return key.startsWith('server.') || key.startsWith('errors.');
  }

  submitAnswer(ex: Exercise, payload: string | number) {
    const arr = this.exercises().slice();
    const idx = arr.findIndex((x) => x.id === ex.id);
    if (idx < 0) return;

    arr[idx].hintBusy = false;
    arr[idx].busy = true;
    arr[idx].validationError = null;
    arr[idx].hintsOnly = null;
    this.exercises.set(arr);

    if (ex.type === 'quiz') {
      if (typeof payload !== 'number') {
        this.clearBusy(idx, 'validation.required');
        return;
      }
      const d = this.takeDuration(ex.id);
      this.addDuration(ex.id, d);
      this.incAttempts(ex.id);
      const body = {
        exerciseId: ex.id,
        chosenOptionId: payload,
        durationSeconds: d
      };
      this.api.createAttempt(body).subscribe({
        next: ({id}) => this.finishEvaluation(idx, id),
        error: (e) => this.clearBusy(idx, this.errKey(e))
      });
    } else {
      if (typeof payload !== 'string' || !payload.trim()) {
        this.clearBusy(idx, 'validation.required');
        return;
      }
      const d = this.takeDuration(ex.id);
      this.addDuration(ex.id, d);
      this.incAttempts(ex.id);
      const body = {
        exerciseId: ex.id,
        submittedAnswer: payload.trim(),
        durationSeconds: d
      };
      this.api.createAttempt(body).subscribe({
        next: ({id}) => this.finishEvaluation(idx, id),
        error: (e) => this.clearBusy(idx, this.errKey(e))
      });
    }
  }

  requestHint(ex: Exercise, userAnswer?: string | null) {
    const arr = this.exercises().slice();
    const idx = arr.findIndex(x => x.id === ex.id);
    if (idx < 0) return;

    const prev = arr[idx];
    const prevCount = prev.hintCount ?? 0;

    let answerToSend: string;

    if (ex.type === 'quiz') {
      answerToSend = 'HINT_ONLY';
    } else {
      const trimmed = (userAnswer ?? '').trim();
      answerToSend = trimmed || 'HINT_ONLY';
    }

    arr[idx] = {...prev, hintBusy: true, validationError: null, result: null};
    this.exercises.set(arr);

    this.api.hint({
      exerciseId: ex.id,
      userAnswer: answerToSend,
      maxHints: 1
    }).subscribe({
      next: (h: HintResponse) => {
        const a = this.exercises().slice();
        const current = a[idx];
        const latest: HintResponse = {
          feedback: h.feedback,
          hints: (h.hints ?? []).slice(),
          correct: h.correct
        };
        a[idx] = {...current, hintsOnly: latest, hintBusy: false, hintCount: prevCount + 1};
        this.exercises.set(a);
      },
      error: (e) => this.clearHintBusy(idx, this.errKey(e))
    });
  }

  private finishEvaluation(idx: number, attemptId: number) {
    this.api.evaluateAttempt(attemptId).subscribe({
      next: (res: GradeResponse) => {
        const a = this.exercises().slice();
        const exerciseId = a[idx]?.id;

        a[idx] = {...a[idx], result: res, busy: false, validationError: null, hintsOnly: null};
        this.exercises.set(a);

        if (this.srsMode() && Number.isFinite(exerciseId) && (exerciseId as number) > 0) {
          const quality = res.correct ? 5 : 2;
          this.srs.review(exerciseId as number, quality).subscribe({
            error: e => this.showErrorKey(this.errKey(e))
          });
        }

        this.checkAllCompleted();
        if (res.awardedXp != null && res.awardedXp > 0) {
          this.checkProgressToasts();
        }
        this.refreshLessonProgress();
      },
      error: (e) => this.clearBusy(idx, this.errKey(e))
    });
  }

  private checkAllCompleted(): void {
    const allDone = this.exercises().every(e => e.result?.correct === true);
    if (allDone) {
      this.api.markLessonCompleted(this.lessonId()).subscribe({
        next: () => this.refreshLessonProgress(),
        error: e => this.showErrorKey(this.errKey(e))
      });
    }
  }

  private resolveLevelForXp(totalXp: number): number {
    const lvls = this.levelsCache.slice().sort((a, b) => a.requiredXp - b.requiredXp);
    if (!lvls.length) return 1;
    let current = lvls[0].level ?? 1;
    for (const l of lvls) {
      if (totalXp >= l.requiredXp) current = l.level; else break;
    }
    return current;
  }

  private checkProgressToasts(): void {
    forkJoin({
      summary: this.xp.getSummaryForUser('me'),
      myAch: this.achApi.listMine()
    }).subscribe({
      next: ({summary, myAch}) => {
        const newTotal = (summary as XpSummary | undefined)?.totalXp ?? this.prevTotalXp;
        const prevLevel = this.resolveLevelForXp(this.prevTotalXp);
        const newLevel = this.resolveLevelForXp(newTotal);
        if (newLevel > prevLevel) {
          this.showToast('toast.levelUp', 'info', {level: newLevel});
        }
        const newIds = new Set((myAch ?? []).map(a => a.achievementId as unknown as number));
        const unlocked: number[] = [];
        newIds.forEach(id => {
          if (!this.myAchievementIds.has(id)) unlocked.push(id);
        });
        unlocked.forEach(id => {
          const a = this.achievementsMap.get(id);
          if (a) this.showToast('toast.achievementUnlocked', 'info', {
            title: a.title,
            description: a.description,
            iconPath: a.iconPath
          });
        });
        this.prevTotalXp = newTotal;
        this.myAchievementIds = newIds;
      },
      error: e => this.showErrorKey(this.errKey(e))
    });
  }

  goto(i: number) {
    if (i < 0 || i >= this.exercises().length) return;
    this.currentIdx.set(i);
    const cur = this.current();
    if (cur) {
      this.markStart(cur.id);
      this.updateExerciseIdInUrl(cur.id);
    }
  }

  loadHistory(ex: Exercise) {
    const arr = this.exercises().slice();
    const idx = arr.findIndex((x) => x.id === ex.id);
    if (idx < 0) return;

    if (arr[idx].history !== null) {
      arr[idx] = {...arr[idx], history: null} as UiExercise;
      this.exercises.set(arr);
      return;
    }

    this.api.listMyAttempts(ex.id).subscribe({
      next: (rows: ExerciseAttempt[]) => {
        arr[idx] = {...arr[idx], history: rows} as UiExercise;
        this.exercises.set(arr);
      },
      error: (e) => {
        arr[idx] = {...arr[idx], history: []} as UiExercise;
        this.exercises.set(arr);
        this.showErrorKey(this.errKey(e));
      }
    });
  }

  clearValidationError(ex: Exercise) {
    const a = this.exercises().slice();
    const idx = a.findIndex(x => x.id === ex.id);
    if (idx < 0) return;
    if (a[idx].validationError) {
      a[idx] = {...a[idx], validationError: null};
      this.exercises.set(a);
    }
  }

  private clearBusy(idx: number, key: string) {
    const a = this.exercises().slice();
    if (this.isBackendErrorKey(key)) {
      a[idx] = {...a[idx], busy: false, validationError: null};
      this.exercises.set(a);
      this.showErrorKey(key);
    } else {
      a[idx] = {...a[idx], busy: false, validationError: key};
      this.exercises.set(a);
    }
  }

  private clearHintBusy(idx: number, key: string) {
    const a = this.exercises().slice();
    if (this.isBackendErrorKey(key)) {
      a[idx] = {...a[idx], hintBusy: false, validationError: null};
      this.exercises.set(a);
      this.showErrorKey(key);
    } else {
      a[idx] = {...a[idx], hintBusy: false, validationError: key};
      this.exercises.set(a);
    }
  }

  private showToast(key: string, type: ToastType, params?: ToastParams) {
    const id = ++this.toastSeq;
    this.toasts.set([{id, key, type, params}, ...this.toasts()]);
    setTimeout(() => this.dismissToast(id), 4000);
  }

  private showErrorKey(key: string) {
    this.showToast(key, 'error');
  }

  dismissToast(id: number) {
    this.toasts.set(this.toasts().filter(t => t.id !== id));
  }

  trackToast(_i: number, t: { id: number }) {
    return t.id;
  }

  totalAttempts(): number {
    let sum = 0;
    this.attemptsCount.forEach(v => sum += v);
    return sum;
  }

  totalHints(): number {
    return this.exercises().reduce((acc, e) => acc + (e.hintCount ?? 0), 0);
  }

  totalDurationSeconds(): number {
    let sum = 0;
    this.durationSpent.forEach(v => sum += v);
    return sum;
  }

  totalAwardedXp(): number {
    return this.exercises().reduce((acc, e) => acc + (e.result?.awardedXp ?? 0), 0);
  }

  avgAttemptsPerExercise(): number {
    const n = this.exercises().length || 1;
    return this.totalAttempts() / n;
  }

  formatDuration(totalSec: number): string {
    const s = Math.max(0, Math.floor(totalSec));
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    if (h > 0) return `${h}h ${m}m ${sec}s`;
    if (m > 0) return `${m}m ${sec}s`;
    return `${sec}s`;
  }
}
