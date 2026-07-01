import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {Level} from '@shared/models';

@Injectable({providedIn: 'root'})
export class LevelsService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  list(): Observable<Level[]> {
    return this.http.get<Level[]>(`${this.api}/levels`);
  }
}
