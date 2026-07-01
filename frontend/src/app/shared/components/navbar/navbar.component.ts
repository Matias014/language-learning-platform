import {Component, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {LangSwitcherComponent} from '@shared/components/lang-switcher/lang-switcher.component';
import {AuthService} from '@core/auth/auth.service';
import {TPipe} from '@shared/i18n/t.pipe';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, LangSwitcherComponent, TPipe],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
})
export class NavbarComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  isAuth = this.auth.isAuth;
  role = this.auth.role;
  username = this.auth.username;
  avatarPath = this.auth.avatarPath;

  onAvatarError(e: Event) {
    (e.target as HTMLImageElement).src = 'assets/avatar.svg';
  }

  logout() {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
