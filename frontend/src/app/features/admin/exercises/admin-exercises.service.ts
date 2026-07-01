import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from 'environments/environment';
import {Exercise, CreateExerciseRequest, UpdateExerciseRequest} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminExercisesService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  listByLesson(lessonId: number) {
    return this.http.get<Exercise[]>(`${this.API}/lessons/${lessonId}/exercises`);
  }

  get(id: number) {
    return this.http.get<Exercise>(`${this.API}/exercises/${id}`);
  }

  create(body: CreateExerciseRequest) {
    return this.http.post<Exercise>(`${this.API}/exercises`, body);
  }

  update(id: number, patch: UpdateExerciseRequest) {
    return this.http.patch<Exercise>(`${this.API}/exercises/${id}`, patch);
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.API}/exercises/${id}`);
  }
}
