import {Component, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {AdminCoursesService} from '../courses/admin-courses.service';
import {AdminLessonsService} from '../lessons/admin-lessons.service';
import {AdminExercisesService} from './admin-exercises.service';
import {AdminAiExerciseService} from './admin-ai-exercise.service';
import {Course, Exercise, DifficultyLevel, Lesson, ExerciseType} from '@shared/models';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {I18nService} from '@app/shared/i18n/i18n.service';

type SortDir = 'asc' | 'desc';
type SortProp = 'id' | 'type' | 'xp' | 'difficulty' | 'orderNumber';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-exercises',
  imports: [CommonModule, FormsModule, TPipe],
  templateUrl: './exercises.component.html',
  styleUrls: ['./exercises.component.scss'],
})
export class AdminExercisesComponent {
  private apiCourses = inject(AdminCoursesService);
  private apiLessons = inject(AdminLessonsService);
  private apiExercises = inject(AdminExercisesService);
  private ai = inject(AdminAiExerciseService);
  private i18n = inject(I18nService);

  loading = signal(false);

  courses = signal<Course[]>([]);
  lessons = signal<Lesson[]>([]);
  exercises = signal<Exercise[]>([]);

  selectedCourseId = signal<number | null>(null);
  selectedLessonId = signal<number | null>(null);

  selectedCourse = computed(() => {
    const id = this.selectedCourseId();
    if (!id) return null;
    return this.courses().find(c => c.id === id) ?? null;
  });

  readonly prettyType = (t: Exercise['type']) => this.i18n.t(`exercise.type.${t}`);
  readonly prettyDifficulty = (d: DifficultyLevel) => this.i18n.t(`difficulty.${d}`);

  readonly types: Exercise['type'][] = ['quiz', 'fill_in', 'writing'];
  readonly difficulties: DifficultyLevel[] = ['easy', 'medium', 'hard'];

  genType = signal<Exercise['type']>('quiz');
  genDiff = signal<DifficultyLevel>('medium');
  genTopic = signal<string>('');
  genCount = signal<number>(3);
  genXp = signal<number>(10);

  generating = signal<boolean>(false);
  genOkCount = signal<number | null>(null);

  filterType = signal<Exercise['type'] | ''>('');
  filterDifficulty = signal<DifficultyLevel | ''>('');
  filterMinXp = signal<number | null>(null);
  filterQuery = signal<string>('');

  page = signal(0);
  size = signal(10);
  sortProp = signal<SortProp>('id');
  sortDir = signal<SortDir>('asc');

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  ngOnInit() {
    this.loadCourses();
  }

