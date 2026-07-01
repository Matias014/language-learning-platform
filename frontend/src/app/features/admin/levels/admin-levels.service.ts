import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from 'environments/environment';
import {Level, CreateLevelRequest, UpdateLevelRequest} from '@shared/models/user-level.model';

@Injectable({providedIn: 'root'})
export class AdminLevelsService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/levels`;

  list() {
    return this.http.get<Level[]>(this.base);
  }

  create(payload: CreateLevelRequest) {
    return this.http.post<Level>(this.base, payload);
  }

  update(level: number, payload: UpdateLevelRequest) {
    return this.http.patch<Level>(`${this.base}/${level}`, payload);
  }

  delete(level: number) {
    return this.http.delete<void>(`${this.base}/${level}`);
  }
}
