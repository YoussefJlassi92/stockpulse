import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AlertService } from './alert.service';
import { AlertRule, CreateAlertRequest } from '../models/alert.model';
import { ALERTS } from './api.config';

describe('AlertService', () => {
  let service: AlertService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AlertService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AlertService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should send GET to correct URL for getAlerts', () => {
    const mockAlerts: AlertRule[] = [
      {
        id: 1,
        userId: 'user1',
        symbol: 'AAPL',
        thresholdPrice: 200,
        alertType: 'ABOVE',
        email: 'test@example.com',
        active: true,
        createdAt: '2024-01-01',
      },
    ];

    service.getAlerts('user1').subscribe((alerts) => {
      expect(alerts).toEqual(mockAlerts);
    });

    const req = httpMock.expectOne(`${ALERTS}/user1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockAlerts);
  });

  it('should send POST with correct body for createAlert', () => {
    const request: CreateAlertRequest = {
      userId: 'user1',
      symbol: 'AAPL',
      thresholdPrice: 200,
      alertType: 'ABOVE',
      email: 'test@example.com',
    };
    const mockAlert: AlertRule = {
      id: 1,
      ...request,
      active: true,
      createdAt: '2024-01-01',
    };

    service.createAlert(request).subscribe((alert) => {
      expect(alert).toEqual(mockAlert);
    });

    const req = httpMock.expectOne(ALERTS);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockAlert);
  });

  it('should send DELETE to correct URL for deleteAlert', () => {
    service.deleteAlert(5).subscribe();

    const req = httpMock.expectOne(`${ALERTS}/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
