import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Portfolio, Position, AddPositionRequest } from '../models/portfolio.model';
import { PORTFOLIOS } from './api.config';

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  constructor(private http: HttpClient) {}

  getPortfolios(userId: string): Observable<Portfolio[]> {
    return this.http.get<Portfolio[]>(`${PORTFOLIOS}/${userId}`);
  }

  getPositions(portfolioId: number): Observable<Position[]> {
    return this.http.get<Position[]>(`${PORTFOLIOS}/${portfolioId}/positions`);
  }

  addPosition(portfolioId: number, request: AddPositionRequest): Observable<Position> {
    return this.http.post<Position>(`${PORTFOLIOS}/${portfolioId}/positions`, request);
  }

  createPortfolio(userId: string, name: string): Observable<Portfolio> {
    return this.http.post<Portfolio>(`${PORTFOLIOS}/${userId}`, { name });
  }
}
