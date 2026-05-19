import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AlertsComponent } from './alerts.component';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { AlertRule } from '../../core/models/alert.model';

const mockAlert: AlertRule = {
  id: 1, userId: 'user1', symbol: 'AAPL',
  thresholdPrice: 200, alertType: 'ABOVE',
  email: 'test@example.com', active: true, createdAt: '',
};

describe('AlertsComponent', () => {
  let fixture: ComponentFixture<AlertsComponent>;
  let component: AlertsComponent;

  const alertServiceMock = {
    getAlerts: vi.fn(),
    createAlert: vi.fn(),
    deleteAlert: vi.fn(),
  };
  const authServiceMock = { getUserId: vi.fn() };
  const snackBarMock = { open: vi.fn() };

  beforeEach(async () => {
    alertServiceMock.getAlerts.mockReturnValue(of([mockAlert]));
    alertServiceMock.createAlert.mockReturnValue(of(mockAlert));
    alertServiceMock.deleteAlert.mockReturnValue(of(undefined));
    authServiceMock.getUserId.mockReturnValue('user1');

    await TestBed.configureTestingModule({
      imports: [AlertsComponent, NoopAnimationsModule],
      providers: [
        { provide: AlertService, useValue: alertServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: MatSnackBar, useValue: snackBarMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AlertsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.clearAllMocks());

  it('should load alerts on init', () => {
    expect(alertServiceMock.getAlerts).toHaveBeenCalledWith('user1');
  });

  it('should populate alerts signal', () => {
    expect(component.alerts()).toHaveLength(1);
    expect(component.alerts()[0].symbol).toBe('AAPL');
  });

  it('should set loading to false after alerts load', () => {
    expect(component.loading()).toBe(false);
  });

  it('should return "active" class for active alert', () => {
    expect(component.getAlertStatusClass(mockAlert)).toBe('active');
  });

  it('should return "inactive" class for inactive alert', () => {
    expect(component.getAlertStatusClass({ ...mockAlert, active: false })).toBe('inactive');
  });

  it('should call createAlert with uppercased symbol and reload', () => {
    component.createForm.setValue({
      symbol: 'goog',
      thresholdPrice: 150,
      alertType: 'BELOW',
      email: 'test@example.com',
    });
    component.onCreateAlert();

    expect(alertServiceMock.createAlert).toHaveBeenCalledWith({
      userId: 'user1',
      symbol: 'GOOG',
      thresholdPrice: 150,
      alertType: 'BELOW',
      email: 'test@example.com',
    });
    expect(alertServiceMock.getAlerts).toHaveBeenCalledTimes(2);
    expect(snackBarMock.open).toHaveBeenCalledWith('Alert created successfully', 'Close', { duration: 3000 });
  });

  it('should not submit and mark touched when form is invalid', () => {
    component.onCreateAlert();
    expect(alertServiceMock.createAlert).not.toHaveBeenCalled();
    expect(component.createForm.get('symbol')?.touched).toBe(true);
  });

  it('should call deleteAlert and reload alerts', () => {
    component.onDeleteAlert(1);

    expect(alertServiceMock.deleteAlert).toHaveBeenCalledWith(1);
    expect(alertServiceMock.getAlerts).toHaveBeenCalledTimes(2);
    expect(snackBarMock.open).toHaveBeenCalledWith('Alert deleted', 'Close', { duration: 3000 });
  });

  it('should show create form when showCreateForm is toggled', () => {
    expect(component.showCreateForm()).toBe(false);
    component.showCreateForm.set(true);
    expect(component.showCreateForm()).toBe(true);
  });

  it('should reset form and hide it after successful creation', () => {
    component.showCreateForm.set(true);
    component.createForm.setValue({
      symbol: 'AAPL', thresholdPrice: 200, alertType: 'ABOVE', email: 'test@example.com',
    });
    component.onCreateAlert();

    expect(component.showCreateForm()).toBe(false);
    expect(component.createForm.get('symbol')?.value).toBeNull();
  });
});
