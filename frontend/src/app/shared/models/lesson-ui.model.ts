import {ExerciseWithOptions} from './exercise-with-options.model';
import {GradeResponse, HintResponse} from './ai.model';
import {ExerciseAttempt} from './exercise-attempt.model';

export interface UiExercise extends ExerciseWithOptions {
  result?: GradeResponse | null;
  hintsOnly?: HintResponse | null;
  busy?: boolean;
  hintBusy?: boolean;
  validationError?: string | null;
  history: ExerciseAttempt[] | null;
  hintCount?: number;
}
