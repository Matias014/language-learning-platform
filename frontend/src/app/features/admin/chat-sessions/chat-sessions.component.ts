import {Component, inject, signal, computed} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {I18nService} from '@shared/i18n/i18n.service';
import {AdminChatSessionsService} from './admin-chat-sessions.service';
import {ChatSession} from '@shared/models';
import {finalize} from 'rxjs/operators';

type SortDir = 'asc' | 'desc';
type SortProp = 'id' | 'userId' | 'title' | 'conversationLanguageCode' | 'startedAt';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-chat-sessions',
  imports: [CommonModule, FormsModule, RouterLink, TPipe],
  templateUrl: './chat-sessions.component.html',
  styleUrls: ['./chat-sessions.component.scss'],
})
export class AdminChatSessionsComponent {
  private api = inject(AdminChatSessionsService);
  private i18n = inject(I18nService);

  loading = signal(false);
  error = signal<string | null>(null);
  baseRows = signal<ChatSession[]>([]);

  filterUserId = signal<number | null>(null);
  q = signal<string>('');

  page = signal(0);
  size = signal(10);
  sortProp = signal<SortProp>('startedAt');
  sortDir = signal<SortDir>('desc');

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  ngOnInit() {
    this.loadAll();
  }

  private compareValues(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'startedAt') {
      const an = typeof a === 'string' ? Date.parse(a) : 0;
      const bn = typeof b === 'string' ? Date.parse(b) : 0;
      return an - bn;
    }
    if (typeof a === 'number' && typeof b === 'number') return a - b;
    const as = String(a).toLowerCase();
    const bs = String(b).toLowerCase();
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

  private filtered = computed(() => {
    const uid = this.filterUserId();
    const query = this.q().trim().toLowerCase();
    return this.baseRows().filter((s) => {
      if (uid != null && s.userId !== uid) return false;
      if (query && !((s.title ?? '').toLowerCase().includes(query))) return false;
      return true;
    });
  });

  private sorted = computed(() => {
    const prop = this.sortProp();
    const dir = this.sortDir();
    const mul = dir === 'asc' ? 1 : -1;
    return [...this.filtered()].sort((x, y) => mul * this.compareValues((x as any)[prop], (y as any)[prop], prop));
  });

  totalElements = computed(() => this.sorted().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
  });

  pageRows = computed(() => {
    const start = this.page() * this.size();
    const end = start + this.size();
    return this.sorted().slice(start, end);
  });

  loadAll() {
    this.loading.set(true);
    this.error.set(null);
    this.api.listAll().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (data) => {
        this.baseRows.set(data ?? []);
        this.page.set(0);
      },
      error: (e) => {
        const msg = this.i18n.getServerErrorMessage(e, 'admin.chatSessions.errors.load') || '';
        this.error.set('admin.chatSessions.errors.load');
        if (msg) this.showError(msg);
        this.baseRows.set([]);
      },
    });
  }

  reload() {
    const uid = this.filterUserId();
    if (uid == null) {
      this.loadAll();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.api.listByUser(uid).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (data) => {
        this.baseRows.set(data ?? []);
        this.page.set(0);
      },
      error: (e) => {
        const msg = this.i18n.getServerErrorMessage(e, 'admin.chatSessions.errors.load') || '';
        this.error.set('admin.chatSessions.errors.load');
        if (msg) this.showError(msg);
        this.baseRows.set([]);
      },
    });
  }

  clearFilters() {
    this.filterUserId.set(null);
    this.q.set('');
    this.page.set(0);
    this.size.set(10);
    this.sortProp.set('startedAt');
    this.sortDir.set('desc');
    this.loadAll();
  }

  remove(id: number) {
    const msg = this.i18n.t('admin.chatSessions.confirmDelete') || '';
    if (!confirm(msg)) return;
    this.api.delete(id).subscribe({
      next: () => {
        this.showSuccess(this.i18n.t('chat.alerts.sessionDeleted') || '');
        this.reload();
      },
      error: (e) => {
        const msg2 = this.i18n.getServerErrorMessage(e, 'admin.chatSessions.errors.delete') || '';
        if (msg2) this.showError(msg2);
        this.reload();
      },
    });
  }

  toggleSort(prop: SortProp) {
    if (this.sortProp() === prop) {
      this.sortDir.set(this.sortDir() === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortProp.set(prop);
      this.sortDir.set('asc');
    }
    this.page.set(0);
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
    if (this.canPrev()) this.page.set(this.page() - 1);
  }

  next() {
    if (this.canNext()) this.page.set(this.page() + 1);
  }

  changeSize(size: number) {
    const s = Number(size);
    this.size.set([5, 10, 20, 50].includes(s) ? s : 10);
    this.page.set(0);
  }

  trackById = (_: number, s: ChatSession) => s.id;

  private showToast(text: string, type: ToastType) {
    const id = ++this.toastSeq;
    this.toasts.set([{id, text, type}, ...this.toasts()]);
    setTimeout(() => this.dismissToast(id), 4000);
  }

  showError(text: string) {
    if (text) this.showToast(text, 'error');
  }

  showSuccess(text: string) {
    if (text) this.showToast(text, 'success');
  }

  dismissToast(id: number) {
    this.toasts.set(this.toasts().filter(t => t.id !== id));
  }

  trackToast(_i: number, t: { id: number }) {
    return t.id;
  }
}
