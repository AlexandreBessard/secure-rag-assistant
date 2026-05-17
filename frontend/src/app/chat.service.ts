import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

interface WelcomeResponse {
  message: string;
}

interface AskResponse {
  answer: string;
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

  ask(question: string): Promise<string> {
    return firstValueFrom(
      this.http.post<AskResponse>(`${this.base}/ask`, { question }),
    ).then((r) => r.answer);
  }
}
