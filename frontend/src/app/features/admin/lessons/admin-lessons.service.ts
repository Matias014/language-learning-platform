import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from 'environments/environment';
import {Observable} from 'rxjs';
import {Lesson, CreateLessonRequest, UpdateLessonRequest} from '@app/shared/models/lesson.model';

@Injectable({providedIn: 'root'})
export class AdminLessonsService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  listByCourse(courseId: number): Observable<Lesson[]> {
    return this.http.get<Lesson[]>(`${this.API}/courses/${courseId}/lessons`);
  }

  create(body: CreateLessonRequest): Observable<Lesson> {
    return this.http.post<Lesson>(`${this.API}/lessons`, body);
  }

  update(id: number, patch: UpdateLessonRequest): Observable<Lesson> {
    return this.http.patch<Lesson>(`${this.API}/lessons/${id}`, patch);
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.API}/lessons/${id}`);
  }
}
