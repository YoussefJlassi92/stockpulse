import { createAction, props } from '@ngrx/store';
import { LoginRequest, LoginResponse } from '../../models/auth.model';

export const login = createAction(
  '[Auth] Login',
  props<{ request: LoginRequest }>()
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ response: LoginResponse }>()
);

export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: string }>()
);

export const logout = createAction('[Auth] Logout');

export const initAuth = createAction('[Auth] Init');

export const initAuthSuccess = createAction(
  '[Auth] Init Success',
  props<{ response: LoginResponse }>()
);
