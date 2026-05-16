import { Component, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';

interface Message {
  text: string;
  sender: 'user' | 'bot';
}

@Component({
  selector: 'app-root',
  imports: [],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements AfterViewChecked {
  messages = signal<Message[]>([]);
  inputText = signal('');

  @ViewChild('messagesEnd') private messagesEnd!: ElementRef;

  ngAfterViewChecked() {
    this.messagesEnd?.nativeElement.scrollIntoView({ behavior: 'smooth' });
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
