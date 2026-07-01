import {Component, DestroyRef, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {FormsModule} from '@angular/forms';
import {HttpErrorResponse} from '@angular/common/http';
import {forkJoin} from 'rxjs';
import {finalize} from 'rxjs/operators';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TPipe} from '@shared/i18n/t.pipe';
import {AdminCoursesService} from './admin-courses.service';
import {Course, CreateCourseRequest, UpdateCourseRequest} from '@app/shared/models/course.model';
import {normalizeLang} from '@app/shared/utils/locale.util';
import {AdminLanguagesService} from '../languages/admin-languages.service';
import {AdminProficiencyLevelsService} from '../proficiency-levels/admin-proficiency-levels.service';
import {Language} from '@app/shared/models/language.model';
import {ProficiencyLevel} from '@app/shared/models/proficiency-level.model';
import {I18nService} from '@shared/i18n/i18n.service';

type SortDir = 'asc' | 'desc';
type SortProp = 'id' | 'learningLanguageCode' | 'fromLanguageCode' | 'title' | 'levelCode' | 'createdAt' | 'updatedAt';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-courses',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TPipe],
  templateUrl: './courses.component.html',
  styleUrls: ['./courses.component.scss'],
})
export class AdminCoursesComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(AdminCoursesService);
  private apiLang = inject(AdminLanguagesService);
  private apiLevels = inject(AdminProficiencyLevelsService);
  private i18n = inject(I18nService);
  private destroyRef = inject(DestroyRef);

  private readonly allowedImageTypes = new Set(['image/png', 'image/jpeg', 'image/webp']);

  loading = signal(true);
  error = signal<string | null>(null);

  baseRows = signal<Course[]>([]);
  filteredRows = signal<Course[]>([]);
  languages = signal<Language[]>([]);
  levels = signal<ProficiencyLevel[]>([]);

  languageCodes = computed(() => this.languages().map(l => l.code).sort());
  levelCodes = computed<string[]>(() => this.levels().map(l => l.code));

  filterLearning = signal('');
  filterFrom = signal('');
  filterLevel = signal('');
  filterTitle = signal('');

  learningOptions = computed(() =>
    Array.from(new Set(this.baseRows().map(c => c.learningLanguageCode).filter(Boolean))).sort()
  );
  fromOptions = computed(() =>
    Array.from(new Set(this.baseRows().map(c => c.fromLanguageCode).filter(Boolean))).sort()
  );
  levelOptions = computed(() =>
    Array.from(new Set(this.baseRows().map(c => c.levelCode).filter(Boolean))).sort()
  );

  sortProp = signal<SortProp>('id');
  sortDir = signal<SortDir>('desc');

  page = signal(0);
  size = signal(10);

  rowsSorted = computed(() => {
    const prop = this.sortProp();
    const dir = this.sortDir();
    const mul = dir === 'asc' ? 1 : -1;
    const rows = this.filteredRows();
    return [...rows].sort((a, b) => mul * this.compareValues((a as any)[prop], (b as any)[prop], prop));
  });

  pageRows = computed(() => {
    const start = this.page() * this.size();
    const end = start + this.size();
    return this.rowsSorted().slice(start, end);
  });

  totalElements = computed(() => this.filteredRows().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
  });

  editingId = signal<number | null>(null);
  isEdit = computed(() => (this.editingId() ?? 0) > 0);

  currentEditingCourse = computed(() => {
    const id = this.editingId();
    if (!id || id <= 0) return null;
    return this.baseRows().find(r => r.id === id) ?? null;
  });

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  uploadingIcon = signal(false);
  countryIconVer = signal<number>(Date.now());

  private selectedCountryIconFile = signal<File | null>(null);
  private selectedCountryIconUrl = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    learningLanguageCode: ['', [Validators.required]],
    fromLanguageCode: ['', [Validators.required]],
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(10000)]],
    levelCode: ['' as string, []],
  });

  ngOnInit() {
    this.loadRefsAndData();
  }

  private loadRefsAndData() {
    this.error.set(null);
    this.loading.set(true);
    forkJoin({
      langs: this.apiLang.list(),
      levels: this.apiLevels.list(),
      courses: this.api.list(),
    })
      .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.languages.set(res.langs || []);
          this.levels.set(res.levels || []);
          this.baseRows.set(res.courses || []);
          this.applyFilters();
        },
        error: err => {
          const msg = this.i18n.getServerErrorMessage(err, 'admin.courses.errors.load') || '';
          this.error.set('admin.courses.errors.load');
          if (msg) this.showError(msg);
          this.baseRows.set([]);
          this.filteredRows.set([]);
        },
      });
  }

  private compareValues(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'createdAt' || prop === 'updatedAt') {
      const an = typeof a === 'string' ? Date.parse(a) : 0;
      const bn = typeof b === 'string' ? Date.parse(b) : 0;
      return an - bn;
    }
    if (typeof a === 'number' && typeof b === 'number') return a - b;
    const as = String(a);
    const bs = String(b);
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

  applyFilters() {
    const t = this.filterLearning();
    const s = this.filterFrom();
    const l = this.filterLevel();
    const ti = this.filterTitle().trim().toLowerCase();
    const out = this.baseRows().filter(c => {
      const byT = !t || (c.learningLanguageCode ?? '') === t;
      const byS = !s || (c.fromLanguageCode ?? '') === s;
      const byL = !l || (c.levelCode ?? '') === l;
      const byTi = !ti || (c.title ?? '').toLowerCase().includes(ti);
      return byT && byS && byL && byTi;
    });
    this.filteredRows.set(out);
    this.page.set(0);
  }

  clearFilters() {
    this.filterLearning.set('');
    this.filterFrom.set('');
    this.filterLevel.set('');
    this.filterTitle.set('');
    this.applyFilters();
  }

  private clearSelectedCountryIcon(): void {
    const url = this.selectedCountryIconUrl();
    if (url) URL.revokeObjectURL(url);
    this.selectedCountryIconUrl.set(null);
    this.selectedCountryIconFile.set(null);
  }

  addNew() {
    this.clearSelectedCountryIcon();
    this.editingId.set(0);
    this.form.reset({
      learningLanguageCode: '',
      fromLanguageCode: '',
      title: '',
      description: '',
      levelCode: '',
    });
    this.form.controls.levelCode.setValidators([Validators.required]);
    this.form.controls.levelCode.updateValueAndValidity();
    this.countryIconVer.set(Date.now());
  }

  edit(c: Course) {
    this.clearSelectedCountryIcon();
    this.editingId.set(c.id);
    this.form.reset({
      learningLanguageCode: c.learningLanguageCode ?? '',
      fromLanguageCode: c.fromLanguageCode ?? '',
      title: c.title ?? '',
      description: c.description ?? '',
      levelCode: c.levelCode ?? '',
    });
    this.form.controls.levelCode.clearValidators();
    this.form.controls.levelCode.updateValueAndValidity();
    this.countryIconVer.set(Date.now());
  }

  cancel() {
    this.clearSelectedCountryIcon();
    this.editingId.set(null);
    this.form.reset();
  }

  countryIconPreviewSrc(): string {
    const pending = this.selectedCountryIconUrl();
    if (pending) return pending;
    const cur = this.currentEditingCourse();
    const base = cur?.countryIconPath || 'assets/avatar.svg';
    const ver = this.countryIconVer();
    return base + (ver ? `?${ver}` : '');
  }

  onCountryIconFileChange(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.clearSelectedCountryIcon();

    if (!this.allowedImageTypes.has(file.type)) {
      this.showError('Nieobsługiwany typ pliku. Dozwolone: PNG, JPG, WEBP.');
      if (input) input.value = '';
      return;
    }

    this.selectedCountryIconFile.set(file);
    this.selectedCountryIconUrl.set(URL.createObjectURL(file));

    if (input) input.value = '';
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const base = {
      learningLanguageCode: normalizeLang(v.learningLanguageCode),
      fromLanguageCode: normalizeLang(v.fromLanguageCode),
      title: String(v.title).trim(),
      description: v.description?.trim() || null,
      levelCode: v.levelCode ? String(v.levelCode).trim().toUpperCase() : null,
    };

    const id = this.editingId();

    const afterSave = (savedId: number, successMsgKey: string) => {
      const file = this.selectedCountryIconFile();
      if (!file) {
        this.showSuccess(this.i18n.t(successMsgKey));
        this.cancel();
        this.loadRefsAndData();
        return;
      }

      this.uploadingIcon.set(true);
      this.api.uploadCountryIcon(savedId, file)
        .pipe(finalize(() => this.uploadingIcon.set(false)), takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => {
            this.clearSelectedCountryIcon();
            this.countryIconVer.set(Date.now());
            this.showSuccess(this.i18n.t(successMsgKey));
            this.cancel();
            this.loadRefsAndData();
          },
          error: err => {
            const msg = this.i18n.getServerErrorMessage(err, 'admin.courses.errors.save') || '';
            if (msg) this.showError(msg);
            this.showSuccess(this.i18n.t(successMsgKey));
            this.cancel();
            this.loadRefsAndData();
          },
        });
    };

    if (!id || id === 0) {
      const body: CreateCourseRequest = {
        learningLanguageCode: base.learningLanguageCode,
        fromLanguageCode: base.fromLanguageCode,
        title: base.title,
        description: base.description,
        levelCode: base.levelCode as string,
      };
      this.api.create(body).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (created) => {
          if (!created?.id) {
            this.showSuccess(this.i18n.t('admin.courses.alerts.created'));
            this.cancel();
            this.loadRefsAndData();
            return;
          }
          afterSave(created.id, 'admin.courses.alerts.created');
        },
        error: err => this.handleSaveError(err),
      });
    } else {
      const patch: UpdateCourseRequest = {
        learningLanguageCode: base.learningLanguageCode,
        fromLanguageCode: base.fromLanguageCode,
        title: base.title,
        description: base.description,
        levelCode: base.levelCode ?? undefined,
      };
      this.api.update(id, patch).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: () => afterSave(id, 'admin.courses.alerts.updated'),
        error: err => this.handleSaveError(err),
      });
    }
  }

  onCountryIconError(e: Event) {
    (e.target as HTMLImageElement).src = 'assets/avatar.svg';
  }

  private handleSaveError(err: unknown) {
    const e = err as HttpErrorResponse;
    const applied = this.i18n.applyFieldErrors(this.form, e);
    const msg = this.i18n.getServerErrorMessage(e, 'admin.courses.errors.save') || '';
    this.error.set('admin.courses.errors.save');
    if (msg) this.showError(msg);
    if (!applied) this.form.markAllAsTouched();
  }

  remove(id: number) {
    const conf = confirm(this.i18n.t('admin.courses.confirmDelete'));
    if (!conf) return;
    this.api.delete(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.showSuccess(this.i18n.t('admin.courses.alerts.deleted'));
        this.loadRefsAndData();
      },
      error: e => {
        const msg = this.i18n.getServerErrorMessage(e, 'admin.courses.errors.delete') || '';
        this.error.set('admin.courses.errors.delete');
        if (msg) this.showError(msg);
        this.loadRefsAndData();
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

  trackById = (_: number, c: Course) => c.id;

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
