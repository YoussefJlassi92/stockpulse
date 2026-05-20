import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Router } from '@angular/router';
import { catchError, map, of, switchMap, tap, EMPTY } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { login, loginSuccess, loginFailure, logout, initAuth, initAuthSuccess } from './auth.actions';

function isTokenValid(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 > Date.now();
  } catch {
    return false;
  }
}

@Injectable()
export class AuthEffects {
  private readonly actions$ = inject(Actions);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly initAuth$ = createEffect(() =>
    this.actions$.pipe(
      ofType(initAuth),
      switchMap(() => {
        const token = this.authService.getToken();
        const userId = this.authService.getUserId();
        if (token && userId && isTokenValid(token)) {
          return of(initAuthSuccess({ response: { token, userId, expiresIn: 0 } }));
        }
        return EMPTY;
      })
    )
  );

  readonly loginEffect$ = createEffect(() =>
    this.actions$.pipe(
      ofType(login),
      switchMap(({ request }) =>
        this.authService.login(request).pipe(
          map((response) => loginSuccess({ response })),
          catchError((err: { message?: string }) =>
            of(loginFailure({ error: err.message ?? 'Login failed' }))
          )
        )
      )
    )
  );

  readonly loginSuccessEffect$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(loginSuccess),
        tap(() => this.router.navigate(['/dashboard']))
      ),
    { dispatch: false }
  );

  readonly logoutEffect$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(logout),
        tap(() => {
          this.authService.logout();
          this.router.navigate(['/login']);
        })
      ),
    { dispatch: false }
  );
}
