import {Injectable, inject} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {ExerciseAward, XpSummary, XpPoint, XpBreakdown} from '@shared/models';

@Injectable({providedIn: 'root'})
export class XpService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  listAwardsForUser(userId: number | 'me'): Observable<ExerciseAward[]> {
    const url = userId === 'me' ? `${this.api}/users/me/exercise-awards` : `${this.api}/users/${userId}/exercise-awards`;
    return this.http.get<ExerciseAward[]>(url);
  }

  getSummaryForUser(userId: number | 'me'): Observable<XpSummary> {
    const url = userId === 'me' ? `${this.api}/users/me/xp/summary` : `${this.api}/users/${userId}/xp/summary`;
    return this.http.get<XpSummary>(url);
  }

  getTimeseries(userId: number | 'me', days: 7 | 30 | 90): Observable<XpPoint[]> {
    const base = userId === 'me' ? `${this.api}/users/me/xp/timeseries` : `${this.api}/users/${userId}/xp/timeseries`;
    const params = new HttpParams().set('days', String(days));
    return this.http.get<XpPoint[]>(base, {params});
  }

  getMyBreakdown(days: 7 | 30 | 90): Observable<XpBreakdown> {
    const url = `${this.api}/users/me/xp/breakdown`;
    const params = new HttpParams().set('days', String(days));
    return this.http.get<XpBreakdown>(url, {params});
  }
}
