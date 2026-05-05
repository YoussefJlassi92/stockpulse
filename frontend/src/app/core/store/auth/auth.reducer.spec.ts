import { authReducer, initialState } from './auth.reducer';
import { login, loginSuccess, loginFailure, logout } from './auth.actions';
import { LoginRequest, LoginResponse } from '../../models/auth.model';

describe('authReducer', () => {
  const mockRequest: LoginRequest = { userId: 'user1', password: 'pass' };
  const mockResponse: LoginResponse = { token: 'jwt-token', userId: 'user1', expiresIn: 3600 };

  it('should return initial state for unknown action', () => {
    const state = authReducer(undefined, { type: '@@UNKNOWN' });
    expect(state).toEqual(initialState);
  });

  it('should set loading true and clear error on login', () => {
    const state = authReducer(
      { ...initialState, error: 'previous error' },
      login({ request: mockRequest })
    );
    expect(state.loading).toBe(true);
    expect(state.error).toBeNull();
  });

  it('should set token, userId, isAuthenticated on loginSuccess', () => {
    const state = authReducer(
      { ...initialState, loading: true },
      loginSuccess({ response: mockResponse })
    );
    expect(state.token).toBe('jwt-token');
    expect(state.userId).toBe('user1');
    expect(state.isAuthenticated).toBe(true);
    expect(state.loading).toBe(false);
    expect(state.error).toBeNull();
  });

  it('should set error and loading false on loginFailure', () => {
    const state = authReducer(
      { ...initialState, loading: true },
      loginFailure({ error: 'Invalid credentials' })
    );
    expect(state.loading).toBe(false);
    expect(state.error).toBe('Invalid credentials');
    expect(state.isAuthenticated).toBe(false);
  });

  it('should reset to initial state on logout', () => {
    const loggedInState = {
      token: 'jwt-token',
      userId: 'user1',
      isAuthenticated: true,
      loading: false,
      error: null,
    };
    const state = authReducer(loggedInState, logout());
    expect(state).toEqual(initialState);
  });
});
