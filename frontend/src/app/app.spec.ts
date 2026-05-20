import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { App } from './app';
import { authReducer } from './core/store/auth/auth.reducer';
import { AuthEffects } from './core/store/auth/auth.effects';
import { AuthService } from './core/services/auth.service';

describe('App', () => {
  const authServiceMock = {
    getToken: vi.fn().mockReturnValue(null),
    getUserId: vi.fn().mockReturnValue(null),
    logout: vi.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideStore({ auth: authReducer }),
        provideEffects([AuthEffects]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();
  });

  afterEach(() => vi.clearAllMocks());

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should contain a router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).not.toBeNull();
  });
});
