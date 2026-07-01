import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from 'environments/environment';
import {ExerciseOption, CreateExerciseOptionRequest, UpdateExerciseOptionRequest} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminExerciseOptionsService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  list(exerciseId: number) {
    return this.http.get<ExerciseOption[]>(`${this.API}/exercises/${exerciseId}/options`);
  }

  create(body: CreateExerciseOptionRequest) {
    return this.http.post<ExerciseOption>(`${this.API}/exercise-options`, body);
  }

  update(id: number, patch: UpdateExerciseOptionRequest) {
    return this.http.patch<ExerciseOption>(`${this.API}/exercise-options/${id}`, patch);
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.API}/exercise-options/${id}`);
  }
}
