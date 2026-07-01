import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from 'environments/environment';
import {LlmLog} from '@shared/models';

@Injectable({providedIn: 'root'})
export class AdminLlmLogsService {
  private http = inject(HttpClient);
  private readonly API = environment.apiUrl;

  listAll() {
    return this.http.get<LlmLog[]>(`${this.API}/llm-logs`);
  }

  byUser(userId: number) {
    return this.http.get<LlmLog[]>(`${this.API}/users/${userId}/llm-logs`);
  }

  byLesson(lessonId: number) {
    return this.http.get<LlmLog[]>(`${this.API}/lessons/${lessonId}/llm-logs`);
  }

  byAttempt(attemptId: number) {
    return this.http.get<LlmLog[]>(`${this.API}/exercise-attempts/${attemptId}/llm-logs`);
  }

  bySession(sessionId: number) {
    return this.http.get<LlmLog[]>(`${this.API}/chat-sessions/${sessionId}/llm-logs`);
  }
}
