import {Component, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {forkJoin, Observable, of} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {TPipe} from '@shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';
import {AuthService} from '@core/auth/auth.service';
import {ProfileService} from '@features/profile/profile.service';
import {
  User,
  Level,
  UpdateUserRequest,
  ChangeLoginRequest,
  ChangeEmailRequest,
  ChangePasswordRequest,
  UserAchievement,
} from '@shared/models';

type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-profile',
  imports: [CommonModule, ReactiveFormsModule, FormsModule, TPipe, RouterLink],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
})
export class ProfileComponent {
  private fb = inject(FormBuilder);
  private i18n = inject(I18nService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private profile = inject(ProfileService);

  loading = signal(true);
  saving = signal(false);
  exporting = signal(false);

  error = signal<string | null>(null);
  success = signal<string | null>(null);

  editMode = signal(false);

  me = signal<User | null>(null);
  levels = signal<Level[]>([]);
  achievements = signal<UserAchievement[]>([]);

  deleteOpen = signal(false);
  deleting = signal(false);

  changeLoginOpen = signal(false);
  changeLoginPwd = signal('');
  changeLoginSubmitting = signal(false);
  changeLoginError = signal<string | null>(null);

  changeEmailOpen = signal(false);
  changeEmailPwd = signal('');
  changeEmailSubmitting = signal(false);
  changeEmailError = signal<string | null>(null);

  private avatarFile: File | null = null;
  private avatarFileNameSig = signal<string>('');

  private stagedPatchBody: UpdateUserRequest | null = null;
  private stagedNextLogin: string | null = null;
  private stagedNextEmail: string | null = null;
  private stagedAvatarFile: File | null = null;
  private stagedPwdChange: { current: string; next: string } | null = null;

  isAdmin = computed(() => String(this.me()?.role || '').toLowerCase() === 'admin');

  currentLevel = computed(() => {
    const user = this.me();
    const levels = this.levels();
    if (!user || !levels.length) return 0;
    const totalXp = user.totalXp ?? 0;
    const sorted = levels.slice().sort((a, b) => a.requiredXp - b.requiredXp);
    let current = 0;
    for (const lvl of sorted) {
      if (totalXp >= lvl.requiredXp) current = lvl.level;
      else break;
    }
    return current;
  });

  xpToNext = computed(() => {
    const user = this.me();
    const levels = this.levels();
    if (!user || !levels.length) return 0;
    const totalXp = user.totalXp ?? 0;
    const sorted = levels.slice().sort((a, b) => a.requiredXp - b.requiredXp);
    for (const lvl of sorted) if (totalXp < lvl.requiredXp) return Math.max(0, lvl.requiredXp - totalXp);
    return 0;
  });

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  form = this.fb.group({
    login: ['', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(64),
      Validators.pattern(/^[A-Za-z0-9._-]{3,64}$/)
    ]],
    name: ['', [Validators.maxLength(100)]],
    surname: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    pwdCurrent: [''],
    pwdNew: [''],
    pwdConfirm: [''],
    avatar: [''],
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.profile.loadBundle().subscribe({
      next: ({me, achievements, levels}) => {
        this.me.set(me);
        this.levels.set(levels || []);
        this.achievements.set(achievements || []);
        this.form.patchValue({
          login: me.login,
          email: me.email,
          name: me.name ?? '',
          surname: me.surname ?? '',
        });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.showError(this.i18n.t('common.loadFailed'));
      },
    });
  }

  startEdit(): void {
    this.success.set(null);
    this.error.set(null);
    this.editMode.set(true);
  }

  cancelEdit(): void {
    const u = this.me();
    if (u) {
      this.form.patchValue({
        login: u.login,
        email: u.email,
        name: u.name ?? '',
        surname: u.surname ?? '',
        pwdCurrent: '',
        pwdNew: '',
        pwdConfirm: '',
        avatar: '',
      });
    }
    this.avatarFile = null;
    this.avatarFileNameSig.set('');
    this.stagedPatchBody = null;
    this.stagedNextLogin = null;
    this.stagedNextEmail = null;
    this.stagedAvatarFile = null;
    this.stagedPwdChange = null;
    this.editMode.set(false);
  }

  save(): void {
    this.success.set(null);
    this.error.set(null);
    if (!this.me() || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const current = this.me()!;
    const nextLogin = String(this.form.value.login ?? '').trim();
    const nextEmail = String(this.form.value.email ?? '').trim();
    const nextName = String(this.form.value.name ?? '').trim();
    const nextSurname = String(this.form.value.surname ?? '').trim();

    const changedLogin = !!nextLogin && nextLogin !== current.login;
    const changedEmail = !!nextEmail && nextEmail !== current.email;

    const patchBody: UpdateUserRequest = {};
    const curName = current.name ?? '';
    const curSurname = current.surname ?? '';

    if (nextName !== curName) patchBody.name = nextName === '' ? null : nextName;
    if (nextSurname !== curSurname) patchBody.surname = nextSurname === '' ? null : nextSurname;

    const changedProfile = Object.keys(patchBody).length > 0;

    const hasAvatar = !!this.avatarFile;

    const pwdCurrent = String(this.form.value.pwdCurrent ?? '');
    const pwdNew = String(this.form.value.pwdNew ?? '');
    const pwdConfirm = String(this.form.value.pwdConfirm ?? '');
    const wantsPwdChange = !!(pwdCurrent || pwdNew || pwdConfirm);

    if (wantsPwdChange) {
      if (!pwdCurrent || !pwdNew || !pwdConfirm) {
        this.showError(this.i18n.t('profile.alerts.providePassword'));
        return;
      }
      if (pwdNew.length < 8 || pwdNew.length > 255) {
        this.showError(this.i18n.t('profile.alerts.passwordLength'));
        return;
      }
      if (pwdNew !== pwdConfirm) {
        this.showError(this.i18n.t('profile.alerts.passwordMismatch'));
        return;
      }
      this.stagedPwdChange = {current: pwdCurrent, next: pwdNew};
    } else {
      this.stagedPwdChange = null;
    }

    if (!changedProfile && !changedLogin && !changedEmail && !hasAvatar && !this.stagedPwdChange) {
      this.showSuccess(this.i18n.t('profile.alerts.noChanges'));
      return;
    }

    this.stagedPatchBody = changedProfile ? patchBody : null;
    this.stagedNextLogin = changedLogin ? nextLogin : null;
    this.stagedNextEmail = changedEmail ? nextEmail : null;
    this.stagedAvatarFile = this.avatarFile;

    if (this.stagedNextLogin) {
      this.changeLoginError.set(null);
      this.changeLoginPwd.set('');
      this.changeLoginOpen.set(true);
      return;
    }

    if (this.stagedNextEmail) {
      this.changeEmailError.set(null);
      this.changeEmailPwd.set('');
      this.changeEmailOpen.set(true);
      return;
    }

    this.saving.set(true);
    this.runStagedChanges('').subscribe({
      next: () => this.afterChangesApplied(),
      error: (err) => this.afterChangesFailed(err),
    });
  }

  isChangingBoth(): boolean {
    return !!this.stagedNextLogin && !!this.stagedNextEmail;
  }

  confirmChangeLogin(): void {
    if (!this.me()) return;
    const pwd = this.changeLoginPwd().trim();
    if (!pwd) {
      this.changeLoginError.set(this.i18n.t('profile.alerts.providePassword'));
      return;
    }
    this.changeLoginSubmitting.set(true);
    this.runStagedChanges(pwd).subscribe({
      next: () => {
        this.changeLoginSubmitting.set(false);
        this.changeLoginOpen.set(false);
        this.afterChangesApplied();
      },
      error: (err: HttpErrorResponse) => {
        this.i18n.applyFieldErrors(this.form, err, {currentPassword: 'pwdCurrent'});
        const mapped = this.i18n.getServerErrorMessage(err);
        const msg =
          mapped ||
          (err.status === 409 && this.i18n.t('profile.alerts.loginTaken')) ||
          ((err.status === 403 || err.status === 401) && this.i18n.t('profile.alerts.wrongPassword')) ||
          this.i18n.t('profile.alerts.saveFailed');
        this.changeLoginError.set(String(msg));
        this.changeLoginSubmitting.set(false);
      },
    });
  }

  closeChangeLogin(): void {
    if (!this.changeLoginSubmitting()) this.changeLoginOpen.set(false);
  }

  confirmChangeEmail(): void {
    if (!this.me()) return;
    const pwd = this.changeEmailPwd().trim();
    if (!pwd) {
      this.changeEmailError.set(this.i18n.t('profile.alerts.providePassword'));
      return;
    }
    this.changeEmailSubmitting.set(true);
    this.runStagedChanges(pwd).subscribe({
      next: () => {
        this.changeEmailSubmitting.set(false);
        this.changeEmailOpen.set(false);
        this.afterChangesApplied();
      },
      error: (err: HttpErrorResponse) => {
        this.i18n.applyFieldErrors(this.form, err, {currentPassword: 'pwdCurrent'});
        const mapped = this.i18n.getServerErrorMessage(err);
        const msg =
          mapped ||
          (err.status === 409 && this.i18n.t('profile.alerts.emailTaken')) ||
          ((err.status === 403 || err.status === 401) && this.i18n.t('profile.alerts.wrongPassword')) ||
          this.i18n.t('profile.alerts.saveFailed');
        this.changeEmailError.set(String(msg));
        this.changeEmailSubmitting.set(false);
      },
    });
  }

  closeChangeEmail(): void {
    if (!this.changeEmailSubmitting()) this.changeEmailOpen.set(false);
  }

  private runStagedChanges(passwordForAuth: string): Observable<unknown> {
    const requests: Array<Observable<unknown>> = [];
    if (this.stagedPatchBody) requests.push(this.profile.updateMe({...this.stagedPatchBody}));
    if (this.stagedNextLogin) {
      const body: ChangeLoginRequest = {newLogin: this.stagedNextLogin, currentPassword: passwordForAuth};
      requests.push(this.profile.changeLogin(body));
    }
    if (this.stagedNextEmail) {
      const body: ChangeEmailRequest = {newEmail: this.stagedNextEmail, currentPassword: passwordForAuth};
      requests.push(this.profile.changeEmail(body));
    }
    if (this.stagedPwdChange) {
      const body: ChangePasswordRequest = {
        currentPassword: this.stagedPwdChange.current,
        newPassword: this.stagedPwdChange.next,
      };
      requests.push(this.profile.changePassword(body));
    }
    if (this.stagedAvatarFile) requests.push(this.profile.uploadAvatar(this.stagedAvatarFile));
    if (!requests.length) return of(null);
    return forkJoin(requests);
  }

  private afterChangesApplied(): void {
    this.auth.refreshAccessToken().subscribe({
      next: () => this.finishAfterRefresh(),
      error: () => this.finishAfterRefresh(),
    });
  }

  private finishAfterRefresh(): void {
    this.auth.me(true).subscribe({
      next: (fresh) => {
        this.me.set(fresh);
        this.saving.set(false);
        this.editMode.set(false);
        this.avatarFile = null;
        this.avatarFileNameSig.set('');
        this.stagedPatchBody = null;
        this.stagedNextLogin = null;
        this.stagedNextEmail = null;
        this.stagedAvatarFile = null;
        this.stagedPwdChange = null;
        this.form.patchValue({pwdCurrent: '', pwdNew: '', pwdConfirm: '', avatar: ''});
        this.showSuccess(this.i18n.t('profile.alerts.saved'));
      },
      error: () => {
        this.saving.set(false);
        this.showError(this.i18n.t('profile.alerts.saveFailed'));
      },
    });
  }

  private afterChangesFailed(err: any): void {
    this.i18n.applyFieldErrors(this.form, err, {
      file: 'avatar',
      login: 'login',
      email: 'email',
      currentPassword: 'pwdCurrent',
      newPassword: 'pwdNew'
    });
    const wrongPwdForPasswordChange = (err?.status === 403 || err?.status === 401) && !!this.stagedPwdChange;
    const mapped = this.i18n.getServerErrorMessage(err);
    const msg =
      mapped ||
      (wrongPwdForPasswordChange && this.i18n.t('profile.alerts.wrongPassword')) ||
      this.i18n.t('profile.alerts.saveFailed');
    this.showError(String(msg));
    this.saving.set(false);
  }

  exportData(): void {
    const me = this.me();
    if (!me || this.exporting()) return;
    this.exporting.set(true);
    this.profile.exportPdf(this.preferredLangTag()).subscribe({
      next: (resp) => {
        const ct = (resp.headers.get('content-type') || '').toLowerCase();
        if (!ct.includes('application/pdf') || !(resp.body instanceof Blob)) {
          this.exporting.set(false);
          this.showError(this.i18n.t('profile.alerts.exportFail'));
          return;
        }
        const dispo = resp.headers.get('content-disposition') || '';
        const filename = this.pickFilenameFromContentDisposition(dispo, me.login);
        const a = document.createElement('a');
        const url = URL.createObjectURL(resp.body);
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
        this.showSuccess(this.i18n.t('profile.alerts.exportOk'));
      },
      error: () => {
        this.exporting.set(false);
        this.showError(this.i18n.t('profile.alerts.exportFail'));
      },
    });
  }

  openDelete(): void {
    this.deleteOpen.set(true);
  }

  closeDelete(): void {
    if (!this.deleting()) this.deleteOpen.set(false);
  }

  confirmDelete(): void {
    if (!this.me()) return;
    this.deleting.set(true);
    this.profile.deleteMe().subscribe({
      next: () => {
        this.auth.logout();
        this.deleting.set(false);
        this.deleteOpen.set(false);
        this.router.navigateByUrl('/home');
      },
      error: (err: HttpErrorResponse) => {
        const mapped = this.i18n.getServerErrorMessage(err, 'profile.alerts.deletedError');
        this.showError(mapped ?? this.i18n.t('profile.alerts.deletedError'));
        this.deleting.set(false);
      },
    });
  }

  onAvatarFileSelected(e: Event): void {
    const input = e.target as HTMLInputElement;
    const f = input.files && input.files[0];
    this.avatarFile = f || null;
    this.avatarFileNameSig.set(f ? f.name : '');
    const c = this.form.get('avatar');
    if (c) {
      c.setErrors(null);
      c.markAsTouched();
    }
  }

  avatarFileName(): string {
    return this.avatarFileNameSig();
  }

  avatarSrc(): string {
    const u = this.me();
    return (u && u.avatarPath) || 'assets/avatar.svg';
  }

  onAvatarError(e: Event): void {
    (e.target as HTMLImageElement).src = 'assets/avatar.svg';
  }

  trackByAch(_i: number, a: UserAchievement): number {
    return a.id;
  }

  firstError(key: string): string | null {
    const c = this.form.get(key);
    if (!c) return null;
    const e = c.errors || {};
    if ((e as any).server && Array.isArray((e as any).server) && (e as any).server.length) {
      return String((e as any).server[0]);
    }
    if (e['required']) return this.i18n.t('validation.required');
    if (e['email']) return this.i18n.t('validation.email');
    if (e['minlength']) return this.i18n.t('validation.minLength', {min: e['minlength'].requiredLength});
    if (e['maxlength']) return this.i18n.t('validation.maxLength', {max: e['maxlength'].requiredLength});
    if (e['pattern']) return key === 'login' ? this.i18n.t('auth.register.validation.loginPattern') : this.i18n.t('validation.pattern');
    return null;
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

  private preferredLangTag(): string {
    const cand: any =
      (this.i18n as any)?.lang?.() ??
      (this.i18n as any)?.currentLang ??
      (document as any)?.documentElement?.lang ??
      (navigator as any)?.language ??
      'en';
    const s = String(cand || '').trim();
    if (!s) return 'en';
    const first = s.split(',')[0];
    const base = first.split('-')[0];
    return base.toLowerCase() === 'pl' ? 'pl' : 'en';
  }

  private sanitizeFilename(name: string): string {
    const n = name.replace(/[\\/:*?"<>|]+/g, '_').trim();
    return n || 'download';
  }

  private decodeRfc2047(encoded: string): string {
    const m = /^=\?([^?]+)\?([QBqb])\?([^?]+)\?=$/.exec(encoded);
    if (!m) return encoded;
    const typ = m[2].toUpperCase();
    const data = m[3];
    if (typ === 'B') {
      try {
        return decodeURIComponent(escape(atob(data)));
      } catch {
        return encoded;
      }
    } else {
      const q = data.replace(/_/g, ' ').replace(/=([0-9A-Fa-f]{2})/g, '%$1');
      try {
        return decodeURIComponent(q);
      } catch {
        return encoded;
      }
    }
  }

  private pickFilenameFromContentDisposition(cd: string, login: string): string {
    const cdNorm = cd || '';
    const fnStar = /filename\*\s*=\s*([^;]+)/i.exec(cdNorm);
    if (fnStar && fnStar[1]) {
      let v = fnStar[1].trim().replace(/^UTF-8''/i, '').replace(/^"+|"+$/g, '');
      try {
        v = decodeURIComponent(v);
      } catch {
      }
      return this.sanitizeFilename(v.endsWith('.pdf') ? v : v + '.pdf');
    }
    const fnQ = /filename\s*=\s*"([^"]+)"/i.exec(cdNorm);
    if (fnQ && fnQ[1]) {
      const raw = fnQ[1];
      const maybe = this.decodeRfc2047(raw);
      return this.sanitizeFilename(maybe.endsWith('.pdf') ? maybe : maybe + '.pdf');
    }
    const fn = /filename\s*=\s*([^;]+)/i.exec(cdNorm);
    if (fn && fn[1]) {
      const raw = fn[1].trim();
      const maybe = this.decodeRfc2047(raw);
      return this.sanitizeFilename(maybe.endsWith('.pdf') ? maybe : maybe + '.pdf');
    }
    const d = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    return `export-${login}-${d}.pdf`;
  }
}
