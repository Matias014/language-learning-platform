export interface Lesson {
  id: number;
  courseId: number;
  title: string;
  description: string | null;
  orderNumber: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLessonRequest {
  courseId: number;
  title: string;
  description?: string | null;
  orderNumber: number;
}

export interface UpdateLessonRequest {
  title?: string;
  description?: string | null;
  orderNumber?: number;
}
