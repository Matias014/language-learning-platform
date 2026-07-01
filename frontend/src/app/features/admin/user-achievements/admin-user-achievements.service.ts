import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, catchError, of} from 'rxjs';
import {environment} from 'environments/environment';
import {UserAchievement} from '@app/shared/models';

@Injectable({providedIn: 'root'})
export class AdminUserAchievementsService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  listAll(): Observable<UserAchievement[]> {
    return this.http.get<UserAchievement[]>(`${this.API}/user-achievements`).pipe(
      catchError(err => (err?.status === 404 ? of<UserAchievement[]>([]) : (() => { throw err; })()))
    );
  }

  listByUser(userId: number): Observable<UserAchievement[]> {
    return this.http.get<UserAchievement[]>(`${this.API}/users/${userId}/achievements`).pipe(
      catchError(err => (err?.status === 404 ? of<UserAchievement[]>([]) : (() => { throw err; })()))
    );
  }

  listByAchievement(achievementId: number): Observable<UserAchievement[]> {
    return this.http.get<UserAchievement[]>(`${this.API}/achievements/${achievementId}/user-achievements`).pipe(
      catchError(err => (err?.status === 404 ? of<UserAchievement[]>([]) : (() => { throw err; })()))
    );
  }

  getByUserAndAchievement(userId: number, achievementId: number): Observable<UserAchievement | null> {
    return this.http.get<UserAchievement>(`${this.API}/users/${userId}/achievements/${achievementId}`).pipe(
      catchError(err => (err?.status === 404 ? of<UserAchievement | null>(null) : (() => { throw err; })()))
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/user-achievements/${id}`);
  }
}
