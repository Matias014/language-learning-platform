import {Component, OnInit, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs/operators';
import {TPipe} from '@shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';
import {AdminAchievementsService} from './admin-achievements.service';
import {Achievement, CreateAchievementRequest, UpdateAchievementRequest} from '@shared/models/achievement.model';

type SortDir = 'asc' | 'desc';
type SortProp = 'id' | 'title' | 'requiredXp';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-achievements',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, TPipe],
  templateUrl: './achievements.component.html',
  styleUrls: ['./achievements.component.scss'],
})
export class AdminAchievementsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(AdminAchievementsService);
  private i18n = inject(I18nService);

  private readonly allowedImageTypes = new Set(['image/png', 'image/jpeg', 'image/webp']);

  loading = signal(false);
  error = signal<string | null>(null);

  baseRows = signal<Achievement[]>([]);
  filterTitle = signal('');
  filterMinXp = signal<number | null>(null);

  rows = computed(() => {
    const title = this.filterTitle().trim().toLowerCase();
    const minXp = this.filterMinXp();
    return this.baseRows().filter(a => {
      const byTitle = !title || a.title.toLowerCase().includes(title);
      const byXp = minXp == null || (a.requiredXp ?? 0) >= minXp;
      return byTitle && byXp;
    });
  });

  sortProp = signal<SortProp>('id');
  sortDir = signal<SortDir>('desc');

  page = signal(0);
  size = signal(10);

  private compareValues(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'id' || prop === 'requiredXp') {
      const an = typeof a === 'number' ? a : Number(a);
      const bn = typeof b === 'number' ? b : Number(b);
      return an - bn;
    }
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

  totalElements = computed(() => this.rows().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
  });

  editingId = signal<number | null>(null);
  isCreate = computed(() => this.editingId() === 0);

  currentEditingAchievement = computed(() => {
    const id = this.editingId();
    if (!id || id <= 0) return null;
    return this.baseRows().find(r => r.id === id) ?? null;
  });

  uploadingIcon = signal(false);
  iconVer = signal<number>(Date.now());

  private selectedIconFile = signal<File | null>(null);
  private selectedIconUrl = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(2000)]],
    requiredXp: [0, [Validators.required, Validators.min(0)]],
  });

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  ngOnInit(): void {
    this.loadAll();
  }

  trackById = (_: number, row: Achievement) => row.id;

  onMinXpChange(v: any) {
    const n = v === '' || v == null ? null : Number(v);
    this.filterMinXp.set(Number.isFinite(n as number) ? (n as number) : null);
    this.page.set(0);
  }

  clearFilters() {
    this.filterTitle.set('');
    this.filterMinXp.set(null);
    this.page.set(0);
  }

  private loadAll(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (list) => {
        this.baseRows.set(list ?? []);
        this.page.set(0);
      },
      error: (e) => {
        this.baseRows.set([]);
        this.error.set('admin.achievements.errors.load');
        const msg = this.i18n.getServerErrorMessage(e, 'admin.achievements.errors.load') || '';
        if (msg) this.showError(msg);
      },
    });
  }

  private clearSelectedIcon(): void {
    const url = this.selectedIconUrl();
    if (url) URL.revokeObjectURL(url);
    this.selectedIconUrl.set(null);
    this.selectedIconFile.set(null);
  }

  addNew(): void {
    this.clearSelectedIcon();
    this.editingId.set(0);
    this.form.reset({title: '', description: '', requiredXp: 0});
    this.iconVer.set(Date.now());
  }

  edit(a: Achievement): void {
    this.clearSelectedIcon();
    this.editingId.set(a.id);
    this.form.reset({
      title: a.title,
      description: a.description ?? '',
      requiredXp: a.requiredXp ?? 0,
    });
    this.iconVer.set(Date.now());
  }

  cancel(): void {
    this.clearSelectedIcon();
    this.editingId.set(null);
    this.form.reset();
  }

  iconPreviewSrc(): string {
    const pending = this.selectedIconUrl();
    if (pending) return pending;
    const cur = this.currentEditingAchievement();
    const base = cur?.iconPath || 'assets/avatar.svg';
    const ver = this.iconVer();
    return base + (ver ? `?${ver}` : '');
  }

  onIconFileChange(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.clearSelectedIcon();

    if (!this.allowedImageTypes.has(file.type)) {
      this.showError('Nieobsługiwany typ pliku. Dozwolone: PNG, JPG, WEBP.');
      if (input) input.value = '';
      return;
    }

    this.selectedIconFile.set(file);
    this.selectedIconUrl.set(URL.createObjectURL(file));

    if (input) input.value = '';
  }

  save(): void {
    if (this.form.invalid) return;

    const id = this.editingId();
    const v = this.form.getRawValue();

    const bodyBase = {
      title: v.title.trim(),
      description: v.description?.trim() || null,
      requiredXp: Number.isFinite(v.requiredXp) ? Math.max(0, Math.trunc(Number(v.requiredXp))) : 0,
    };

    const onError = (err: any) => {
      const applied = this.i18n.applyFieldErrors(this.form, err);
      if (!applied) {
        const msg = this.i18n.getServerErrorMessage(err, 'errors.badRequest') || this.i18n.t('errors.badRequest');
        this.showError(msg);
      }
    };

    const afterSave = (savedId: number) => {
      const file = this.selectedIconFile();
      if (!file) {
        this.cancel();
        this.loadAll();
        return;
      }
      this.uploadingIcon.set(true);
      this.api.uploadIcon(savedId, file)
        .pipe(finalize(() => this.uploadingIcon.set(false)))
        .subscribe({
          next: () => {
            this.clearSelectedIcon();
            this.iconVer.set(Date.now());
            this.cancel();
            this.loadAll();
          },
          error: (err) => {
            const msg = this.i18n.getServerErrorMessage(err, 'errors.default') || '';
            if (msg) this.showError(msg);
            this.cancel();
            this.loadAll();
          },
        });
    };

    if (!id || id === 0) {
      const body: CreateAchievementRequest = bodyBase;
      this.api.create(body).subscribe({
        next: (created) => {
          if (!created?.id) {
            this.cancel();
            this.loadAll();
            return;
          }
          afterSave(created.id);
        },
        error: onError
      });
    } else {
      const patch: UpdateAchievementRequest = bodyBase;
      this.api.update(id, patch).subscribe({
        next: () => afterSave(id),
        error: onError
      });
    }
  }

  onIconError(e: Event) {
    (e.target as HTMLImageElement).src = 'assets/avatar.svg';
  }

  remove(id: number): void {
    if (!confirm(this.i18n.t('admin.achievements.confirmDelete'))) return;
    this.api.delete(id).subscribe({
      next: () => this.loadAll(),
      error: (err) => {
        const msg = this.i18n.getServerErrorMessage(err, 'common.deleteFailed') || this.i18n.t('common.deleteFailed');
        this.showError(msg);
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
