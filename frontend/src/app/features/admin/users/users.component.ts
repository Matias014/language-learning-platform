import {Component, ElementRef, OnInit, ViewChild, inject, signal, computed} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
  FormControl,
  FormGroup,
  FormsModule,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {AdminUsersService} from './admin-users.service';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';
import {Observable} from 'rxjs';
import {PageResponse, UserRole, User} from '@shared/models';

type SortDir = 'asc' | 'desc';
type SortProp = 'id' | 'login' | 'email' | 'totalXp' | 'createdAt' | 'lastLoginAt';
type ToastType = 'success' | 'error';

type UserForm = FormGroup<{
  login: FormControl<string>;
  email: FormControl<string>;
  name: FormControl<string | null>;
  surname: FormControl<string | null>;
  role: FormControl<UserRole>;
  password: FormControl<string>;
}>;

@Component({
  standalone: true,
  selector: 'app-admin-users',
  imports: [CommonModule, ReactiveFormsModule, FormsModule, TPipe],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class AdminUsersComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(AdminUsersService);
  private i18n = inject(I18nService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private readonly allowedImageTypes = new Set(['image/png', 'image/jpeg', 'image/webp']);

  @ViewChild('editorTop') editorTop?: ElementRef<HTMLDivElement>;

  loading = signal(true);
  rows = signal<User[]>([]);
  listError = signal<string | null>(null);
  formError = signal<string | null>(null);

  page = signal(0);
  size = signal(5);
  totalElements = signal(0);
  totalPages = signal(0);

  sortProp = signal<SortProp>('id');
  sortDir = signal<SortDir>('desc');

  q = signal<string>('');
  role = signal<'' | UserRole>('');

  editingId = signal<number | null>(null);
  editingOriginal = signal<User | null>(null);
  editorOpen = computed(() => this.editingId() !== null);
  isCreate = computed(() => this.editingId() === 0);
  submitted = signal(false);

  selectedAvatarFile = signal<File | null>(null);
  avatarVer = signal<number>(Date.now());

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  private debounceHandle: any = null;

  form: UserForm = this.fb.group({
    login: this.fb.control('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(64),
        Validators.pattern(/^[a-zA-Z0-9._-]{3,64}$/)
      ]
    }),
    email: this.fb.control('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(255)]
    }),
    name: this.fb.control<string | null>(null),
    surname: this.fb.control<string | null>(null),
    role: this.fb.control<UserRole>('user', {nonNullable: true}),
    password: this.fb.control('', {nonNullable: true})
  });

  private passwordOptionalValidator(ctrl: AbstractControl<string>): ValidationErrors | null {
    const v = ctrl.value ?? '';
    if (!v) return null;
    if (v.length < 8) return {minlength: {requiredLength: 8, actualLength: v.length}};
    if (v.length > 255) return {maxlength: {requiredLength: 255, actualLength: v.length}};
    return null;
  }

  ngOnInit() {
    this.route.queryParamMap.subscribe((pm) => {
      const q = pm.get('q') ?? '';
      const roleParam = (pm.get('role') as '' | UserRole) ?? '';
      const page = this.safeInt(pm.get('page'), 0);
      const size = this.safeInt(pm.get('size'), 5);
      const sortRaw = pm.get('sort') ?? 'id,desc';
      const [propRaw, dirRaw] = sortRaw.split(',');
      const allowed: SortProp[] = ['id', 'login', 'email', 'totalXp', 'createdAt', 'lastLoginAt'];
      const prop = (allowed.includes(propRaw as SortProp) ? propRaw : 'id') as SortProp;
      const dir = (dirRaw === 'asc' ? 'asc' : 'desc') as SortDir;
      const role = roleParam === 'user' || roleParam === 'admin' ? roleParam : '';

      this.q.set(q);
      this.role.set(role);
      this.page.set(Math.max(0, page));
      this.size.set([5, 10, 20, 50].includes(size) ? size : 5);
      this.sortProp.set(prop);
      this.sortDir.set(dir);

      this.load();
    });
  }

  private safeInt(v: string | null, def: number) {
    const n = Number(v);
    return Number.isFinite(n) ? Math.trunc(n) : def;
  }

  private toQueryParams() {
    return {
      q: this.q() || null,
      role: this.role() || null,
      page: this.page(),
      size: this.size(),
      sort: `${this.sortProp()},${this.sortDir()}`
    };
  }

  private updateUrlFromState(replace = false) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: this.toQueryParams(),
      queryParamsHandling: '',
      replaceUrl: replace
    });
  }

  private load() {
    this.loading.set(true);
    this.listError.set(null);
    const sort = `${this.sortProp()},${this.sortDir()}`;
    this.api
      .search({
        q: this.q() || undefined,
        role: (this.role() || undefined) as any,
        page: this.page(),
        size: this.size(),
        sort
      })
      .subscribe({
        next: (res: PageResponse<User>) => {
          this.rows.set(res.content || []);
          this.page.set(res.page);
          this.size.set(res.size);
          this.totalElements.set(res.totalElements);
          this.totalPages.set(res.totalPages);
          this.loading.set(false);
        },
        error: (e) => {
          this.listError.set(this.i18n.getServerErrorMessage(e, 'admin.users.errors.load')!);
          this.showError(this.i18n.getServerErrorMessage(e, 'admin.users.errors.load') || '');
          this.loading.set(false);
        }
      });
  }

  private debounceUrlUpdate() {
    clearTimeout(this.debounceHandle);
    this.debounceHandle = setTimeout(() => {
      this.page.set(0);
      this.updateUrlFromState(true);
    }, 300);
  }

  onQueryChange(v: string) {
    this.q.set(v);
    this.debounceUrlUpdate();
  }

  onRoleChange(v: '' | UserRole) {
    this.role.set(v);
    this.page.set(0);
    this.updateUrlFromState();
  }

  clearFilters() {
    this.q.set('');
    this.role.set('');
    this.page.set(0);
    this.sortProp.set('id');
    this.sortDir.set('desc');
    this.updateUrlFromState();
  }

  toggleSort(prop: SortProp) {
    if (this.sortProp() === prop) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortProp.set(prop);
      this.sortDir.set('asc');
    }
    this.page.set(0);
    this.updateUrlFromState();
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
    this.updateUrlFromState();
  }

  next() {
    if (!this.canNext()) return;
    this.page.set(this.page() + 1);
    this.updateUrlFromState();
  }

  changeSize(size: number) {
    this.size.set(Number(size));
    this.page.set(0);
    this.updateUrlFromState();
  }

  private scrollToEditor() {
    queueMicrotask(() => this.editorTop?.nativeElement.scrollIntoView({block: 'start'}));
  }

  addNew() {
    this.submitted.set(false);
    this.formError.set(null);
    this.editingId.set(0);
    this.editingOriginal.set(null);
    this.form.reset({login: '', email: '', name: '', surname: '', role: 'user', password: ''});
    this.form.controls.login.setValidators([
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(64),
      Validators.pattern(/^[a-zA-Z0-9._-]{3,64}$/)
    ]);
    this.form.controls.email.setValidators([Validators.required, Validators.email, Validators.maxLength(255)]);
    this.form.controls.name.setValidators([Validators.required, Validators.maxLength(100)]);
    this.form.controls.surname.setValidators([Validators.required, Validators.maxLength(100)]);
    this.form.controls.password.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(255)]);
    this.form.updateValueAndValidity();
    this.selectedAvatarFile.set(null);
    this.scrollToEditor();
  }

  edit(u: User) {
    this.submitted.set(false);
    this.formError.set(null);
    this.editingId.set(u.id);
    this.editingOriginal.set(u);
    this.form.reset({
      login: u.login ?? '',
      email: u.email ?? '',
      name: u.name ?? '',
      surname: u.surname ?? '',
      role: (u.role ?? 'user') as UserRole,
      password: ''
    });
    this.form.controls.login.setValidators([
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(64),
      Validators.pattern(/^[a-zA-Z0-9._-]{3,64}$/)
    ]);
    this.form.controls.email.setValidators([Validators.required, Validators.email, Validators.maxLength(255)]);
    this.form.controls.name.setValidators([Validators.maxLength(100)]);
    this.form.controls.surname.setValidators([Validators.maxLength(100)]);
    this.form.controls.password.setValidators([this.passwordOptionalValidator.bind(this)]);
    this.form.updateValueAndValidity();
    this.selectedAvatarFile.set(null);
    this.avatarVer.set(Date.now());
    this.scrollToEditor();
  }

  cancel() {
    this.editingId.set(null);
    this.editingOriginal.set(null);
    this.form.reset();
    this.submitted.set(false);
    this.formError.set(null);
    this.selectedAvatarFile.set(null);
  }

  private handleFormError(e: any, fallbackKey: string) {
    const applied = this.i18n.applyFieldErrors(this.form, e, {
      newLogin: 'login',
      newEmail: 'email',
      newPassword: 'password'
    });
    if (!applied) this.formError.set(this.i18n.getServerErrorMessage(e, fallbackKey)!);
    const msg = this.i18n.getServerErrorMessage(e, fallbackKey) || '';
    if (msg) this.showError(msg);
  }

  private runSequential(calls: Observable<any>[], onDone: () => void, onErr: (e: any) => void) {
    if (!calls.length) {
      onDone();
      return;
    }
    const [head, ...tail] = calls;
    head.subscribe({next: () => this.runSequential(tail, onDone, onErr), error: onErr});
  }

  save() {
    this.submitted.set(true);
    if (this.form.invalid) return;

    const id = this.editingId();

    if (id && id > 0) {
      const orig = this.editingOriginal();
      const v = this.form.getRawValue();
      const calls: Observable<any>[] = [];
      if (orig && v.login !== (orig.login ?? '')) calls.push(this.api.changeLoginAdmin(id, v.login));
      if (orig && v.email !== (orig.email ?? '')) calls.push(this.api.changeEmailAdmin(id, v.email));
      if (v.password && v.password.trim().length > 0) calls.push(this.api.setPasswordAdmin(id, v.password));
      const patch: any = {};
      if (orig && v.name !== (orig.name ?? '')) patch.name = v.name ?? '';
      if (orig && v.surname !== (orig.surname ?? '')) patch.surname = v.surname ?? '';
      if (Object.keys(patch).length > 0) calls.push(this.api.update(id, patch));

      this.runSequential(
        calls,
        () => {
          const file = this.selectedAvatarFile();
          if (!file) {
            this.showSuccess(this.i18n.t('admin.users.alerts.updated'));
            this.cancel();
            this.load();
            return;
          }
          this.api.uploadAvatar(id, file).subscribe({
            next: (updated) => {
              this.editingOriginal.set(updated);
              this.avatarVer.set(Date.now());
              this.selectedAvatarFile.set(null);
              this.showSuccess(this.i18n.t('admin.users.alerts.updated'));
              this.cancel();
              this.load();
            },
            error: (e) => this.handleFormError(e, 'admin.users.errors.update')
          });
        },
        (e) => this.handleFormError(e, 'admin.users.errors.update')
      );
    } else {
      const v = this.form.getRawValue();
      const body = {
        login: v.login,
        email: v.email,
        password: v.password,
        name: v.name ?? '',
        surname: v.surname ?? ''
      };
      this.api.create(body).subscribe({
        next: (created) => {
          const file = this.selectedAvatarFile();
          if (!created?.id || !file) {
            this.showSuccess(this.i18n.t('admin.users.alerts.created'));
            this.cancel();
            this.load();
            return;
          }
          this.api.uploadAvatar(created.id, file).subscribe({
            next: () => {
              this.avatarVer.set(Date.now());
              this.selectedAvatarFile.set(null);
              this.showSuccess(this.i18n.t('admin.users.alerts.created'));
              this.cancel();
              this.load();
            },
            error: (e) => {
              this.formError.set(this.i18n.getServerErrorMessage(e, 'admin.users.errors.create')!);
              this.showError(this.i18n.getServerErrorMessage(e, 'admin.users.errors.create') || '');
              this.load();
            }
          });
        },
        error: (e) => this.handleFormError(e, 'admin.users.errors.create')
      });
    }
  }

  remove(id: number) {
    if (!confirm(this.i18n.t('admin.users.confirmDelete'))) return;
    this.api.delete(id).subscribe({
      next: () => {
        this.showSuccess(this.i18n.t('admin.users.alerts.deleted'));
        this.load();
      },
      error: (e) => {
        const msg = this.i18n.getServerErrorMessage(e, 'admin.users.errors.delete') || '';
        this.listError.set(msg || null);
        this.showError(msg);
      }
    });
  }

  onAvatarFileChange(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;

    if (!file) {
      this.selectedAvatarFile.set(null);
      return;
    }

    if (!this.allowedImageTypes.has(file.type)) {
      this.selectedAvatarFile.set(null);
      this.showError('Nieobsługiwany typ pliku. Dozwolone: PNG, JPG, WEBP.');
      if (input) input.value = '';
      return;
    }

    this.selectedAvatarFile.set(file);
    if (input) input.value = '';
  }

  onAvatarError(e: Event) {
    (e.target as HTMLImageElement).src = 'assets/avatar.svg';
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
