import { Component, inject, signal, ElementRef, ViewChild, AfterViewChecked, OnInit } from '@angular/core';
import { KeycloakService } from './keycloak.service';
import { ChatService } from './chat.service';

interface Message {
  text: string;
  sender: 'user' | 'bot';
  loading?: boolean;
}

@Component({
  selector: 'app-root',
  imports: [],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit, AfterViewChecked {
  protected keycloak = inject(KeycloakService);
  private chat = inject(ChatService);

  messages = signal<Message[]>([]);
  inputText = signal('');
  ready = signal(false);

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

  sendMessage() {
    const text = this.inputText().trim();
    if (!text) return;

    this.messages.update((msgs) => [...msgs, { text, sender: 'user' }]);
    this.inputText.set('');

    setTimeout(() => {
      this.messages.update((msgs) => [...msgs, { text: 'Got it', sender: 'bot' }]);
    }, 300);
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}
