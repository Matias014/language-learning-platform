import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {
  User,
  XpPoint,
  XpSummary,
  UserLessonProgress,
  LessonStatus,
  XpBreakdownItem,
  XpBreakdown,
  SrsThisWeek,
  SrsSummary
} from '@shared/models';
import {XpService} from '@shared/services/xp.service';
import {UserLessonProgressService} from '@shared/services/user-lesson-progress.service';
import {EffectivenessStats} from '@shared/models/effectiveness-stats.model';
import {ActivityStats} from '@shared/models/activity-stats.model';

@Injectable({providedIn: 'root'})
export class StatsService {
  private http = inject(HttpClient);
  private xp = inject(XpService);
  private ulp = inject(UserLessonProgressService);
  private readonly api = environment.apiUrl;

  getMe(): Observable<User> {
    return this.http.get<User>(`${this.api}/users/me`);
  }

  getXpSummary(): Observable<XpSummary> {
    return this.xp.getSummaryForUser('me');
  }

  getTimeseries(days: 7 | 30 | 90): Observable<XpPoint[]> {
    return this.xp.getTimeseries('me', days);
  }

  getXpBreakdowns(days: 7 | 30 | 90): Observable<{ type: XpBreakdownItem[]; difficulty: XpBreakdownItem[] }> {
    return this.xp.getMyBreakdown(days).pipe(
      map((dto: XpBreakdown) => {
        const type: XpBreakdownItem[] = Object.entries(dto.byType || {}).map(([k, v]) => ({key: k, xp: v as number}));
        const difficulty: XpBreakdownItem[] = Object.entries(dto.byDifficulty || {}).map(([k, v]) => ({
          key: k,
          xp: v as number
        }));
        return {type, difficulty};
      })
    );
  }

  getSrsThisWeek(): Observable<SrsThisWeek> {
    return this.http.get<SrsThisWeek>(`${this.api}/users/me/srs/this-week`);
  }

  getSrsSummary(): Observable<SrsSummary> {
    return this.http.get<SrsSummary>(`${this.api}/users/me/srs/summary`);
  }

  getEffectivenessStats(): Observable<EffectivenessStats> {
    return this.http.get<EffectivenessStats>(`${this.api}/users/me/stats/effectiveness`);
  }

  getActivityStats(): Observable<ActivityStats> {
    return this.http.get<ActivityStats>(`${this.api}/users/me/stats/activity`);
  }

  listMyProgress(status?: LessonStatus): Observable<UserLessonProgress[]> {
    return this.ulp.listMyProgress(status);
  }

  countCompletedLessons(): Observable<number> {
    return this.listMyProgress('completed').pipe(map(arr => arr?.length ?? 0));
  }

  exportUserPdf(lang?: string): Observable<Blob> {
    const params = lang ? {params: {lang}} : undefined;
    return this.http.get(`${this.api}/users/me/export`, {responseType: 'blob', ...(params ?? {})});
  }
}
