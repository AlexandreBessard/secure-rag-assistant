import { Component, inject, signal, ElementRef, ViewChild, AfterViewChecked, OnInit } from '@angular/core';
import { MarkdownComponent } from 'ngx-markdown';
import { KeycloakService } from './keycloak.service';
import { ChatService } from './chat.service';
import { UploadService } from './upload.service';

interface Message {
  text: string;
  sender: 'user' | 'bot';
  loading?: boolean;
}

@Component({
  selector: 'app-root',
  imports: [MarkdownComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit, AfterViewChecked {
  protected keycloak = inject(KeycloakService);
  private chat = inject(ChatService);
  private uploadService = inject(UploadService);

  messages = signal<Message[]>([]);
  inputText = signal('');
  ready = signal(false);

  showUpload = signal(false);
  uploadRole = signal('employee');
  uploadFile = signal<File | null>(null);
  uploadStatus = signal<'idle' | 'uploading' | 'done' | 'error'>('idle');

  readonly appRoles = ['employee', 'manager', 'hr', 'executive'];

  @ViewChild('messagesEnd') private messagesEnd!: ElementRef;

  ngAfterViewChecked() {
    this.messagesEnd?.nativeElement.scrollIntoView({ behavior: 'smooth' });
  }

  async ngOnInit() {
    this.messages.set([{ text: '', sender: 'bot', loading: true }]);
    const message = await this.chat.getWelcome(this.keycloak.username, this.keycloak.role);
    this.messages.set([{ text: message, sender: 'bot' }]);
    this.ready.set(true);
  }

  async sendMessage() {
    const text = this.inputText().trim();
    if (!text || !this.ready()) return;

    this.messages.update((msgs) => [...msgs, { text, sender: 'user' }]);
    this.inputText.set('');
    this.ready.set(false);

    this.messages.update((msgs) => [...msgs, { text: '', sender: 'bot', loading: true }]);

    try {
      const answer = await this.chat.ask(text);
      this.messages.update((msgs) => [...msgs.slice(0, -1), { text: answer, sender: 'bot' }]);
    } catch {
      this.messages.update((msgs) => [
        ...msgs.slice(0, -1),
        { text: 'Something went wrong. Please try again.', sender: 'bot' },
      ]);
    } finally {
      this.ready.set(true);
    }
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    this.uploadFile.set(input.files?.[0] ?? null);
    this.uploadStatus.set('idle');
  }

  async submitUpload() {
    const file = this.uploadFile();
    if (!file) return;
    this.uploadStatus.set('uploading');
    try {
      await this.uploadService.upload(file, this.uploadRole());
      this.uploadStatus.set('done');
      this.uploadFile.set(null);
    } catch {
      this.uploadStatus.set('error');
    }
  }
}
