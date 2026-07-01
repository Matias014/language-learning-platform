import {inject} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivateFn, Router} from '@angular/router';
import {AuthService} from './auth.service';
import {HttpClient} from '@angular/common/http';
import {catchError, forkJoin, map, of, switchMap} from 'rxjs';
import {Lesson} from '@shared/models/lesson.model';
import {CourseEnrollment} from '@shared/models/course-enrollment.model';
import {environment} from 'environments/environment';

export const canActivateLessonEnrolled: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const http = inject(HttpClient);

  const returnUrl = router.routerState.snapshot.url || '/dashboard';

  const idParam = route.paramMap.get('lessonId') ?? route.paramMap.get('id');
  const lessonId = idParam ? Number(idParam) : NaN;
  if (!Number.isFinite(lessonId) || lessonId <= 0) {
    return router.createUrlTree(['/dashboard']);
  }

  const base = environment.apiUrl;

  const load$ = forkJoin({
    lesson: http.get<Lesson>(`${base}/lessons/${lessonId}`),
    enrollments: http.get<CourseEnrollment[]>(`${base}/users/me/enrollments`)
  }).pipe(
    map(({lesson, enrollments}) => {
      const enrolled = enrollments.some(e => e.courseId === lesson.courseId);
      return enrolled ? true : router.createUrlTree(['/courses', lesson.courseId]);
    }),
    catchError(() => of(router.createUrlTree(['/dashboard'])))
  );

  if (auth.isAuthenticated()) {
    return load$;
  }

  return auth.refreshAccessToken().pipe(
    switchMap(ok => ok ? load$ : of(router.createUrlTree(['/login'], {queryParams: {returnUrl}})))
  );
};
