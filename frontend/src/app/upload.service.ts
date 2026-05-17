import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UploadService {
  private http = inject(HttpClient);
  private base = 'http://localhost:8080';

  upload(file: File, targetRole: string): Promise<{ key: string }> {
    const form = new FormData();
    form.append('file', file);
    form.append('targetRole', targetRole);
    return firstValueFrom(this.http.post<{ key: string }>(`${this.base}/upload`, form));
  }
}
