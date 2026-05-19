import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PortfolioComponent } from './portfolio.component';
import { PortfolioService } from '../../core/services/portfolio.service';
import { AuthService } from '../../core/services/auth.service';
import { Portfolio, Position } from '../../core/models/portfolio.model';

const mockPortfolio: Portfolio = {
  id: 1, userId: 'user1', name: 'My Portfolio',
  description: '', createdAt: '', updatedAt: '',
};

const mockPosition: Position = {
  id: 1, portfolioId: 1, symbol: 'AAPL',
  quantity: 10, avgBuyPrice: 150, currentPrice: 200, lastUpdated: '',
};

describe('PortfolioComponent', () => {
  let fixture: ComponentFixture<PortfolioComponent>;
  let component: PortfolioComponent;

  const portfolioServiceMock = {
    getPortfolios: vi.fn(),
    getPositions: vi.fn(),
    addPosition: vi.fn(),
  };
  const authServiceMock = { getUserId: vi.fn() };

  beforeEach(async () => {
    portfolioServiceMock.getPortfolios.mockReturnValue(of([mockPortfolio]));
    portfolioServiceMock.getPositions.mockReturnValue(of([mockPosition]));
    portfolioServiceMock.addPosition.mockReturnValue(of(mockPosition));
    authServiceMock.getUserId.mockReturnValue('user1');

    await TestBed.configureTestingModule({
      imports: [PortfolioComponent, NoopAnimationsModule],
      providers: [
        { provide: PortfolioService, useValue: portfolioServiceMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PortfolioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.clearAllMocks());

  it('should load portfolios on init', () => {
    expect(portfolioServiceMock.getPortfolios).toHaveBeenCalledWith('user1');
  });

  it('should auto-select the first portfolio and load its positions', () => {
    expect(component.selectedPortfolioId()).toBe(1);
    expect(portfolioServiceMock.getPositions).toHaveBeenCalledWith(1);
  });

  it('should set loading to false after positions load', () => {
    expect(component.loading()).toBe(false);
  });

  it('should populate positions signal', () => {
    expect(component.positions()).toHaveLength(1);
    expect(component.positions()[0].symbol).toBe('AAPL');
  });

  it('should calculate P&L correctly', () => {
    // (200 - 150) * 10 = 500
    expect(component.calculatePnL(mockPosition)).toBe(500);
  });

  it('should calculate P&L percent correctly', () => {
    // ((200 - 150) / 150) * 100 ≈ 33.33
    expect(component.calculatePnLPercent(mockPosition)).toBeCloseTo(33.33, 1);
  });

  it('should calculate totalValue from positions', () => {
    // 10 * 200 = 2000
    expect(component.totalValue()).toBe(2000);
  });

  it('should calculate totalPnL from positions', () => {
    expect(component.totalPnL()).toBe(500);
  });

  it('should call addPosition with uppercased symbol and reload positions', () => {
    component.addForm.setValue({ symbol: 'goog', quantity: 5, price: 100 });
    component.onAddPosition();

    expect(portfolioServiceMock.addPosition).toHaveBeenCalledWith(1, {
      symbol: 'GOOG',
      quantity: 5,
      price: 100,
    });
    expect(portfolioServiceMock.getPositions).toHaveBeenCalledTimes(2);
  });

  it('should not submit and mark touched when form is invalid', () => {
    component.onAddPosition();
    expect(portfolioServiceMock.addPosition).not.toHaveBeenCalled();
    expect(component.addForm.get('symbol')?.touched).toBe(true);
  });

  it('should reload positions on portfolio change', () => {
    vi.clearAllMocks();
    portfolioServiceMock.getPositions.mockReturnValue(of([]));
    component.onPortfolioChange(1);
    expect(portfolioServiceMock.getPositions).toHaveBeenCalledWith(1);
  });
});
