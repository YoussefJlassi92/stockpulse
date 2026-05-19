import { Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import { Position } from '../models/portfolio.model';
import { WS_PORTFOLIO_URL } from './api.config';

@Injectable({ providedIn: 'root' })
export class WebsocketService {
  readonly connected = signal(false);
  private client: Client | null = null;
  private connectPromise: Promise<void> | null = null;
  private connectResolve?: () => void;

  connect(token: string): void {
    this.connectPromise = new Promise((resolve) => {
      this.connectResolve = resolve;
    });

    this.client = new Client({
      brokerURL: WS_PORTFOLIO_URL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        this.connected.set(true);
        this.connectResolve?.();
      },
      onDisconnect: () => this.connected.set(false),
      reconnectDelay: 5000,
    });
    this.client.activate();
  }

  subscribeToPortfolioUpdates(portfolioId: number): Observable<Position> {
    return new Observable((observer) => {
      if (!this.client || !this.connectPromise) {
        observer.complete();
        return;
      }
      this.connectPromise.then(() => {
        if (!this.client) return;
        const sub = this.client.subscribe(
          `/topic/portfolio/${portfolioId}`,
          (message: IMessage) => {
            observer.next(JSON.parse(message.body) as Position);
          }
        );
        observer.add(() => sub.unsubscribe());
      });
    });
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.connected.set(false);
    this.connectPromise = null;
  }
}
