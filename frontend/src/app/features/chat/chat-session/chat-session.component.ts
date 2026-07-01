import {Component, ElementRef, ViewChild, inject, signal, computed} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {FormsModule} from '@angular/forms';
import {ChatService, MAX_USER_MESSAGE_CHARS} from '../chat.service';
import {ChatMessage, ChatSession, ChatSendRequest, Language} from '@shared/models';
import {TPipe} from '@shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';
import {LanguageService} from '@shared/services/language.service';
import {Subject, of} from 'rxjs';
import {catchError, debounceTime, distinctUntilChanged, finalize, switchMap, tap} from 'rxjs/operators';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';

type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-chat-session',
  imports: [CommonModule, FormsModule, RouterLink, TPipe],
  templateUrl: './chat-session.component.html',
  styleUrls: ['./chat-session.component.scss'],
})
export class ChatSessionComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ChatService);
  private i18n = inject(I18nService);
  private langs = inject(LanguageService);

  @ViewChild('scrollContainer') scrollRef!: ElementRef<HTMLDivElement>;
  @ViewChild('msgInput') inputRef!: ElementRef<HTMLTextAreaElement>;

  loading = signal(true);
  sending = signal(false);

  session = signal<ChatSession | null>(null);
  messages = signal<ChatMessage[]>([]);
  text = signal('');

  editTitle = signal(false);
  titleEdit = signal<string>('');

  confirmDelete = signal(false);

  readonly maxLen = MAX_USER_MESSAGE_CHARS;
  readonly charsLeft = computed(() => this.maxLen - this.text().length);
  readonly tooLong = computed(() => this.text().length > this.maxLen);

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  languages = signal<Language[]>([]);
  selectedLang = signal<string>('');
  langBusy = signal(false);

  errorStatus = signal<number | null>(null);

  private langChanged$ = new Subject<string>();

  constructor() {
    this.langChanged$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => this.langBusy.set(true)),
        switchMap(code => {
          const s = this.session();
          if (!s) return of(null);
          const next = (code || '').trim();
          const cur = s.conversationLanguageCode || '';
          if (next === cur) return of(s);
          return this.api.updateSession(s.id, {conversationLanguageCode: next || null}).pipe(
            catchError(e => {
              this.selectedLang.set(cur);
              const msg = this.i18n.getServerErrorMessage(e, 'chat.errors.updateSession') || '';
              this.showError(msg);
              return of(null);
            }),
            finalize(() => this.langBusy.set(false))
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe(updated => {
        if (updated && updated.id) {
          this.session.set(updated);
          this.selectedLang.set(updated.conversationLanguageCode || '');
          this.showSuccess(this.i18n.t('chat.alerts.languageSaved'));
        }
      });
  }

  ngOnInit() {
    this.loadLanguages();

    const rawId = this.route.snapshot.paramMap.get('id');
    const id = rawId ? Number(rawId) : NaN;
    if (!Number.isFinite(id) || id <= 0) {
      this.loading.set(false);
      return;
    }
    this.api.getSession(id).subscribe({
      next: s => {
        this.session.set(s);
        this.titleEdit.set(s.title || '');
        this.selectedLang.set(s.conversationLanguageCode || '');
        this.loadMessages();
      },
      error: e => {
        if (e?.status === 404) {
          this.errorStatus.set(404);
        } else if (e?.status === 403) {
          this.errorStatus.set(403);
        } else {
          this.showError(this.i18n.getServerErrorMessage(e, 'chat.errors.fetchSession') || '');
        }
        this.loading.set(false);
      },
    });
  }

  private loadLanguages() {
    this.langs.list().subscribe({
      next: arr => this.languages.set(arr || []),
      error: () => this.languages.set([]),
    });
  }

  private scrollToBottom() {
    requestAnimationFrame(() => {
      const el = this.scrollRef?.nativeElement;
      if (!el) return;
      el.scrollTop = el.scrollHeight;
    });
  }

  private focusInput() {
    requestAnimationFrame(() => {
      if (this.inputRef?.nativeElement) this.inputRef.nativeElement.focus();
    });
  }

  private firstFieldError(e: any): string | null {
    const fields = this.i18n.getFieldErrors(e);
    const first = Object.keys(fields)[0];
    const msgs = first ? fields[first] : null;
    return Array.isArray(msgs) && msgs.length ? String(msgs[0]) : null;
  }

  onLangSelect(code: string) {
    this.selectedLang.set(code);
    this.langChanged$.next(code);
  }

  loadMessages() {
    const s = this.session();
    if (!s) {
      this.loading.set(false);
      return;
    }
    this.api.getMessages(s.id).subscribe({
      next: arr => {
        this.messages.set(arr);
        this.loading.set(false);
        this.scrollToBottom();
      },
      error: e => {
        const fieldMsg = this.firstFieldError(e);
        if (fieldMsg) {
          this.showError(fieldMsg);
        } else {
          this.showError(this.i18n.getServerErrorMessage(e, 'chat.errors.fetchMessages') || '');
        }
        this.loading.set(false);
      },
    });
  }

  deleteSession() {
    const s = this.session();
    if (!s) return;
    this.api.deleteSession(s.id).subscribe({
      next: () => {
        const toastText = this.i18n.t('chat.alerts.sessionDeleted');
        this.router.navigate(['/chat', 'sessions'], {state: {toastText}});
      },
      error: e => {
        const fieldMsg = this.firstFieldError(e);
        if (fieldMsg) {
          this.showError(fieldMsg);
        } else {
          this.showError(this.i18n.getServerErrorMessage(e, 'chat.errors.deleteSession') || '');
        }
      },
    });
  }

  send() {
    const s = this.session();
    const t = this.text().trim();
    if (!s || !t) return;

    if (t.length > this.maxLen) {
      this.showError(this.i18n.t('chat.errors.tooLong', {max: this.maxLen}));
      return;
    }

    this.sending.set(true);

    const optimistic: ChatMessage = {
      id: 0,
      sessionId: s.id,
      sender: 'user',
      message: t,
      sentAt: new Date().toISOString()
    };

    this.messages.set(this.messages().concat([optimistic]));
    this.scrollToBottom();
    this.text.set('');

    const body: ChatSendRequest = {message: t};
    this.api.send(s.id, body).subscribe({
      next: aiMsg => {
        this.messages.set(this.messages().concat([aiMsg]));
        this.sending.set(false);
        this.scrollToBottom();
        this.focusInput();
      },
      error: e => {
        this.messages.set(this.messages().filter(x => x !== optimistic));
        this.sending.set(false);
        const fieldMsg = this.firstFieldError(e);
        if (fieldMsg) {
          this.showError(fieldMsg);
        } else {
          this.showError(this.i18n.getServerErrorMessage(e, 'chat.errors.send') || '');
        }
        this.focusInput();
      },
    });
  }

  startEditTitle() {
    this.editTitle.set(true);
  }

  cancelEditTitle() {
    this.editTitle.set(false);
    this.titleEdit.set(this.session()?.title || '');
  }

  saveTitle() {
    const s = this.session();
    if (!s) return;
    const title = this.titleEdit().trim();
    this.api.updateSession(s.id, {title}).subscribe({
      next: updated => {
        this.session.set(updated);
        this.editTitle.set(false);
        this.showSuccess(this.i18n.t('chat.alerts.titleSaved'));
      },
      error: e => {
        const fieldMsg = this.firstFieldError(e);
        if (fieldMsg) {
          this.showError(fieldMsg);
        } else {
          this.showError(this.i18n.getServerErrorMessage(e, 'chat.errors.updateSession') || '');
        }
      },
    });
  }

  private showToast(text: string, type: ToastType) {
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
