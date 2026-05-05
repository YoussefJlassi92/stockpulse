import {
  selectAuthState,
  selectIsAuthenticated,
  selectUserId,
  selectAuthLoading,
  selectAuthError,
} from './auth.selectors';
import { AuthState } from '../../models/auth.model';
import { AppState } from '../index';

describe('auth selectors', () => {
  const authState: AuthState = {
    token: 'jwt-token',
    userId: 'user1',
    isAuthenticated: true,
    loading: false,
    error: null,
  };

  const appState: AppState = { auth: authState };

  it('selectAuthState should return auth slice', () => {
    expect(selectAuthState(appState)).toEqual(authState);
  });

  it('selectIsAuthenticated should return isAuthenticated', () => {
    expect(selectIsAuthenticated(appState)).toBe(true);
  });

  it('selectUserId should return userId', () => {
    expect(selectUserId(appState)).toBe('user1');
  });

  it('selectAuthLoading should return loading', () => {
    expect(selectAuthLoading(appState)).toBe(false);
  });

  it('selectAuthError should return error', () => {
    expect(selectAuthError(appState)).toBeNull();
  });

  it('selectIsAuthenticated should return false when not authenticated', () => {
    const unauthState: AppState = {
      auth: { ...authState, isAuthenticated: false, token: null, userId: null },
    };
    expect(selectIsAuthenticated(unauthState)).toBe(false);
  });
});
