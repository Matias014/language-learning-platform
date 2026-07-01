import {DifficultyLevel, ExerciseType} from './enums.model';

export interface EffectivenessStats {
  globalAccuracy: number;
  accuracyByType: Partial<Record<ExerciseType, number>>;
  accuracyByDifficulty: Partial<Record<DifficultyLevel, number>>;
  averageOpenScore: number | null;
}
