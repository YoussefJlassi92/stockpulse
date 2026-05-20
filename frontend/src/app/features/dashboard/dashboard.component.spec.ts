import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, NEVER } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { DashboardComponent } from './dashboard.component';
import { PortfolioService } from '../../core/services/portfolio.service';
import { AuthService } from '../../core/services/auth.service';
import { WebsocketService } from '../../core/services/websocket.service';
import { Portfolio, Position } from '../../core/models/portfolio.model';
import { AlertRule } from '../../core/models/alert.model';
import { PORTFOLIOS, ALERTS } from '../../core/services/api.config';

const PORTFOLIOS_URL = `${PORTFOLIOS}/user/user1`;
const ALERTS_URL = `${ALERTS}/user1`;

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

/**
 * Dashboard uses both httpResource (portfolios, alerts) and rxResource (positions).
 * httpResource settles after one microtask; rxResource needs an extra round
 * (effect fires → getPositions mock emits → rxResource Promise resolves).
 */
async function flushDashboard(
  httpMock: HttpTestingController,
  fixture: ComponentFixture<DashboardComponent>
): Promise<void> {
  // Flush both HTTP requests (order doesn't matter with HttpTestingController)
  httpMock.expectOne(PORTFOLIOS_URL).flush([mockPortfolio]);
  httpMock.expectOne(ALERTS_URL).flush([mockAlert]);

  // Round 1: httpResource Promises settle → portfoliosResource.value() signal updates
  await Promise.resolve();
  fixture.detectChanges(); // rxResource params() changes → stream fires (getPositions)

  // Round 2: rxResource Promise settles → positionsResource.value() signal updates
  await Promise.resolve();
  fixture.detectChanges(); // WS effect fires, computed signals update
}

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  let httpMock: HttpTestingController;

  // PortfolioService.getPositions is called inside rxResource's stream
  const portfolioServiceMock = { getPositions: vi.fn() };
  const authServiceMock = { getUserId: vi.fn(), getToken: vi.fn() };
  const wsServiceMock = {
    connect: vi.fn(),
    disconnect: vi.fn(),
    subscribeToPortfolioUpdates: vi.fn(),
    connected: signal(false),
  };

  beforeEach(async () => {
    portfolioServiceMock.getPositions.mockReturnValue(of([mockPosition]));
    authServiceMock.getUserId.mockReturnValue('user1');
    authServiceMock.getToken.mockReturnValue('test-token');
    wsServiceMock.subscribeToPortfolioUpdates.mockReturnValue(NEVER);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PortfolioService, useValue: portfolioServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: WebsocketService, useValue: wsServiceMock },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    await flushDashboard(httpMock, fixture);
  });

  afterEach(() => {
    httpMock.verify();
    vi.clearAllMocks();
  });

  it('should load portfolios via httpResource', () => {
    expect(component.portfoliosResource.value()).toEqual([mockPortfolio]);
  });

  it('should load alerts via httpResource', () => {
    expect(component.alertsResource.value()).toEqual([mockAlert]);
  });

  it('should call getPositions for each portfolio via rxResource', () => {
    expect(portfolioServiceMock.getPositions).toHaveBeenCalledWith(1);
  });

  it('should set loading to false after all resources load', () => {
    expect(component.loading()).toBe(false);
  });

  it('should connect to WebSocket after portfolios load', () => {
    expect(wsServiceMock.connect).toHaveBeenCalledWith('test-token');
  });

  it('should subscribe to portfolio updates', () => {
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
    expect(component.activeAlerts()).toBe(1);
  });

  it('should reflect wsConnected from service signal', () => {
    expect(component.wsConnected()).toBe(false);
  });

  it('should update positions when WebSocket override arrives', () => {
    expect(component.positions()[0].currentPrice).toBe(200);

    // Simulate a live price update via wsOverrides
    component['wsOverrides'].update((m) => {
      const next = new Map(m);
      next.set(1, 250);
      return next;
    });

    expect(component.positions()[0].currentPrice).toBe(250);
    expect(component.positions()[0].pnl).toBe(10 * (250 - 150)); // 1000
  });

  it('should not make HTTP requests when userId is null', () => {
    vi.clearAllMocks();
    authServiceMock.getUserId.mockReturnValue(null);

    const nullFixture = TestBed.createComponent(DashboardComponent);
    nullFixture.detectChanges();

    httpMock.expectNone(PORTFOLIOS_URL);
    httpMock.expectNone(ALERTS_URL);
  });
});
