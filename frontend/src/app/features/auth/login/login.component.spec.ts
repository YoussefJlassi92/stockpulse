import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { LoginComponent } from './login.component';
import { login } from '../../../core/store/auth/auth.actions';
import { selectAuthLoading, selectAuthError } from '../../../core/store/auth/auth.selectors';
import { AuthState } from '../../../core/models/auth.model';

const initialAuthState: AuthState = {
  token: null,
  userId: null,
  isAuthenticated: false,
  loading: false,
  error: null,
};

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, NoopAnimationsModule],
      providers: [
        provideMockStore({ initialState: { auth: initialAuthState } }),
      ],
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => store.resetSelectors());

  it('should mark form invalid when fields are empty', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('should mark userId field required when empty and touched', () => {
    const ctrl = component.form.get('userId')!;
    ctrl.markAsTouched();
    expect(ctrl.hasError('required')).toBe(true);
  });

  it('should mark password field invalid when shorter than 3 characters', () => {
    component.form.get('password')!.setValue('ab');
    expect(component.form.get('password')!.hasError('minlength')).toBe(true);
  });

  it('should dispatch login action when form is valid', () => {
    const dispatchSpy = vi.spyOn(store, 'dispatch');
    component.form.setValue({ userId: 'user1', password: 'pass123' });
    component.onSubmit();
    expect(dispatchSpy).toHaveBeenCalledWith(
      login({ request: { userId: 'user1', password: 'pass123' } })
    );
  });

  it('should not dispatch when form is invalid', () => {
    const dispatchSpy = vi.spyOn(store, 'dispatch');
    component.onSubmit();
    expect(dispatchSpy).not.toHaveBeenCalled();
  });

  it('should show spinner when loading is true', () => {
    store.overrideSelector(selectAuthLoading, true);
    store.refreshState();
    fixture.detectChanges();
    const spinner = fixture.debugElement.query(By.css('mat-spinner'));
    expect(spinner).not.toBeNull();
  });

  it('should hide spinner when loading is false', () => {
    store.overrideSelector(selectAuthLoading, false);
    store.refreshState();
    fixture.detectChanges();
    const spinner = fixture.debugElement.query(By.css('mat-spinner'));
    expect(spinner).toBeNull();
  });

  it('should display error message when error exists', () => {
    store.overrideSelector(selectAuthError, 'Invalid credentials');
    store.refreshState();
    fixture.detectChanges();
    const errorEl = fixture.debugElement.query(By.css('.error-banner'));
    expect(errorEl).not.toBeNull();
    expect(errorEl.nativeElement.textContent).toContain('Invalid credentials');
  });

  it('should not display error banner when error is null', () => {
    const errorEl = fixture.debugElement.query(By.css('.error-banner'));
    expect(errorEl).toBeNull();
  });
});
