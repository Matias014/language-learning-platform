export interface ExerciseOption {
  id: number;
  exerciseId: number;
  content: string;
  orderNumber: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExerciseOptionRequest {
  exerciseId: number;
  content: string;
  orderNumber: number;
}

export interface UpdateExerciseOptionRequest {
  content?: string;
  orderNumber?: number;
}
