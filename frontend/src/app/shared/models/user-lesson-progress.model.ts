import {LessonStatus} from './enums.model';

export interface UserLessonProgress {
  id: number;
  userId: number;
  lessonId: number;
  status: LessonStatus;
  completedAt: string | null;
  lastActivityAt: string | null;
}

export interface CreateUserLessonProgressRequest {
  lessonId: number;
}

export interface UpdateUserLessonProgressRequest {
  status?: LessonStatus;
}
