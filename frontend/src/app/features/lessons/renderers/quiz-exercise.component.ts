import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TPipe} from '@shared/i18n/t.pipe';
import {ExerciseOption, UiExercise} from '@shared/models';

@Component({
  standalone: true,
  selector: 'app-quiz-exercise',
  imports: [CommonModule, TPipe],
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
        <div class="list-group">
          <button
            *ngFor="let opt of optionsSorted; trackBy: trackById"
            type="button"
            class="list-group-item list-group-item-action"
            [class.active]="selectedOptionId === opt.id"
            (click)="select(opt.id)"
            [disabled]="exercise.busy"
          >
            <strong class="me-2">{{ opt.orderNumber }}.</strong>
            {{ opt.content }}
          </button>
        </div>
        <div class="d-flex gap-2 mt-3 align-items-center">
          <button class="btn btn-primary" (click)="submit()" [disabled]="!selectedOptionId || exercise.busy">
            {{ 'lessons.submit' | t }}
            <span *ngIf="exercise.busy" class="spinner-border spinner-border-sm ms-1"></span>
          </button>
          <button class="btn btn-outline-secondary" (click)="clear()" [disabled]="exercise.busy">
            {{ 'common.clear' | t }}
          </button>
          <button class="btn btn-outline-info ms-auto" type="button" (click)="requestHint()"
                  [disabled]="exercise.hintBusy">
            {{ 'lessons.hint' | t }}
            <span *ngIf="exercise.hintBusy" class="spinner-border spinner-border-sm ms-1"></span>
          </button>
        </div>
        <div *ngIf="exercise.validationError" class="text-danger small mt-2">{{ exercise.validationError | t }}</div>
      </div>
    </div>
  `,
})
export class QuizExerciseComponent implements OnChanges {
  @Input({required: true}) exercise!: UiExercise;
  @Output() answer = new EventEmitter<number>();
  @Output() askHint = new EventEmitter<string | null>();
  @Output() changed = new EventEmitter<void>();
  selectedOptionId: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['exercise']) {
      const prevId = changes['exercise'].previousValue?.id;
      const curId = changes['exercise'].currentValue?.id;
      if (prevId !== curId) {
        this.selectedOptionId = null;
      }
    }
  }

  get optionsSorted(): ExerciseOption[] {
    return (this.exercise.options ?? []).slice().sort((a, b) => a.orderNumber - b.orderNumber);
  }

  trackById = (_: number, o: ExerciseOption) => o.id;

  select(id: number) {
    this.selectedOptionId = id;
    this.changed.emit();
  }

  clear() {
    this.selectedOptionId = null;
    this.changed.emit();
  }

  submit() {
    if (!this.selectedOptionId) return;
    this.answer.emit(this.selectedOptionId);
  }

  requestHint() {
    this.askHint.emit(null);
  }
}
