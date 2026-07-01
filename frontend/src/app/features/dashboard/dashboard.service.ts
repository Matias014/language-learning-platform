import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {catchError, forkJoin, map, of, switchMap} from 'rxjs';
import {environment} from 'environments/environment';
import {
  Achievement,
  Course,
  CourseEnrollment,
  CourseRecommendation,
  DashboardVm,
  Level,
  User,
  UserAchievement,
  XpPoint,
  XpSummary,
  CourseProgress,
  SrsSummary,
  LevelSummary
} from '@shared/models';
import {XpService} from '@shared/services/xp.service';

type ResolvedRecommendation = {course: Course; score: number};

@Injectable({providedIn: 'root'})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly xp = inject(XpService);
  private readonly API = environment.apiUrl;

  load() {
    return forkJoin({
      me: this.http.get<User>(`${this.API}/users/me`),
      enrollments: this.http.get<CourseEnrollment[]>(`${this.API}/users/me/enrollments`),
      userAchievements: this.http.get<UserAchievement[]>(`${this.API}/users/me/achievements`),
      achievements: this.http.get<Achievement[]>(`${this.API}/achievements`),
      levels: this.http.get<Level[]>(`${this.API}/levels`),
      levelSummary: this.http.get<LevelSummary>(`${this.API}/users/me/level-summary`),
      xp7d: this.xp.getTimeseries('me', 7),
      xpSummary: this.xp.getSummaryForUser('me'),
      srsSummary: this.http.get<SrsSummary>(`${this.API}/users/me/srs/summary`),
      recommendation: this.getTopRecommendationCourse(),
      courseProgressList: this.http.get<CourseProgress[]>(`${this.API}/users/me/courses/progress`)
    }).pipe(
      switchMap(({
                   me,
                   enrollments,
                   userAchievements,
                   achievements,
                   levels,
                   levelSummary,
                   xp7d,
                   xpSummary,
                   srsSummary,
                   recommendation,
                   courseProgressList
                 }) => {
        const uniqueCourseIds = Array.from(new Set((enrollments ?? []).map(e => e.courseId)));

        const courses$ = uniqueCourseIds.length
          ? this.http.get<Course[]>(`${this.API}/courses/batch`, {
            params: new HttpParams().set('ids', uniqueCourseIds.join(','))
          })
          : of([] as Course[]);

        return forkJoin({courses: courses$}).pipe(
          switchMap(({courses}) => {
            const courseMap = new Map<number, Course>();
            courses.forEach(c => courseMap.set(c.id, c));

            const progressMap = new Map<number, number>();
            (courseProgressList ?? []).forEach(p => progressMap.set(p.courseId, p.progressPercent));

            const enriched = (enrollments ?? []).map(e => ({
              ...e,
              courseTitle: courseMap.get(e.courseId)?.title ?? `#${e.courseId}`,
              progressPercent: progressMap.get(e.courseId) ?? 0
            }));

            const byInProgress = enriched.filter(e => e.status === 'in_progress');
            const candidates = byInProgress.length ? byInProgress : enriched;
            const sorted = candidates.slice().sort((a, b) => {
              const la = a.lastActivityAt ? new Date(a.lastActivityAt).getTime() : 0;
              const lb = b.lastActivityAt ? new Date(b.lastActivityAt).getTime() : 0;
              if (lb !== la) return lb - la;
              const pa = typeof a.progressPercent === 'number' ? a.progressPercent : 0;
              const pb = typeof b.progressPercent === 'number' ? b.progressPercent : 0;
              return pb - pa;
            });

            const withCurrent = sorted.find(e => e.currentLessonId != null);
            const nextLessonId = withCurrent ? withCurrent.currentLessonId! : null;

            const achMap = new Map<number, Achievement>();
            (achievements ?? []).forEach(a => achMap.set(a.id, a));
            const recentAchievements = (userAchievements ?? [])
              .slice()
              .sort((a, b) => new Date(b.earnedAt).getTime() - new Date(a.earnedAt).getTime())
              .slice(0, 3)
              .map(ua => {
                const a = achMap.get(ua.achievementId);
                return {title: a?.title ?? 'Achievement', iconPath: a?.iconPath ?? null};
              });

            const contextCourseId = sorted[0]?.courseId ?? null;
            const contextCourse = contextCourseId ? (courseMap.get(contextCourseId) ?? null) : null;

            return this.ensureRecommendation(recommendation, contextCourse).pipe(
              map(rec => {
                const vm: DashboardVm = {
                  me,
                  enrollments: enriched,
                  nextLessonId,
                  recentAchievements,
                  xp7d: (xp7d ?? []) as XpPoint[],
                  totalXp: (xpSummary as XpSummary | undefined)?.totalXp ?? me.totalXp,
                  awardsCount: (xpSummary as XpSummary | undefined)?.awardsCount,
                  levels,
                  recommendation: rec ?? undefined,
                  srsSummary: srsSummary ?? undefined,
                  currentLevel: levelSummary.currentLevel,
                  percentToNext: levelSummary.percentToNext
                };
                return vm;
              })
            );
          })
        );
      })
    );
  }

  getDashboard() {
    return this.load();
  }

  private ensureRecommendation(existing: ResolvedRecommendation | null, contextCourse: Course | null) {
    if (existing) return of(existing);
    if (!contextCourse) return of(null);
    return this.generateTopRecommendationCourse(contextCourse).pipe(
      catchError(() => of(null))
    );
  }

  private getTopRecommendationCourse() {
    const params = new HttpParams().set('limit', '1');
    return this.http.get<CourseRecommendation[]>(`${this.API}/users/me/recommendations/top`, {params}).pipe(
      switchMap(list => {
        const first = (list ?? [])[0];
        if (!first) return of(null);
        const ids = [first.courseId];
        return this.http.get<Course[]>(`${this.API}/courses/batch`, {
          params: new HttpParams().set('ids', ids.join(','))
        }).pipe(
          map(batch => {
            const course = batch[0];
            if (!course) return null;
            return {course, score: Number(first.score)} as ResolvedRecommendation;
          })
        );
      }),
      catchError(() => of(null))
    );
  }

  private generateTopRecommendationCourse(contextCourse: Course) {
    const body = {
      limit: 1,
      learningLanguageCode: (contextCourse as any).learningLanguageCode,
      fromLanguageCode: (contextCourse as any).fromLanguageCode,
      levelCode: (contextCourse as any).levelCode
    };

    return this.http.post<CourseRecommendation[]>(`${this.API}/users/me/recommendations/generate`, body).pipe(
      switchMap(list => {
        const first = (list ?? [])[0];
        if (!first) return of(null);

        return this.http.get<Course[]>(`${this.API}/courses/batch`, {
          params: new HttpParams().set('ids', String(first.courseId))
        }).pipe(
          map(batch => {
            const course = batch[0];
            if (!course) return null;
            return {course, score: Number(first.score)} as ResolvedRecommendation;
          })
        );
      })
    );
  }
}
