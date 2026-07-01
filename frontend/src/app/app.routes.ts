import {Routes} from '@angular/router';
import {canActivateAuth, canActivateGuest} from '@core/auth/auth.guard';
import {canActivateLessonEnrolled} from '@core/auth/enrollment.guard';
import {adminCanMatch} from '@core/auth/admin.guard';

export const routes: Routes = [
  {
    path: 'admin',
    canMatch: [adminCanMatch],
    loadChildren: () =>
      import('@features/admin/admin.routes').then(
        (m) => m.ADMIN_ROUTES
      ),
  },
  {
    path: 'login',
    canActivate: [canActivateGuest],
    data: {hideNavbar: true},
    loadComponent: () =>
      import('@features/auth/login/login.component').then(
        (m) => m.LoginComponent
      ),
  },
  {
    path: 'register',
    canActivate: [canActivateGuest],
    data: {hideNavbar: true},
    loadComponent: () =>
      import('@features/auth/register/register.component').then(
        (m) => m.RegisterComponent
      ),
  },
  {
    path: 'home',
    loadComponent: () =>
      import('@features/home/home.component').then(
        (m) => m.HomeComponent
      ),
  },
  {
    path: 'dashboard',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('@features/dashboard/dashboard.component').then(
        (m) => m.DashboardComponent
      ),
  },
  {
    path: 'profile',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('@features/profile/profile.component').then(
        (m) => m.ProfileComponent
      ),
  },
  {
    path: 'courses',
    loadComponent: () =>
      import('@features/courses/courses.component').then(
        (m) => m.CoursesComponent
      ),
  },
  {
    path: 'courses/:id',
    loadComponent: () =>
      import('@features/courses/course-detail/course-detail.component').then(
        (m) => m.CourseDetailComponent
      ),
  },
  {
    path: 'lessons/:id',
    canActivate: [canActivateAuth, canActivateLessonEnrolled],
    loadComponent: () =>
      import('@features/lessons/lesson.component').then(
        (m) => m.LessonComponent
      ),
  },
  {
    path: 'srs',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('@features/srs/srs.component').then(
        (m) => m.SrsComponent
      ),
  },
  {
    path: 'stats',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('@features/stats/stats.component').then(
        (m) => m.StatsComponent
      ),
  },
  {
    path: 'chat/sessions',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('@features/chat/chat-sessions/chat-sessions.component').then(m => m.ChatSessionsComponent),
  },
  {
    path: 'chat/sessions/:id',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('@features/chat/chat-session/chat-session.component').then(m => m.ChatSessionComponent),
  },
  {
    path: '403',
    data: {hideNavbar: true},
    loadComponent: () =>
      import('@shared/components/forbidden/forbidden.component').then(
        (m) => m.ForbiddenComponent
      ),
  },
  {path: '', pathMatch: 'full', redirectTo: 'home'},
  {
    path: '404',
    data: {hideNavbar: true},
    loadComponent: () =>
      import('@shared/components/not-found/not-found.component').then(
        (m) => m.NotFoundComponent
      ),
  },
  {path: '**', redirectTo: '404'},
];
