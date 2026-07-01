import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {TPipe} from '@shared/i18n/t.pipe';
import {UiExercise} from '@shared/models';

@Component({
  standalone: true,
  selector: 'app-writing-exercise',
  imports: [CommonModule, FormsModule, TPipe],
  template: `
    <div class="card">
      <div class="card-body">
        <div class="mb-2 d-flex align-items-center justify-content-between">
          <span class="badge text-bg-secondary">{{ ('difficulty.' + exercise.difficulty) | t }}</span>
          <small class="text-muted">{{ 'lessons.xp' | t:{xp: exercise.xp} }}</small>
        </div>
        <div class="d-flex align-items-center justify-content-between">
          <h5 class="mb-3">{{ exercise.question }}</h5>
          <span *ngIf="exercise.result?.correct" class="badge text-bg-success">
            {{ 'lessons.correct' | t }}
          </span>
        </div>
        <textarea
          class="form-control"
          rows="4"
          [(ngModel)]="value"
          (ngModelChange)="onChange()"
          [readonly]="exercise.busy"
          placeholder="{{ 'lessons.submit' | t }}…"
        ></textarea>
        <div class="d-flex gap-2 mt-3 align-items-center">
          <button class="btn btn-primary" (click)="submit()" [disabled]="!value || exercise.busy">
            {{ 'lessons.submit' | t }}
            <span *ngIf="exercise.busy" class="spinner-border spinner-border-sm ms-1"></span>
          </button>
          <button class="btn btn-outline-secondary" (click)="clear()" [disabled]="exercise.busy">
            {{ 'common.clear' | t }}
          </button>
          <button
            class="btn btn-outline-info ms-auto"
            type="button"
            (click)="requestHint()"
            [disabled]="exercise.hintBusy"
          >
            {{ 'lessons.hint' | t }}
            <span *ngIf="exercise.hintBusy" class="spinner-border spinner-border-sm ms-1"></span>
          </button>
        </div>
        <div *ngIf="exercise.validationError" class="text-danger small mt-2">
          {{ exercise.validationError | t }}
        </div>
      </div>
    </div>
  `,
})
export class WritingExerciseComponent implements OnChanges {
  @Input({required: true}) exercise!: UiExercise;
  @Output() answer = new EventEmitter<string>();
  @Output() askHint = new EventEmitter<string | null>();
  @Output() changed = new EventEmitter<void>();
  value = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['exercise']) {
      const prevId = changes['exercise'].previousValue?.id;
      const curId = changes['exercise'].currentValue?.id;
      if (prevId !== curId) {
        this.value = '';
      }
    }
  }

  onChange() {
    this.changed.emit();
  }

  clear() {
    this.value = '';
    this.changed.emit();
  }

  submit() {
    const v = this.value.trim();
    if (!v) return;
    this.answer.emit(v);
  }

  requestHint() {
    const v = this.value.trim();
    this.askHint.emit(v || null);
  }
}
