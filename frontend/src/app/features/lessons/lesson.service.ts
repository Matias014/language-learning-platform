import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, forkJoin, map, switchMap, catchError, of} from 'rxjs';
import {
  Course,
  CreateExerciseAttemptRequest,
  Exercise,
  ExerciseAttempt,
  ExerciseOption,
  ExerciseWithOptions,
  GradeResponse,
  HintRequest,
  HintResponse,
  Lesson
} from '@shared/models';
import {environment} from 'environments/environment';
import {UserLessonProgressService} from '@shared/services/user-lesson-progress.service';
import {UserLessonProgress, UpdateUserLessonProgressRequest} from '@shared/models/user-lesson-progress.model';

@Injectable({providedIn: 'root'})
export class LessonService {
  private http = inject(HttpClient);
  private readonly API = environment.apiUrl;
  private readonly ulp = inject(UserLessonProgressService);

  getLesson(id: number): Observable<Lesson> {
    return this.http.get<Lesson>(`${this.API}/lessons/${id}`);
  }

  getCourse(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.API}/courses/${id}`);
  }

  getExercisesByLesson(lessonId: number): Observable<Exercise[]> {
    return this.http.get<Exercise[]>(`${this.API}/lessons/${lessonId}/exercises`).pipe(
      map(rows => (rows ?? []).slice().sort((a, b) => a.orderNumber - b.orderNumber))
    );
  }

  private getOptionsForExercises(exercises: Exercise[]): Observable<Record<number, ExerciseOption[]>> {
    const calls = exercises.map(e => this.http.get<ExerciseOption[]>(`${this.API}/exercises/${e.id}/options`));
    return forkJoin(calls).pipe(
      map(responses => {
        const out: Record<number, ExerciseOption[]> = {};
        exercises.forEach((e, i) => {
          out[e.id] = (responses[i] ?? []).slice().sort((a, b) => a.orderNumber - b.orderNumber);
        });
        return out;
      })
    );
  }

  getExercisesWithOptions(lessonId: number): Observable<ExerciseWithOptions[]> {
    return this.getExercisesByLesson(lessonId).pipe(
      switchMap(exs => {
        if (!exs.length) return of([]);
        return this.getOptionsForExercises(exs).pipe(
          map(byId => exs.map(e => ({...e, options: byId[e.id] ?? []} as ExerciseWithOptions)))
        );
      })
    );
  }

  createAttempt(body: CreateExerciseAttemptRequest): Observable<{ id: number }> {
    return this.http.post<{ id: number }>(`${this.API}/exercise-attempts`, body);
  }

  evaluateAttempt(attemptId: number): Observable<GradeResponse> {
    return this.http.post<GradeResponse>(`${this.API}/exercise-attempts/${attemptId}/evaluations`, {});
  }

  listMyAttempts(exerciseId: number): Observable<ExerciseAttempt[]> {
    return this.http.get<ExerciseAttempt[]>(`${this.API}/users/me/exercises/${exerciseId}/attempts`).pipe(
      map(rows => (rows ?? []).slice().sort((a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime()))
    );
  }

  hint(body: HintRequest): Observable<HintResponse> {
    return this.http.post<HintResponse>(`${this.API}/ai/hints`, body);
  }

  ensureMyProgress(lessonId: number): Observable<UserLessonProgress> {
    return this.ulp.getMyLessonProgress(lessonId).pipe(
      catchError(err => {
        if (err?.status === 404) {
          return this.ulp.createMyProgress({lessonId});
        }
        throw err;
      })
    );
  }

  markLessonCompleted(lessonId: number): Observable<UserLessonProgress> {
    return this.ensureMyProgress(lessonId).pipe(
      switchMap(p => this.ulp.updateProgress(p.id, {status: 'completed'} as UpdateUserLessonProgressRequest))
    );
  }

  getMyLessonProgressPercent(lessonId: number): Observable<number> {
    return this.http
      .get<{ lessonId: number; progressPercent: number }>(`${this.API}/users/me/lessons/${lessonId}/progress-percent`)
      .pipe(map(r => Math.max(0, Math.min(100, Number(r?.progressPercent ?? 0)))));
  }
}
