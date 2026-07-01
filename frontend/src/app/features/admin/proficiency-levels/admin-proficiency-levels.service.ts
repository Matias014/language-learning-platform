import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {ProficiencyLevel, CreateProficiencyLevelRequest, UpdateProficiencyLevelRequest} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminProficiencyLevelsService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  list(): Observable<ProficiencyLevel[]> {
    return this.http.get<ProficiencyLevel[]>(`${this.API}/proficiency-levels`);
  }

  create(body: CreateProficiencyLevelRequest): Observable<ProficiencyLevel> {
    return this.http.post<ProficiencyLevel>(`${this.API}/proficiency-levels`, body);
  }

  update(code: string, patch: UpdateProficiencyLevelRequest): Observable<ProficiencyLevel> {
    return this.http.patch<ProficiencyLevel>(`${this.API}/proficiency-levels/${code}`, patch);
  }

  delete(code: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/proficiency-levels/${code}`);
  }
}
