import {ExerciseType, DifficultyLevel} from './enums.model';

export interface ChatSendRequest {
  message: string;
}

export interface GradeResponse {
  correct: boolean;
  feedback: string;
  hints: string[] | null;
  attemptId: number;
  awardedXp: number | null;
}

export interface HintRequest {
  exerciseId: number;
  userAnswer?: string;
  maxHints?: number;
}

export interface HintResponse {
  correct: boolean;
  feedback: string;
  hints: string[] | null;
}

export interface GenerateExercisesRequest {
  exerciseType: ExerciseType;
  difficultyLevel: DifficultyLevel;
  topic: string;
  count: number;
  xp?: number | null;
}
