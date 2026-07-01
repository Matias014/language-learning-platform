import {CourseStatus} from './enums.model';

export interface CourseEnrollment {
  id: number;
  userId: number;
  courseId: number;
  currentLessonId: number | null;
  status: CourseStatus;
  startedAt: string;
  completedAt: string | null;
  lastActivityAt: string | null;
}

export interface CreateCourseEnrollmentRequest {
  courseId: number;
}

export interface UpdateCourseEnrollmentRequest {
  currentLessonId?: number | null;
}
