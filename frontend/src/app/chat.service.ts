import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

interface WelcomeResponse {
  message: string;
}

export interface Source {
  documentName: string;
  documentId: string;
}

interface AskResponse {
  answer: string;
  sources: Source[];
}

export interface HistoryMessage {
  role: 'user' | 'bot';
  text: string;
  sources?: Source[];
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);
  private base = 'http://localhost:8080';

  getWelcome(username: string, role: string): Promise<string> {
    return firstValueFrom(
      this.http.get<WelcomeResponse>(`${this.base}/welcome`, {
        params: { username, role },
      }),
    ).then((r) => r.message);
  }

  ask(question: string): Promise<{ answer: string; sources: Source[] }> {
    return firstValueFrom(
      this.http.post<AskResponse>(`${this.base}/ask`, { question }),
    ).then((r) => ({ answer: r.answer, sources: r.sources ?? [] }));
  }

  getHistory(): Promise<HistoryMessage[]> {
    return firstValueFrom(this.http.get<HistoryMessage[]>(`${this.base}/history`)).catch(() => []);
  }

  clearHistory(): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.base}/history`));
  }
}
