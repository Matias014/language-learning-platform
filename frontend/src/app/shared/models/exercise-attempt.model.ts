export interface ExerciseAttempt {
  id: number;
  userId: number;
  exerciseId: number;
  submittedAnswer: string | null;
  chosenOptionId: number | null;
  score: number | null;
  feedback: string | null;
  correct: boolean;
  attemptNumber: number;
  submittedAt: string;
  durationSeconds: number | null;
}

export interface CreateExerciseAttemptRequest {
  exerciseId: number;
  submittedAnswer?: string | null;
  chosenOptionId?: number | null;
  durationSeconds?: number | null;
}

export interface UpdateExerciseAttemptRequest {
  correct?: boolean | null;
  score?: number | null;
  feedback?: string | null;
}
