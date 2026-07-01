import {Injectable, inject} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {
  UserLessonProgress,
  CreateUserLessonProgressRequest,
  UpdateUserLessonProgressRequest
} from '@shared/models/user-lesson-progress.model';
import {LessonStatus} from '@shared/models';

@Injectable({providedIn: 'root'})
export class UserLessonProgressService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getMyLessonProgress(lessonId: number): Observable<UserLessonProgress> {
    return this.http.get<UserLessonProgress>(`${this.apiUrl}/users/me/lessons/${lessonId}/progress`);
  }

  listMyProgress(status?: LessonStatus): Observable<UserLessonProgress[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<UserLessonProgress[]>(`${this.apiUrl}/users/me/lesson-progress`, {params});
  }

  createMyProgress(req: CreateUserLessonProgressRequest): Observable<UserLessonProgress> {
    return this.http.post<UserLessonProgress>(`${this.apiUrl}/user-lesson-progress`, req);
  }

  updateProgress(id: number, req: UpdateUserLessonProgressRequest): Observable<UserLessonProgress> {
    return this.http.patch<UserLessonProgress>(`${this.apiUrl}/user-lesson-progress/${id}`, req);
  }
}
