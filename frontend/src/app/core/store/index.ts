import { AuthState } from '../models/auth.model';

export interface AppState {
  auth: AuthState;
}

export * from './auth/auth.actions';
export * from './auth/auth.reducer';
export * from './auth/auth.selectors';
export * from './auth/auth.effects';
export * from './auth/auth.state';
