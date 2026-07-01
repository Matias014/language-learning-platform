import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {Achievement, CreateAchievementRequest, UpdateAchievementRequest} from '@shared/models/achievement.model';

@Injectable({providedIn: 'root'})
export class AdminAchievementsService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  list(): Observable<Achievement[]> {
    return this.http.get<Achievement[]>(`${this.API}/achievements`);
  }

  create(body: CreateAchievementRequest): Observable<Achievement> {
    return this.http.post<Achievement>(`${this.API}/achievements`, body);
  }

  update(id: number, patch: UpdateAchievementRequest): Observable<Achievement> {
    return this.http.patch<Achievement>(`${this.API}/achievements/${id}`, patch);
  }

  uploadIcon(id: number, file: File): Observable<Achievement> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.put<Achievement>(`${this.API}/achievements/${id}/icon`, fd);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/achievements/${id}`);
  }
}
