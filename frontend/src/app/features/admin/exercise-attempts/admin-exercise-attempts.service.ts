import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, catchError, of} from 'rxjs';
import {environment} from 'environments/environment';
import {ExerciseAttempt} from '@shared/models';
import {GradeResponse} from '@shared/models/ai.model';

@Injectable({providedIn: 'root'})
export class AdminExerciseAttemptsService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  listAll(): Observable<ExerciseAttempt[]> {
    return this.http.get<ExerciseAttempt[]>(`${this.API}/exercise-attempts`).pipe(
      catchError(err => (err?.status === 404 ? of<ExerciseAttempt[]>([]) : (() => {
        throw err;
      })()))
    );
  }

  listByExercise(exerciseId: number): Observable<ExerciseAttempt[]> {
    return this.http.get<ExerciseAttempt[]>(`${this.API}/exercises/${exerciseId}/attempts`).pipe(
      catchError(err => (err?.status === 404 ? of<ExerciseAttempt[]>([]) : (() => {
        throw err;
      })()))
    );
  }

  listByUserAndExercise(userId: number, exerciseId: number): Observable<ExerciseAttempt[]> {
    return this.http.get<ExerciseAttempt[]>(`${this.API}/users/${userId}/exercises/${exerciseId}/attempts`).pipe(
      catchError(err => (err?.status === 404 ? of<ExerciseAttempt[]>([]) : (() => {
        throw err;
      })()))
    );
  }

  get(id: number): Observable<ExerciseAttempt> {
    return this.http.get<ExerciseAttempt>(`${this.API}/exercise-attempts/${id}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/exercise-attempts/${id}`);
  }

  grade(id: number): Observable<GradeResponse> {
    return this.http.post<GradeResponse>(`${this.API}/exercise-attempts/${id}/evaluations`, {});
  }
}
