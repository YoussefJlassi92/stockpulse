import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AlertRule, CreateAlertRequest } from '../models/alert.model';
import { ALERTS } from './api.config';

@Injectable({ providedIn: 'root' })
export class AlertService {
  constructor(private http: HttpClient) {}

  getAlerts(userId: string): Observable<AlertRule[]> {
    return this.http.get<AlertRule[]>(`${ALERTS}/${userId}`);
  }

  createAlert(request: CreateAlertRequest): Observable<AlertRule> {
    return this.http.post<AlertRule>(ALERTS, request);
  }

  deleteAlert(alertId: number): Observable<void> {
    return this.http.delete<void>(`${ALERTS}/${alertId}`);
  }
}
