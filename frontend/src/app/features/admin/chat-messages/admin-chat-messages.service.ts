import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from 'environments/environment';
import {Observable} from 'rxjs';
import {ChatMessage} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminChatMessagesService {
  private http = inject(HttpClient);
  private API = environment.apiUrl;

  listAll(): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.API}/chat-messages`);
  }

  listBySession(sessionId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.API}/chat-sessions/${sessionId}/messages`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/chat-messages/${id}`);
  }
}
