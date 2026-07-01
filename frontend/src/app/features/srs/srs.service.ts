import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {forkJoin, map, of, Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {Exercise, Lesson, UserSrs} from '@shared/models';

@Injectable({providedIn: 'root'})
export class SrsService {
  private http = inject(HttpClient);
  private readonly API = environment.apiUrl;

  listSrsDue(beforeIso: string) {
    const params = new HttpParams().set('before', beforeIso);
    return this.http.get<UserSrs[]>(`${this.API}/users/me/srs/due`, {params});
  }

  review(exerciseId: number, quality: number): Observable<UserSrs> {
    return this.http.post<UserSrs>(`${this.API}/users/me/srs/review`, {exerciseId, quality});
  }

  getExercise(id: number) {
    return this.http.get<Exercise>(`${this.API}/exercises/${id}`);
  }

  getLesson(id: number) {
    return this.http.get<Lesson>(`${this.API}/lessons/${id}`);
  }

  loadExercises(ids: number[]) {
    const uniq = Array.from(new Set(ids)).filter(x => Number.isFinite(x));
    if (!uniq.length) return of(new Map<number, Exercise>());
    const calls = uniq.map(id => this.getExercise(id));
    return forkJoin(calls).pipe(
      map(list => {
        const m = new Map<number, Exercise>();
        for (const e of list) m.set(e.id as number, e);
        return m;
      })
    );
  }

  loadLessons(ids: number[]) {
    const uniq = Array.from(new Set(ids)).filter(x => Number.isFinite(x));
    if (!uniq.length) return of(new Map<number, Lesson>());
    const calls = uniq.map(id => this.getLesson(id));
    return forkJoin(calls).pipe(
      map(list => {
        const m = new Map<number, Lesson>();
        for (const l of list) m.set(l.id as number, l);
        return m;
      })
    );
  }
}
