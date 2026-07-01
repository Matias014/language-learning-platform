import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {Language, CreateLanguageRequest, UpdateLanguageRequest} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminLanguagesService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  list(): Observable<Language[]> {
    return this.http.get<Language[]>(`${this.API}/languages`);
  }

  create(body: CreateLanguageRequest): Observable<Language> {
    return this.http.post<Language>(`${this.API}/languages`, body);
  }

  update(code: string, patch: UpdateLanguageRequest): Observable<Language> {
    return this.http.patch<Language>(`${this.API}/languages/${code}`, patch);
  }

  delete(code: string) {
    return this.http.delete<void>(`${this.API}/languages/${code}`);
  }
}
