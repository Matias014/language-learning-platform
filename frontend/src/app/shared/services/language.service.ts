import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {Language} from '@shared/models';

@Injectable({providedIn: 'root'})
export class LanguageService {
  private http = inject(HttpClient);
  private api = environment.apiUrl;

  list(): Observable<Language[]> {
    return this.http.get<Language[]>(`${this.api}/languages`);
  }
}