  private loadCourses() {
    this.loading.set(true);
    this.apiCourses.list().subscribe({
      next: (c) => {
        this.courses.set(c ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.showError(this.i18n.getServerErrorMessage(err, 'admin.exercises.errors.load') || '');
        this.courses.set([]);
      },
    });
  }

  onCourseChange() {
    const cid = this.selectedCourseId();
    this.selectedLessonId.set(null);
    this.lessons.set([]);
    this.exercises.set([]);
    if (!cid) return;

    this.loading.set(true);
    this.apiLessons.listByCourse(cid).subscribe({
      next: (l) => {
        this.lessons.set(l ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.showError(this.i18n.getServerErrorMessage(err, 'admin.exercises.errors.load') || '');
        this.lessons.set([]);
      },
    });
  }

  onLessonChange() {
    const lid = this.selectedLessonId();
    this.exercises.set([]);
    if (!lid) return;

    this.loading.set(true);
    this.apiExercises.listByLesson(lid).subscribe({
      next: (ex) => {
        this.exercises.set(ex ?? []);
        this.loading.set(false);
        this.page.set(0);
      },
      error: (err) => {
        this.loading.set(false);
        this.showError(this.i18n.getServerErrorMessage(err, 'admin.exercises.errors.load') || '');
        this.exercises.set([]);
      },
    });
  }

  onMinXpChange(v: any) {
    const n = v === '' || v == null ? null : Number(v);
    this.filterMinXp.set(Number.isFinite(n as number) ? (n as number) : null);
    this.page.set(0);
  }

  canGenerate(): boolean {
    const topic = (this.genTopic() || '').trim();
    const lenOk = topic.length >= 1 && topic.length <= 100;
    const count = Number(this.genCount() || 0);
    const xp = Number(this.genXp() ?? 0);
    return !!this.selectedLessonId() && lenOk && count >= 1 && count <= 20 && xp >= 0;
  }

  generateAI() {
    const lid = this.selectedLessonId();
    if (!lid) return;

    this.generating.set(true);
    this.genOkCount.set(null);

    this.ai.generate(lid, {
      exerciseType: this.genType(),
      difficultyLevel: this.genDiff(),
      topic: (this.genTopic() || '').trim(),
      count: Math.max(1, Math.min(20, Number(this.genCount() || 1))),
      xp: Math.max(0, Number(this.genXp() ?? 0)),
    }).subscribe({
      next: (ids: number[]) => {
        const cnt = (ids ?? []).length;
        this.genOkCount.set(cnt);
        this.showSuccess(this.i18n.t('admin.exercises.generator.generated', {count: cnt}));
        this.generating.set(false);
        this.onLessonChange();
      },
      error: (err: any) => {
        let msg =
          this.i18n.getServerErrorMessage(err) ||
          this.i18n.t('admin.exercises.errors.generic');

        if (err?.status === 404) {
          msg = this.i18n.t('admin.exercises.errors.disabled404');
        } else if (err?.status === 503) {
          msg = this.i18n.t('admin.exercises.errors.unavailable503');
        }

        this.showError(msg);
        this.generating.set(false);
      },
    });
  }

  private matchesFilter(e: Exercise): boolean {
    const t = this.filterType();
    const d = this.filterDifficulty();
    const minXp = this.filterMinXp();
    const q = this.filterQuery().trim().toLowerCase();

    const byT = !t || e.type === t;
    const byD = !d || e.difficulty === d;
    const byX = minXp == null || e.xp >= minXp;
    const byQ = !q || (e.question ?? '').toLowerCase().includes(q);
    return byT && byD && byX && byQ;
  }

  filteredRows = computed(() => this.exercises().filter(e => this.matchesFilter(e)));

  private typeOrder: Record<ExerciseType, number> = {quiz: 1, fill_in: 2, writing: 3};
  private diffOrder: Record<DifficultyLevel, number> = {easy: 1, medium: 2, hard: 3};

  private compareValues(prop: SortProp, a: unknown, b: unknown) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;

    if (prop === 'id' || prop === 'xp' || prop === 'orderNumber') {
      const an = Number(a);
      const bn = Number(b);
      return (isNaN(an) ? 0 : an) - (isNaN(bn) ? 0 : bn);
    }

    if (prop === 'difficulty') {
      return (this.diffOrder[a as DifficultyLevel] ?? 0) - (this.diffOrder[b as DifficultyLevel] ?? 0);
    }

    if (prop === 'type') {
      return (this.typeOrder[a as ExerciseType] ?? 0) - (this.typeOrder[b as ExerciseType] ?? 0);
    }

    const as = String(a);
    const bs = String(b);
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

  sortedRows = computed(() => {
    const prop = this.sortProp();
    const dir = this.sortDir();
    const mul = dir === 'asc' ? 1 : -1;
    return [...this.filteredRows()].sort((x, y) => mul * this.compareValues(prop, (x as any)[prop], (y as any)[prop]));
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
    if (this.canPrev()) this.page.set(this.page() - 1);
  }

  next() {
    if (this.canNext()) this.page.set(this.page() + 1);
  }

  onFilterChange() {
    this.page.set(0);
  }

  trackById = (_: number, e: Exercise) => e.id;

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
