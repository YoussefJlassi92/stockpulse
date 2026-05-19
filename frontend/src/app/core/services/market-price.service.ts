import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { StockPrice } from '../models/market.model';
import { STOCK_PRICES } from './api.config';

@Injectable({ providedIn: 'root' })
export class MarketPriceService {
  private readonly http = inject(HttpClient);
  readonly prices = signal<Map<string, StockPrice>>(new Map());

  getLatestPrices(): Observable<StockPrice[]> {
    return this.http.get<StockPrice[]>(STOCK_PRICES).pipe(
      tap((stockPrices) => {
        const map = new Map<string, StockPrice>();
        stockPrices.forEach((p) => map.set(p.symbol, p));
        this.prices.set(map);
      })
    );
  }
}
