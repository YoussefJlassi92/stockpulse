import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError, NEVER } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DashboardComponent } from './dashboard.component';
import { PortfolioService } from '../../core/services/portfolio.service';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { Portfolio, Position } from '../../core/models/portfolio.model';
import { AlertRule } from '../../core/models/alert.model';

const mockPortfolio: Portfolio = {
  id: 1, userId: 'user1', name: 'My Portfolio', description: '', createdAt: '', updatedAt: '',
};

const mockPosition: Position = {
  id: 1, portfolioId: 1, symbol: 'AAPL', quantity: 10,
  avgBuyPrice: 150, currentPrice: 200, lastUpdated: '',
};

const mockAlert: AlertRule = {
  id: 1, userId: 'user1', symbol: 'AAPL', thresholdPrice: 200,
  alertType: 'ABOVE', email: '', active: true, createdAt: '',
};

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;

  const portfolioServiceMock = {
    getPortfolios: vi.fn(),
    getPositions: vi.fn(),
  };
  const alertServiceMock = { getAlerts: vi.fn() };
  const authServiceMock = { getUserId: vi.fn(), getToken: vi.fn() };
  const wsServiceMock = {
    connect: vi.fn(),
    disconnect: vi.fn(),
    subscribeToPortfolioUpdates: vi.fn(),
    connected: signal(false),
  };

  beforeEach(async () => {
    portfolioServiceMock.getPortfolios.mockReturnValue(of([mockPortfolio]));
    portfolioServiceMock.getPositions.mockReturnValue(of([mockPosition]));
    alertServiceMock.getAlerts.mockReturnValue(of([mockAlert]));
    authServiceMock.getUserId.mockReturnValue('user1');
    authServiceMock.getToken.mockReturnValue('test-token');
    wsServiceMock.subscribeToPortfolioUpdates.mockReturnValue(NEVER);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule],
      providers: [
        { provide: PortfolioService, useValue: portfolioServiceMock },
        { provide: AlertService, useValue: alertServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: WebsocketService, useValue: wsServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.clearAllMocks());

  it('should call getPortfolios with userId on init', () => {
    expect(portfolioServiceMock.getPortfolios).toHaveBeenCalledWith('user1');
  });

  it('should call getPositions for each portfolio', () => {
    expect(portfolioServiceMock.getPositions).toHaveBeenCalledWith(1);
  });

  it('should call getAlerts with userId on init', () => {
    expect(alertServiceMock.getAlerts).toHaveBeenCalledWith('user1');
  });

  it('should set loading to false after data loads', () => {
    expect(component.loading()).toBe(false);
  });

  it('should connect to WebSocket after loading', () => {
    expect(wsServiceMock.connect).toHaveBeenCalledWith('test-token');
  });

  it('should subscribe to portfolio updates after loading', () => {
    expect(wsServiceMock.subscribeToPortfolioUpdates).toHaveBeenCalledWith(1);
  });

  it('should calculate totalValue correctly', () => {
    // quantity=10, currentPrice=200 → 2000
    expect(component.totalValue()).toBe(2000);
  });

  it('should calculate totalPnL correctly', () => {
    // pnl = 10 * (200 - 150) = 500
    expect(component.totalPnL()).toBe(500);
  });

  it('should count only active alerts', () => {
    component.alerts.set([
      { ...mockAlert, active: true },
      { ...mockAlert, id: 2, active: false },
    ]);
    expect(component.activeAlerts()).toBe(1);
  });

  it('should reflect wsConnected from service signal', () => {
    expect(component.wsConnected()).toBe(false);
  });

  it('should set error signal on service failure', async () => {
    portfolioServiceMock.getPortfolios.mockReturnValue(
      throwError(() => ({ message: 'Network error' }))
    );

    const errorFixture = TestBed.createComponent(DashboardComponent);
    errorFixture.detectChanges();

    expect(errorFixture.componentInstance.error()).toBe('Network error');
    expect(errorFixture.componentInstance.loading()).toBe(false);
  });

  it('should do nothing when userId is null', () => {
    vi.clearAllMocks();
    authServiceMock.getUserId.mockReturnValue(null);

    const nullFixture = TestBed.createComponent(DashboardComponent);
    nullFixture.detectChanges();

    expect(portfolioServiceMock.getPortfolios).not.toHaveBeenCalled();
  });
});
