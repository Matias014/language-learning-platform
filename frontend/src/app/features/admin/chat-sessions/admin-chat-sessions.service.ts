import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from 'environments/environment';
import {Observable} from 'rxjs';
import {ChatSession} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminChatSessionsService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  listAll(): Observable<ChatSession[]> {
    return this.http.get<ChatSession[]>(`${this.API}/chat-sessions`);
  }

  listByUser(userId: number): Observable<ChatSession[]> {
    return this.http.get<ChatSession[]>(`${this.API}/users/${userId}/chat-sessions`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/chat-sessions/${id}`);
  }
}
