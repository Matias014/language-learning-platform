import {User} from './user.model';
import {CourseEnrollment} from './course-enrollment.model';
import {XpPoint} from './xp.model';
import {Level} from './user-level.model';
import {Course} from './course.model';
import {SrsSummary} from './user-srs.model';

export interface DashboardVm {
  me: User;
  enrollments: Array<CourseEnrollment & { courseTitle: string; progressPercent?: number }>;
  nextLessonId: number | null;
  resumeLessonId?: number | null;
  resumeExerciseId?: number | null;
  recentAchievements: Array<{ title: string; iconPath: string | null }>;
  xp7d: XpPoint[];
  totalXp: number;
  awardsCount?: number;
  levels?: Level[];
  recommendation?: { course: Course; score: number };
  srsSummary?: SrsSummary;
  currentLevel: number;
  percentToNext: number;
}
