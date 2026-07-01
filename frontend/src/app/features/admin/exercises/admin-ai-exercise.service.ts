import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {GenerateExercisesRequest} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminAiExerciseService {
  private http = inject(HttpClient);
  private readonly API = environment.apiUrl;

  generate(lessonId: number, payload: GenerateExercisesRequest): Observable<number[]> {
    return this.http.post<number[]>(`${this.API}/lessons/${lessonId}/exercises`, payload);
  }
}
