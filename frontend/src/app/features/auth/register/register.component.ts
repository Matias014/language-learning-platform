import {Component, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
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
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TPipe, LangSwitcherComponent],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent {
  private fb = inject(NonNullableFormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private i18n = inject(I18nService);

  loading = signal(false);
  error = signal<string | null>(null);
  submitted = signal(false);

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  form = this.fb.group(
    {
      login: this.fb.control('', {
        validators: [
          requiredNonWhitespace(),
          Validators.minLength(3),
          Validators.maxLength(64),
          Validators.pattern(/^[A-Za-z0-9._-]+$/),
        ],
      }),
      email: this.fb.control('', {
        validators: [requiredNonWhitespace(), Validators.email, Validators.maxLength(255)],
      }),
      password: this.fb.control('', {
        validators: [requiredNonWhitespace(), Validators.minLength(8), Validators.maxLength(255)],
      }),
      confirm: this.fb.control('', {
        validators: [requiredNonWhitespace()],
      }),
      name: this.fb.control('', {
        validators: [requiredNonWhitespace(), Validators.maxLength(100)],
      }),
      surname: this.fb.control('', {
        validators: [requiredNonWhitespace(), Validators.maxLength(100)],
      }),
    },
    {
      validators: group => {
        const password = group.get('password')?.value ?? '';
        const confirm = group.get('confirm')?.value ?? '';
        return password && confirm && password !== confirm ? {mismatch: true} : null;
      },
    }
  );

  get loginCtrl() {
    return this.form.controls.login;
  }

  get emailCtrl() {
    return this.form.controls.email;
  }

  get passwordCtrl() {
    return this.form.controls.password;
  }

  get confirmCtrl() {
    return this.form.controls.confirm;
  }

  get nameCtrl() {
    return this.form.controls.name;
  }

  get surnameCtrl() {
    return this.form.controls.surname;
  }

  submit(): void {
    this.submitted.set(true);
    if (this.form.invalid || this.loading()) {
      return;
    }

    this.error.set(null);
    this.loading.set(true);

    const raw = this.form.getRawValue();
    const login = String(raw.login ?? '').trim();
    const email = String(raw.email ?? '').trim();
    const password = String(raw.password ?? '');
    const name = String(raw.name ?? '').trim();
    const surname = String(raw.surname ?? '').trim();

    this.auth.register({login, email, password, name, surname}, {remember: false}).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl('/dashboard');
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);

        const appliedFieldErrors = this.i18n.applyFieldErrors(this.form, err);

        if (err.status === 409) {
          const msg =
            this.i18n.getServerErrorMessage(err, 'auth.register.errors.conflict') ??
            this.i18n.t('auth.register.errors.conflict');
          this.error.set(msg);
          this.showError(msg);
          return;
        }

        if (!appliedFieldErrors) {
          const msg =
            this.i18n.getServerErrorMessage(err, 'auth.register.errors.generic') ??
            this.i18n.t('auth.register.errors.generic');
          this.error.set(msg);
          this.showError(msg);
        }
      },
    });
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
