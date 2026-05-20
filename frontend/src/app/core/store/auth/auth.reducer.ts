import { createReducer, on } from '@ngrx/store';
import { AuthState } from '../../models/auth.model';
import { login, loginSuccess, loginFailure, logout, initAuthSuccess } from './auth.actions';

export const initialState: AuthState = {
  token: null,
  userId: null,
  isAuthenticated: false,
  loading: false,
  error: null,
};

export const authReducer = createReducer(
  initialState,
  on(login, (state) => ({ ...state, loading: true, error: null })),
  on(loginSuccess, (state, { response }) => ({
    ...state,
    token: response.token,
    userId: response.userId,
    isAuthenticated: true,
    loading: false,
    error: null,
  })),
  on(loginFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),
  on(logout, () => ({ ...initialState })),
  on(initAuthSuccess, (state, { response }) => ({
    ...state,
    token: response.token,
    userId: response.userId,
    isAuthenticated: true,
    loading: false,
    error: null,
  }))
);
