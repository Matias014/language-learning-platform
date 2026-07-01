import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from 'environments/environment';
import {Observable, catchError, of} from 'rxjs';
import {ExerciseAward} from '@app/shared/models/exercise-award.model';

@Injectable({providedIn: 'root'})
export class AdminExerciseAwardsService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  listAll(): Observable<ExerciseAward[]> {
    return this.http.get<ExerciseAward[]>(`${this.API}/exercise-awards`).pipe(
      catchError(err => (err?.status === 404 ? of<ExerciseAward[]>([]) : (() => {
        throw err;
      })()))
    );
  }

  listByUser(userId: number): Observable<ExerciseAward[]> {
    return this.http.get<ExerciseAward[]>(`${this.API}/users/${userId}/exercise-awards`).pipe(
      catchError(err => (err?.status === 404 ? of<ExerciseAward[]>([]) : (() => {
        throw err;
      })()))
    );
  }
}
