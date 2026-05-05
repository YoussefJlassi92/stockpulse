import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { LoginRequest, LoginResponse } from '../models/auth.model';
import { AUTH_LOGIN } from './api.config';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should send POST to login endpoint and store token', () => {
    const request: LoginRequest = { userId: 'user1', password: 'pass' };
    const response: LoginResponse = { token: 'jwt-token', userId: 'user1', expiresIn: 3600 };

    service.login(request).subscribe((res) => {
      expect(res).toEqual(response);
      expect(localStorage.getItem('stockpulse_token')).toBe('jwt-token');
      expect(localStorage.getItem('stockpulse_userId')).toBe('user1');
    });

    const req = httpMock.expectOne(AUTH_LOGIN);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);
  });

  it('should clear localStorage on logout', () => {
    localStorage.setItem('stockpulse_token', 'jwt-token');
    localStorage.setItem('stockpulse_userId', 'user1');

    service.logout();

    expect(localStorage.getItem('stockpulse_token')).toBeNull();
    expect(localStorage.getItem('stockpulse_userId')).toBeNull();
  });

  it('should return true from isAuthenticated when token exists', () => {
    localStorage.setItem('stockpulse_token', 'jwt-token');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should return false from isAuthenticated when no token', () => {
    expect(service.isAuthenticated()).toBe(false);
  });
});
