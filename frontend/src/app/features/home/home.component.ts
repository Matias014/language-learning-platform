import {Component, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterLink} from '@angular/router';
import {AuthService} from '@core/auth/auth.service';
import {TPipe} from '@shared/i18n/t.pipe';

@Component({
  standalone: true,
  selector: 'app-home',
  imports: [CommonModule, RouterLink, TPipe],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent {
  private router = inject(Router);
  private auth = inject(AuthService);

  loggedIn = this.auth.isAuth;

  goPrimary() {
    this.loggedIn()
      ? this.router.navigateByUrl('/dashboard')
      : this.router.navigateByUrl('/register');
  }
}
