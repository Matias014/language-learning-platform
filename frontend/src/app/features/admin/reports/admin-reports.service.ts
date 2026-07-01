import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams, HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {AdminHardestExercise, AdminLlmStats} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminReportsService {
  private http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  getLlmStats(): Observable<AdminLlmStats> {
    return this.http.get<AdminLlmStats>(`${this.api}/admin/stats/llm`);
  }

  getHardestExercises(limit: number): Observable<AdminHardestExercise[]> {
    const n = Math.min(Math.max(Math.floor(limit || 10), 1), 100);
    const params = new HttpParams().set('limit', String(n));
    return this.http.get<AdminHardestExercise[]>(`${this.api}/admin/stats/exercises/hardest`, {params});
  }

  downloadLlmCsv(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.api}/admin/stats/llm/export`, {observe: 'response', responseType: 'blob'});
  }

  downloadLlmPdf(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.api}/admin/stats/llm/export.pdf`, {observe: 'response', responseType: 'blob'});
  }

  downloadHardestCsv(limit: number): Observable<HttpResponse<Blob>> {
    const n = Math.min(Math.max(Math.floor(limit || 10), 1), 100);
    const params = new HttpParams().set('limit', String(n));
    return this.http.get(`${this.api}/admin/stats/exercises/hardest/export`, {
      observe: 'response',
      responseType: 'blob',
      params
    });
  }

  downloadHardestPdf(limit: number): Observable<HttpResponse<Blob>> {
    const n = Math.min(Math.max(Math.floor(limit || 10), 1), 100);
    const params = new HttpParams().set('limit', String(n));
    return this.http.get(`${this.api}/admin/stats/exercises/hardest/export.pdf`, {
      observe: 'response',
      responseType: 'blob',
      params
    });
  }
}
