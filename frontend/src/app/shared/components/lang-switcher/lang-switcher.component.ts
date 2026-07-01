import {Component, inject} from '@angular/core';
import {CommonModule} from '@angular/common';
import {I18nService} from '@shared/i18n/i18n.service';

@Component({
  selector: 'app-lang-switcher',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lang-switcher.component.html',
  styleUrls: ['./lang-switcher.component.scss'],
})
export class LangSwitcherComponent {
  private i18n = inject(I18nService);

  get current(): string {
    return this.i18n.currentLang;
  }

  set(lang: string): void {
    this.i18n.setLang(lang);
  }
}
