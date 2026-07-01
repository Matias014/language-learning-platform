import {Component, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {AuthService} from '@core/auth/auth.service';
import {I18nService} from '@shared/i18n/i18n.service';
import {TPipe} from '@shared/i18n/t.pipe';
import {LangSwitcherComponent} from '@shared/components/lang-switcher/lang-switcher.component';

function requiredNonWhitespace(): ValidatorFn {
  return (control: AbstractControl) => {
    const v = String(control.value ?? '');
    return v.trim().length === 0 ? {required: true} : null;
  };
}

type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TPipe, LangSwitcherComponent],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  private fb = inject(NonNullableFormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private i18n = inject(I18nService);

  form = this.fb.group({
    login: this.fb.control('', {
      validators: [requiredNonWhitespace(), Validators.maxLength(255)],
    }),
    password: this.fb.control('', {
      validators: [requiredNonWhitespace(), Validators.minLength(8), Validators.maxLength(255)],
    }),
    remember: this.fb.control(false),
  });

  submitted = signal(false);
  error = signal<string | null>(null);
  loading = signal(false);

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  get loginCtrl() {
    return this.form.controls.login;
  }

  get passwordCtrl() {
    return this.form.controls.password;
  }

  submit(): void {
    this.submitted.set(true);
    this.error.set(null);
    if (this.form.invalid || this.loading()) {
      return;
    }

    const raw = this.form.getRawValue();
    const login = String(raw.login ?? '').trim();
    const password = String(raw.password ?? '');
    const remember = !!raw.remember;

    this.loading.set(true);
    this.auth.login({loginOrEmail: login, password}, {remember}).subscribe({
      next: () => {
        this.loading.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        this.handleError(e);
      },
    });
  }

  private handleError(e: HttpErrorResponse): void {
    if (e.status === 429) {
      const msg = this.i18n.t('auth.login.errors.throttle');
      this.error.set(msg);
      this.showError(msg);
      return;
    }

    const code = this.extractErrorCode(e);

    if (code === 'INVALID_CREDENTIALS' || e.status === 401) {
      const loginErrors = this.loginCtrl.errors ?? {};
      const passwordErrors = this.passwordCtrl.errors ?? {};
      this.loginCtrl.setErrors({...loginErrors, invalid: true});
      this.passwordCtrl.setErrors({...passwordErrors, invalid: true});

      const msg =
        this.i18n.getServerErrorMessage(e, 'auth.login.errors.invalid') ??
        this.i18n.t('auth.login.errors.invalid');
      this.error.set(msg);
      this.showError(msg);
      return;
    }

    this.i18n.applyFieldErrors(this.form, e, {loginOrEmail: 'login'});

    const msg =
      this.i18n.getServerErrorMessage(e, 'auth.login.errors.generic') ??
      this.i18n.t('auth.login.errors.generic');
    this.error.set(msg);
    this.showError(msg);
  }

  private extractErrorCode(e: HttpErrorResponse): string | null {
    const raw = e.error ?? e;
    const c = raw?.code;
    return c ? String(c).toUpperCase() : null;
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
