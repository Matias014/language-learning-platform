import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {TPipe} from '@app/shared/i18n/t.pipe';

@Component({
  standalone: true,
  selector: 'app-admin-shell',
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, TPipe],
  templateUrl: './admin-shell.component.html',
  styleUrls: ['./admin-shell.component.scss'],
})
export class AdminShellComponent {
}
