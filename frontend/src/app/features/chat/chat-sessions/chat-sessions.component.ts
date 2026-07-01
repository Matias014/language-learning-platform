import {Component, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterLink} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {ChatService} from '../chat.service';
import {TPipe} from '@shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';
import {Language, ChatSession} from '@shared/models';
import {LanguageService} from '@shared/services/language.service';

type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-chat-sessions',
  imports: [CommonModule, FormsModule, RouterLink, TPipe],
  templateUrl: './chat-sessions.component.html',
  styleUrls: ['./chat-sessions.component.scss'],
})
export class ChatSessionsComponent {
  private chat = inject(ChatService);
  private router = inject(Router);
  private i18n = inject(I18nService);
  private langs = inject(LanguageService);

  items = signal<ChatSession[]>([]);
  loading = signal(true);
  title = signal('');
  submitted = signal(false);
  titleError = signal<string | null>(null);

  languages = signal<Language[]>([]);
  selectedLang = signal<string>('');

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  confirmId = signal<number | null>(null);

  constructor() {
    this.loadLanguages();
    const navText =
      (this.router.getCurrentNavigation()?.extras.state as any)?.toastText ||
      (history.state as any)?.toastText;
    if (navText) this.showSuccess(navText);
    this.refresh();
  }

  private loadLanguages() {
    this.langs.list().subscribe({
      next: arr => this.languages.set(arr || []),
      error: () => this.languages.set([]),
    });
  }

  refresh() {
    this.loading.set(true);
    this.chat.getSessionsMy().subscribe({
      next: arr => {
        const list = (arr || []).sort((a, b) => +new Date(b.startedAt) - +new Date(a.startedAt));
        this.items.set(list);
        this.loading.set(false);
      },
      error: e => {
        this.showError(this.i18n.getServerErrorMessage(e, 'chat.errors.fetchSessions') || '');
        this.loading.set(false);
      },
    });
  }

  private applyInlineErrors(e: any): boolean {
    this.titleError.set(null);
    const fieldErrors = this.i18n.getFieldErrors(e);
    const titleMsgs = fieldErrors['title'];
    if (Array.isArray(titleMsgs) && titleMsgs.length) {
      this.titleError.set(String(titleMsgs[0]));
      return true;
    }
    return false;
  }

  create() {
    this.submitted.set(true);
    this.titleError.set(null);
    const t = this.title().trim();
    if (!t) return;
    const conv = this.selectedLang().trim() || null;
    this.chat.createSession({title: t, conversationLanguageCode: conv}).subscribe({
      next: s => {
        this.title.set('');
        this.selectedLang.set('');
        this.submitted.set(false);
        this.titleError.set(null);
        this.router.navigate(['/chat', 'sessions', s.id]);
      },
      error: e => {
        const inline = this.applyInlineErrors(e);
        if (inline) return;
        this.showError(this.i18n.getServerErrorMessage(e, 'chat.errors.createSession') || '');
      },
    });
  }

  removeRequest(id: number) {
    this.confirmId.set(id);
  }

  cancelRemove() {
    this.confirmId.set(null);
  }

  confirmRemove(id: number) {
    this.chat.deleteSession(id).subscribe({
      next: () => {
        this.showSuccess(this.i18n.t('chat.alerts.sessionDeleted'));
        this.items.set(this.items().filter(i => i.id !== id));
        this.confirmId.set(null);
      },
      error: e => {
        this.showError(this.i18n.getServerErrorMessage(e, 'chat.errors.deleteSession') || '');
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
