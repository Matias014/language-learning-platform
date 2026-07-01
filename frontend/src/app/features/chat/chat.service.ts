import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';
import {environment} from 'environments/environment';
import {
  ChatMessage,
  ChatSession,
  CreateChatSessionRequest,
  UpdateChatSessionRequest,
  ChatSendRequest
} from '@shared/models';

export const MAX_USER_MESSAGE_CHARS = 1200;

@Injectable({providedIn: 'root'})
export class ChatService {
  private http = inject(HttpClient);
  private readonly API = environment.apiUrl;

  getSessionsMy(): Observable<ChatSession[]> {
    return this.http.get<ChatSession[]>(`${this.API}/users/me/chat-sessions`);
  }

  getSession(id: number): Observable<ChatSession> {
    return this.http.get<ChatSession>(`${this.API}/chat-sessions/${id}`);
  }

  createSession(body: CreateChatSessionRequest) {
    return this.http.post<ChatSession>(`${this.API}/chat-sessions`, body);
  }

  updateSession(id: number, patch: UpdateChatSessionRequest) {
    return this.http.patch<ChatSession>(`${this.API}/chat-sessions/${id}`, patch);
  }

  deleteSession(id: number) {
    return this.http.delete<void>(`${this.API}/chat-sessions/${id}`);
  }

  getMessages(sessionId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.API}/chat-sessions/${sessionId}/messages`).pipe(
      map(arr =>
        arr
          .map(m => this.mapMessage(m))
          .sort((a, b) => +new Date(a.sentAt) - +new Date(b.sentAt))
      )
    );
  }

  send(sessionId: number, body: ChatSendRequest) {
    return this.http.post<ChatMessage>(`${this.API}/chat-sessions/${sessionId}/ai-messages`, body).pipe(
      map(dto => this.mapMessage(dto))
    );
  }

  private mapMessage(dto: ChatMessage): ChatMessage {
    return {
      id: dto.id,
      sessionId: dto.sessionId,
      sender: dto.sender,
      message: dto.message,
      sentAt: dto.sentAt
    };
  }
}
