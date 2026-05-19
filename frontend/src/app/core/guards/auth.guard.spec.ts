import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { firstValueFrom } from 'rxjs';
import { authGuard } from './auth.guard';
import { selectIsAuthenticated } from '../store/auth/auth.selectors';
import { AuthState } from '../models/auth.model';

const emptyAuthState: AuthState = {
  token: null, userId: null, isAuthenticated: false, loading: false, error: null,
};

describe('authGuard', () => {
  let store: MockStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideMockStore({ initialState: { auth: emptyAuthState } }),
        provideRouter([]),
      ],
    });
    store = TestBed.inject(MockStore);
  });

  afterEach(() => store.resetSelectors());

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

  it('should return true when user is authenticated', async () => {
    store.overrideSelector(selectIsAuthenticated, true);
    store.refreshState();

    const result = await firstValueFrom(runGuard() as ReturnType<typeof runGuard> & { subscribe: unknown } as never);
    expect(result).toBe(true);
  });

  it('should return a UrlTree redirecting to /login when not authenticated', async () => {
    store.overrideSelector(selectIsAuthenticated, false);
    store.refreshState();

    const result = await firstValueFrom(runGuard() as ReturnType<typeof runGuard> & { subscribe: unknown } as never);
    expect(result).toBeInstanceOf(UrlTree);
    const router = TestBed.inject(Router);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });

  it('should not allow access with null token (default state)', async () => {
    const result = await firstValueFrom(runGuard() as ReturnType<typeof runGuard> & { subscribe: unknown } as never);
    expect(result).not.toBe(true);
  });
});
