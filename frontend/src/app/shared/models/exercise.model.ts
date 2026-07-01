import {DifficultyLevel, ExerciseType} from './enums.model';

export interface Exercise {
  id: number;
  lessonId: number;
  type: ExerciseType;
  question: string;
  answerSchema: Record<string, unknown> | null;
  sampleAnswer: string | null;
  difficulty: DifficultyLevel;
  xp: number;
  orderNumber: number;
  correctOptionId: number | null;
  passingScore: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExerciseRequest {
  lessonId: number;
  type: ExerciseType;
  question: string;
  answerSchema?: Record<string, unknown> | null;
  sampleAnswer?: string | null;
  difficulty: DifficultyLevel;
  xp: number;
  orderNumber: number;
  correctOptionId?: number | null;
  passingScore?: number | null;
}

export interface UpdateExerciseRequest {
  lessonId?: number;
  type?: ExerciseType;
  question?: string;
  answerSchema?: Record<string, unknown> | null;
  sampleAnswer?: string | null;
  difficulty?: DifficultyLevel;
  xp?: number;
  orderNumber?: number;
  correctOptionId?: number | null;
  passingScore?: number | null;
}
