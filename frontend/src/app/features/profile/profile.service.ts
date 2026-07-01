import {Injectable, inject} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams, HttpResponse} from '@angular/common/http';
import {forkJoin, Observable} from 'rxjs';
import {environment} from 'environments/environment';
import {
  User,
  UpdateUserRequest,
  ChangeLoginRequest,
  ChangeEmailRequest,
  ChangePasswordRequest,
  Level,
  UserAchievement,
} from '@shared/models';

@Injectable({providedIn: 'root'})
export class ProfileService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  loadBundle(): Observable<{ me: User; achievements: UserAchievement[]; levels: Level[] }> {
    return forkJoin({
      me: this.http.get<User>(`${this.api}/users/me`),
      achievements: this.http.get<UserAchievement[]>(`${this.api}/users/me/achievements`),
      levels: this.http.get<Level[]>(`${this.api}/levels`),
    });
  }

  updateMe(body: UpdateUserRequest): Observable<User> {
    return this.http.patch<User>(`${this.api}/users/me`, body);
  }

  changeLogin(body: ChangeLoginRequest): Observable<User> {
    return this.http.patch<User>(`${this.api}/users/me/login`, body);
  }

  changeEmail(body: ChangeEmailRequest): Observable<User> {
    return this.http.patch<User>(`${this.api}/users/me/email`, body);
  }

  changePassword(body: ChangePasswordRequest): Observable<void> {
    return this.http.patch<void>(`${this.api}/users/me/password`, body);
  }

  uploadAvatar(file: File): Observable<User> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.put<User>(`${this.api}/users/me/avatar`, fd);
  }

  exportPdf(lang: string): Observable<HttpResponse<Blob>> {
    const params = new HttpParams().set('lang', lang);
    const headers = new HttpHeaders().set('Accept', 'application/pdf');
    return this.http.get(`${this.api}/users/me/export`, {
      params,
      headers,
      observe: 'response',
      responseType: 'blob',
    });
  }

  deleteMe(): Observable<void> {
    return this.http.delete<void>(`${this.api}/users/me`);
  }
}
