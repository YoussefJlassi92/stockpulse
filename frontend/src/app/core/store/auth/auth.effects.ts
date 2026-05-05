import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Router } from '@angular/router';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { login, loginSuccess, loginFailure, logout } from './auth.actions';

@Injectable()
export class AuthEffects {
  private readonly actions$ = inject(Actions);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
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
