import {Routes} from '@angular/router';
import {AdminShellComponent} from './admin-shell.component';
import {AdminUsersComponent} from './users/users.component';
import {AdminCoursesComponent} from './courses/courses.component';
import {AdminLessonsComponent} from './lessons/lessons.component';
import {AdminExercisesComponent} from './exercises/exercises.component';
import {AdminLanguagesComponent} from './languages/languages.component';
import {AdminProficiencyLevelsComponent} from './proficiency-levels/proficiency-levels.component';
import {AdminLevelsComponent} from '@features/admin/levels/levels.component';
import {AdminAchievementsComponent} from './achievements/achievements.component';
import {AdminCourseEnrollmentsComponent} from '@features/admin/course-enrollments/course-enrollments.component';
import {AdminUserAchievementsComponent} from './user-achievements/user-achievements.component';
import {AdminAttemptsComponent} from '@features/admin/exercise-attempts/exercise-attempts.component';
import {AdminAwardsComponent} from '@features/admin/exercise-awards/exercise-awards.component';
import {AdminUserLessonProgressComponent} from '@features/admin/user-lesson-progress/user-lesson-progress.component';
import {AdminLlmLogsComponent} from './llm-logs/llm-logs.component';
import {AdminChatSessionsComponent} from './chat-sessions/chat-sessions.component';
import {AdminChatMessagesComponent} from './chat-messages/chat-messages.component';
import {AdminReportsComponent} from '@features/admin/reports/reports.component';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      {path: '', pathMatch: 'full', redirectTo: 'users'},
      {path: 'achievements', component: AdminAchievementsComponent},
      {path: 'chat-messages', component: AdminChatMessagesComponent},
      {path: 'chat-sessions', component: AdminChatSessionsComponent},
      {path: 'course-enrollments', component: AdminCourseEnrollmentsComponent},
      {path: 'courses', component: AdminCoursesComponent},
      {path: 'exercise-attempts', component: AdminAttemptsComponent},
      {path: 'exercise-awards', component: AdminAwardsComponent},
      {path: 'exercises', component: AdminExercisesComponent},
      {path: 'languages', component: AdminLanguagesComponent},
      {path: 'lessons', component: AdminLessonsComponent},
      {path: 'levels', component: AdminLevelsComponent},
      {path: 'llm-logs', component: AdminLlmLogsComponent},
      {path: 'proficiency-levels', component: AdminProficiencyLevelsComponent},
      {path: 'reports', component: AdminReportsComponent},
      {path: 'user-achievements', component: AdminUserAchievementsComponent},
      {path: 'user-lesson-progress', component: AdminUserLessonProgressComponent},
      {path: 'users', component: AdminUsersComponent}
    ]
  }
];
