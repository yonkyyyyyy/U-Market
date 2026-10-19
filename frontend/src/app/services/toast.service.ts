import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'info';

export interface ToastMessage {
  id: number;
  kind: ToastKind;
  title: string;
  description?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  readonly messages = signal<ToastMessage[]>([]);
  private nextId = 0;
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();

  success(title: string, description?: string): void {
    this.show('success', title, description);
  }

  error(title: string, description?: string): void {
    this.show('error', title, description);
  }

  info(title: string, description?: string): void {
    this.show('info', title, description);
  }

  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
    this.messages.update(messages => messages.filter(message => message.id !== id));
  }

  private show(kind: ToastKind, title: string, description?: string): void {
    const id = ++this.nextId;
    this.messages.update(messages => [...messages, { id, kind, title, description }]);
    this.timers.set(id, setTimeout(() => this.dismiss(id), 4200));
  }
}
