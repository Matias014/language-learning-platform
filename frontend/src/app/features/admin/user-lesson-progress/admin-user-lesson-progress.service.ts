import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable, catchError, of} from 'rxjs';
import {environment} from 'environments/environment';
import {LessonStatus, UserLessonProgress} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminUserLessonProgressService {
  private http = inject(HttpClient);
  private api = environment.apiUrl;

  listAll(): Observable<UserLessonProgress[]> {
    return this.http.get<UserLessonProgress[]>(`${this.api}/user-lesson-progress`).pipe(
      catchError(err => (err?.status === 404 ? of<UserLessonProgress[]>([]) : (() => { throw err; })()))
    );
  }

  listByUser(userId: number, status?: LessonStatus): Observable<UserLessonProgress[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<UserLessonProgress[]>(`${this.api}/users/${userId}/lesson-progress`, {params}).pipe(
      catchError(err => (err?.status === 404 ? of<UserLessonProgress[]>([]) : (() => { throw err; })()))
    );
  }

  getByUserAndLesson(userId: number, lessonId: number): Observable<UserLessonProgress | null> {
    return this.http.get<UserLessonProgress>(`${this.api}/users/${userId}/lessons/${lessonId}/progress`).pipe(
      catchError(err => (err?.status === 404 ? of<UserLessonProgress | null>(null) : (() => { throw err; })()))
    );
  }
}
