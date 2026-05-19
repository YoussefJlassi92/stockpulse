import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { PortfolioService } from './portfolio.service';
import { Portfolio, Position, AddPositionRequest } from '../models/portfolio.model';
import { PORTFOLIOS } from './api.config';

describe('PortfolioService', () => {
  let service: PortfolioService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PortfolioService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PortfolioService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should send GET to correct URL for getPortfolios', () => {
    const mockPortfolios: Portfolio[] = [
      {
        id: 1,
        userId: 'user1',
        name: 'My Portfolio',
        description: '',
        createdAt: '2024-01-01',
        updatedAt: '2024-01-01',
      },
    ];

    service.getPortfolios('user1').subscribe((portfolios) => {
      expect(portfolios).toEqual(mockPortfolios);
    });

    const req = httpMock.expectOne(`${PORTFOLIOS}/user/user1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPortfolios);
  });

  it('should send POST with correct body for addPosition', () => {
    const request: AddPositionRequest = { symbol: 'AAPL', quantity: 10, price: 150 };
    const mockPosition: Position = {
      id: 1,
      portfolioId: 42,
      symbol: 'AAPL',
      quantity: 10,
      avgBuyPrice: 150,
      currentPrice: 155,
      lastUpdated: '2024-01-01',
    };

    service.addPosition(42, request).subscribe((position) => {
      expect(position).toEqual(mockPosition);
    });

    const req = httpMock.expectOne(`${PORTFOLIOS}/42/positions`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockPosition);
  });
});
