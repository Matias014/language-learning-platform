import {Component, Input} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {ExerciseAttempt} from '@shared/models';
import {TPipe} from '@shared/i18n/t.pipe';

@Component({
  standalone: true,
  selector: 'app-attempt-timeline',
  imports: [CommonModule, DatePipe, TPipe],
  template: `
    <div class="card mt-3">
      <div class="card-header py-2 d-flex align-items-center justify-content-between">
        <span class="fw-semibold small">{{ 'lessons.history' | t }}</span>
        <span class="small text-body-secondary" *ngIf="attempts?.length">
          {{ 'lessons.timelineTotal' | t:{count: attempts.length} }}
        </span>
      </div>
      <div class="card-body p-0">
        <ng-container *ngIf="!loading; else spinnerTpl">
          <ng-container *ngIf="!error; else errTpl">
            <ng-container *ngIf="attempts?.length; else emptyTpl">
              <ul class="timeline list-unstyled m-0">
                <li *ngFor="let a of attempts" class="timeline-item">
                  <div class="dot" [class.ok]="a.correct" [class.bad]="!a.correct"></div>
                  <div class="content">
                    <div class="d-flex justify-content-between">
                      <div class="fw-semibold small">
                        {{ 'lessons.attemptNumber' | t:{no: a.attemptNumber} }}
                        <span
                          class="badge ms-2"
                          [class.text-bg-success]="a.correct"
                          [class.text-bg-danger]="!a.correct"
                        >
                          {{ a.correct ? ('lessons.correctShort' | t) : ('lessons.incorrectShort' | t) }}
                        </span>
                      </div>
                      <div class="small text-body-secondary">
                        {{ a.submittedAt | date : 'yyyy-MM-dd HH:mm' }}
                      </div>
                    </div>
                    <div class="small text-body-secondary mt-1" *ngIf="a.score != null">
                      {{ 'lessons.scoreLabel' | t:{score: a.score} }}
                    </div>
                    <div class="small mt-1" *ngIf="a.feedback">
                      <span class="text-body-secondary">
                        {{ 'lessons.feedbackLabel' | t }}
                      </span>
                      {{ a.feedback }}
                    </div>
                    <div class="small mt-1" *ngIf="a.submittedAnswer">
                      <span class="text-body-secondary">
                        {{ 'lessons.answerLabel' | t }}
                      </span>
                      {{ a.submittedAnswer }}
                    </div>
                  </div>
                </li>
              </ul>
            </ng-container>
          </ng-container>
        </ng-container>
      </div>
    </div>
    <ng-template #spinnerTpl>
      <div class="p-3 text-center">
        <div class="spinner-border spinner-border-sm"></div>
      </div>
    </ng-template>
    <ng-template #errTpl>
      <div class="p-3 text-danger small">{{ error }}</div>
    </ng-template>
    <ng-template #emptyTpl>
      <div class="p-3 text-body-secondary small">
        {{ 'lessons.noAttempts' | t }}
      </div>
    </ng-template>
  `,
  styles: [
    `
      .timeline {
        padding: 0.75rem 1rem;
        position: relative;
      }

      .timeline-item {
        display: flex;
        gap: 0.75rem;
        position: relative;
      }

      .timeline-item + .timeline-item {
        margin-top: 0.75rem;
      }

      .dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        margin-top: 0.35rem;
        flex: 0 0 10px;
        background: #ccc;
      }

      .dot.ok {
        background: #28a745;
      }

      .dot.bad {
        background: #dc3545;
      }

      .content {
        flex: 1;
      }
    `,
  ],
})
export class AttemptTimelineComponent {
  @Input() loading = false;
  @Input() error: string | null = null;
  @Input() attempts: ExerciseAttempt[] = [];
}
