import {Component, Input, OnChanges, SimpleChanges, computed, signal} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {ExerciseAttempt} from '@shared/models';
import {TPipe} from '@shared/i18n/t.pipe';

@Component({
  standalone: true,
  selector: 'app-attempt-history',
  imports: [CommonModule, DatePipe, TPipe],
  template: `
    <div class="card">
      <div class="card-body">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <div class="fw-semibold">{{ 'lessons.history' | t }}</div>
          <div class="d-flex align-items-center" *ngIf="totalElements() > 0">
            <span class="text-body-secondary small">
              {{ 'common.pagination' | t }}:
              {{ page() + 1 }} / {{ totalPages() || 1 }} • {{ totalElements() }} {{ 'common.total' | t }}
            </span>
            <div class="btn-group ms-2">
              <button class="btn btn-sm btn-outline-secondary" [disabled]="!canPrev()" (click)="prev()">&laquo;</button>
              <button class="btn btn-sm btn-outline-secondary" [disabled]="!canNext()" (click)="next()">&raquo;</button>
            </div>
          </div>
        </div>

        <div *ngIf="rows?.length; else empty">
          <div class="list-group list-group-flush small">
            <div class="list-group-item" *ngFor="let r of pageRows(); trackBy: trackById">
              <div class="d-flex justify-content-between align-items-center">
                <span>{{ r.submittedAt | date:'yyyy-MM-dd HH:mm' }}</span>
                <span class="badge" [ngClass]="r.correct ? 'text-bg-success' : 'text-bg-warning'">
                  {{ r.correct ? ('lessons.correct' | t) : ('lessons.tryAgain' | t) }}
                </span>
              </div>
              <div *ngIf="r.score != null" class="text-body-secondary">
                {{ 'lessons.scoreLabel' | t:{score: r.score} }}
              </div>
              <div *ngIf="r.feedback" class="mt-1">{{ r.feedback }}</div>
            </div>
          </div>
        </div>

        <ng-template #empty>
          <div class="text-body-secondary small">—</div>
        </ng-template>
      </div>
    </div>
  `,
})
export class AttemptHistoryComponent implements OnChanges {
  @Input() rows: ExerciseAttempt[] | null = null;

  page = signal(0);
  size = signal(5);

  ngOnChanges(changes: SimpleChanges) {
    if (changes['rows']) this.page.set(0);
  }

  totalElements() {
    return (this.rows?.length ?? 0);
  }

  totalPages() {
    const n = Math.ceil(this.totalElements() / this.size());
    return n || 0;
  }

  canPrev() {
    return this.page() > 0;
  }

  canNext() {
    return this.page() + 1 < this.totalPages();
  }

  prev() {
    if (!this.canPrev()) return;
    this.page.set(this.page() - 1);
  }

  next() {
    if (!this.canNext()) return;
    this.page.set(this.page() + 1);
  }

  pageRows(): ExerciseAttempt[] {
    const all = this.rows ?? [];
    const start = this.page() * this.size();
    return all.slice(start, start + this.size());
  }

  trackById = (_: number, r: ExerciseAttempt) => r.id;
}
