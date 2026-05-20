import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { PortfolioComponent } from './portfolio.component';
import { PortfolioService } from '../../core/services/portfolio.service';
import { AuthService } from '../../core/services/auth.service';
import { Portfolio, Position } from '../../core/models/portfolio.model';
import { PORTFOLIOS } from '../../core/services/api.config';

const PORTFOLIOS_URL = `${PORTFOLIOS}/user/user1`;
const POSITIONS_URL = `${PORTFOLIOS}/1/positions`;

const mockPortfolio: Portfolio = {
  id: 1, userId: 'user1', name: 'My Portfolio',
  description: '', createdAt: '', updatedAt: '',
};

const mockPosition: Position = {
  id: 1, portfolioId: 1, symbol: 'AAPL',
  quantity: 10, avgBuyPrice: 150, currentPrice: 200, lastUpdated: '',
};

/** Flush httpResource Promise and run effects. */
async function flushResources(fixture: ComponentFixture<unknown>): Promise<void> {
  await Promise.resolve();
  fixture.detectChanges();
}

describe('PortfolioComponent', () => {
  let fixture: ComponentFixture<PortfolioComponent>;
  let component: PortfolioComponent;
  let httpMock: HttpTestingController;

  // Only addPosition remains as a direct service call; GETs go through httpResource
  const portfolioServiceMock = { addPosition: vi.fn() };
  const authServiceMock = { getUserId: vi.fn() };

  beforeEach(async () => {
    authServiceMock.getUserId.mockReturnValue('user1');
    portfolioServiceMock.addPosition.mockReturnValue(of(mockPosition));

    await TestBed.configureTestingModule({
      imports: [PortfolioComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PortfolioService, useValue: portfolioServiceMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PortfolioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // 1. Flush portfolios → httpResource Promise settles → effect auto-selects portfolio 1
    httpMock.expectOne(PORTFOLIOS_URL).flush([mockPortfolio]);
    await flushResources(fixture);

    // 2. Auto-select effect set selectedPortfolioId(1) → positionsResource fires
    httpMock.expectOne(POSITIONS_URL).flush([mockPosition]);
    await flushResources(fixture);
  });

  afterEach(() => {
    httpMock.verify();
    vi.clearAllMocks();
  });

  it('should load portfolios via httpResource on init', () => {
    expect(component.portfolios()).toHaveLength(1);
    expect(component.portfolios()[0].name).toBe('My Portfolio');
  });

  it('should auto-select the first portfolio', () => {
    expect(component.selectedPortfolioId()).toBe(1);
  });

  it('should load positions for the selected portfolio', () => {
    expect(component.positions()).toHaveLength(1);
    expect(component.positions()[0].symbol).toBe('AAPL');
  });

  it('should set loading to false after all resources load', () => {
    expect(component.loading()).toBe(false);
  });

  it('should calculate P&L correctly', () => {
    expect(component.calculatePnL(mockPosition)).toBe(500);
  });

  it('should calculate P&L percent correctly', () => {
    expect(component.calculatePnLPercent(mockPosition)).toBeCloseTo(33.33, 1);
  });

  it('should calculate totalValue from positions', () => {
    expect(component.totalValue()).toBe(2000);
  });

  it('should calculate totalPnL from positions', () => {
    expect(component.totalPnL()).toBe(500);
  });

  it('should call addPosition with uppercased symbol and reload positions', async () => {
    component.addForm.setValue({ symbol: 'goog', quantity: 5, price: 100 });
    component.onAddPosition();

    expect(portfolioServiceMock.addPosition).toHaveBeenCalledWith(1, {
      symbol: 'GOOG', quantity: 5, price: 100,
    });

    // positionsResource.reload() schedules a new GET on the next effect cycle
    fixture.detectChanges();
    await Promise.resolve();
    httpMock.expectOne(POSITIONS_URL).flush([mockPosition]);
    await flushResources(fixture);
  });

  it('should not submit and mark touched when form is invalid', () => {
    component.onAddPosition();
    expect(portfolioServiceMock.addPosition).not.toHaveBeenCalled();
    expect(component.addForm.get('symbol')?.touched).toBe(true);
  });

  it('should reload positions on portfolio change', async () => {
    // Switch to portfolio 2 (different from the already-selected 1)
    component.onPortfolioChange(2);
    expect(component.selectedPortfolioId()).toBe(2);

    // selectedPortfolioId changed → positionsResource params update → new GET
    fixture.detectChanges();
    await Promise.resolve();
    httpMock.expectOne(`${PORTFOLIOS}/2/positions`).flush([]);
    await flushResources(fixture);
  });
});
