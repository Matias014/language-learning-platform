import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {environment} from 'environments/environment';
import {map} from 'rxjs/operators';
import {
  User,
  UserRole,
  PageResponse,
  CreateUserRequest,
  UpdateUserRequest,
  AdminChangeLoginRequest,
  AdminChangeEmailRequest,
  AdminSetPasswordRequest
} from '@app/shared/models';

@Injectable({providedIn: 'root'})
export class AdminUsersService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  search(opts?: { q?: string; role?: UserRole; page?: number; size?: number; sort?: string }) {
    let params = new HttpParams();
    if (opts?.q) params = params.set('q', opts.q);
    if (opts?.role) params = params.set('role', opts.role);
    if (opts?.page != null) params = params.set('page', String(opts.page));
    if (opts?.size != null) params = params.set('size', String(opts.size));
    if (opts?.sort) params = params.set('sort', opts.sort);
    return this.http.get<PageResponse<User>>(`${this.API}/users`, {params});
  }

  list() {
    return this.search({page: 0, size: 100, sort: 'login,asc'}).pipe(map(r => r.content ?? []));
  }

  create(body: CreateUserRequest) {
    return this.http.post<User>(`${this.API}/users`, body);
  }

  update(id: number, patch: UpdateUserRequest) {
    return this.http.patch<User>(`${this.API}/users/${id}`, patch);
  }

  changeLoginAdmin(id: number, newLogin: string) {
    const body: AdminChangeLoginRequest = {newLogin};
    return this.http.patch<User>(`${this.API}/users/${id}/login-admin`, body);
  }

  changeEmailAdmin(id: number, newEmail: string) {
    const body: AdminChangeEmailRequest = {newEmail};
    return this.http.patch<User>(`${this.API}/users/${id}/email-admin`, body);
  }

  setPasswordAdmin(id: number, newPassword: string) {
    const body: AdminSetPasswordRequest = {newPassword};
    return this.http.patch<void>(`${this.API}/users/${id}/password-admin`, body);
  }

  delete(id: number) {
    return this.http.delete<void>(`${this.API}/users/${id}`);
  }

  uploadAvatar(userId: number, file: File) {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.put<User>(`${this.API}/users/${userId}/avatar`, fd);
  }
}
