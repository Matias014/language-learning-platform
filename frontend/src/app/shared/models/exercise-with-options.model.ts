import {Exercise} from './exercise.model';
import {ExerciseOption} from './exercise-option.model';

export interface ExerciseWithOptions extends Exercise {
  options: ExerciseOption[];
}
