import {Component, computed, inject, signal} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {TPipe} from '@app/shared/i18n/t.pipe';
import {I18nService} from '@app/shared/i18n/i18n.service';
import {AdminChatMessagesService} from './admin-chat-messages.service';
import {ChatMessage} from '@shared/models';

type SortDir = 'asc' | 'desc';
type SortProp = 'id' | 'sessionId' | 'sender' | 'message' | 'sentAt';
type ToastType = 'success' | 'error';

@Component({
  standalone: true,
  selector: 'app-admin-chat-messages',
  imports: [CommonModule, FormsModule, TPipe],
  templateUrl: './chat-messages.component.html',
  styleUrls: ['./chat-messages.component.scss'],
})
export class AdminChatMessagesComponent {
  private api = inject(AdminChatMessagesService);
  private route = inject(ActivatedRoute);
  private i18n = inject(I18nService);

  sessionId = signal<string>('');
  q = signal<string>('');

  loading = signal(false);
  error = signal<string | null>(null);
  rows = signal<ChatMessage[]>([]);

  page = signal(0);
  size = signal(10);
  sortProp = signal<SortProp>('sentAt');
  sortDir = signal<SortDir>('desc');

  toasts = signal<{ id: number; text: string; type: ToastType }[]>([]);
  private toastSeq = 0;

  ngOnInit() {
    const raw = this.route.snapshot.queryParamMap.get('sessionId');
    this.sessionId.set(raw ?? '');
    this.reload();
  }

  onSessionIdChange(v: any) {
    this.sessionId.set((v ?? '').toString());
    this.page.set(0);
  }

  onQueryChange(v: any) {
    this.q.set((v ?? '').toString());
    this.page.set(0);
  }

  reload() {
    this.loading.set(true);
    this.error.set(null);
    const sidRaw = (this.sessionId() ?? '').trim();
    const sidNum = sidRaw ? Number(sidRaw) : NaN;
    const req$ = sidRaw && Number.isFinite(sidNum) && sidNum >= 1
      ? this.api.listBySession(sidNum)
      : this.api.listAll();

    req$.subscribe({
      next: data => {
        this.rows.set(data ?? []);
        this.page.set(0);
        this.loading.set(false);
      },
      error: err => {
        this.rows.set([]);
        this.page.set(0);
        this.loading.set(false);
        const msg = this.i18n.getServerErrorMessage(err, 'admin.chatMessages.errors.load') || '';
        this.error.set('admin.chatMessages.errors.load');
        if (msg) this.showError(msg);
      }
    });
  }

  clearFilters() {
    this.sessionId.set('');
    this.q.set('');
    this.page.set(0);
    this.size.set(10);
    this.sortProp.set('sentAt');
    this.sortDir.set('desc');
    this.rows.set([]);
    this.error.set(null);
  }

  private compareValues(a: unknown, b: unknown, prop: SortProp) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    if (prop === 'sentAt') {
      const an = typeof a === 'string' ? Date.parse(a) : 0;
      const bn = typeof b === 'string' ? Date.parse(b) : 0;
      return an - bn;
    }
    if (typeof a === 'number' && typeof b === 'number') return a - b;
    const as = String(a);
    const bs = String(b);
    return as < bs ? -1 : as > bs ? 1 : 0;
  }

  private filteredRows = computed(() => {
    const query = (this.q() ?? '').trim().toLowerCase();
    if (!query) return this.rows();
    return this.rows().filter(m =>
      (m.message || '').toLowerCase().includes(query)
      || String(m.id).includes(query)
      || String(m.sessionId).includes(query)
      || String(m.sender).toLowerCase().includes(query)
    );
  });

  private sortedRows = computed(() => {
    const prop = this.sortProp();
    const dir = this.sortDir();
    const mul = dir === 'asc' ? 1 : -1;
    return [...this.filteredRows()].sort((x, y) => mul * this.compareValues((x as any)[prop], (y as any)[prop], prop));
  });

  totalElements = computed(() => this.sortedRows().length);
  totalPages = computed(() => {
    const te = this.totalElements();
    return te ? Math.ceil(te / this.size()) : 0;
  });

  pageRows = computed(() => {
    const start = this.page() * this.size();
    const end = start + this.size();
    return this.sortedRows().slice(start, end);
  });

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

  remove(id: number) {
    const msg = this.i18n.t('admin.chatMessages.confirmDelete') || '';
    if (!confirm(msg)) return;
    this.api.delete(id).subscribe({
      next: () => {
        this.showSuccess(this.i18n.t('common.delete'));
        this.reload();
      },
      error: err => {
        const msg2 = this.i18n.getServerErrorMessage(err, 'admin.chatMessages.errors.delete') || '';
        if (msg2) this.showError(msg2);
      }
    });
  }

  trackById = (_: number, m: ChatMessage) => m.id;

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
