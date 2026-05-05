export interface Portfolio {
  id: number;
  userId: string;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface Position {
  id: number;
  portfolioId: number;
  symbol: string;
  quantity: number;
  avgBuyPrice: number;
  currentPrice: number;
  lastUpdated: string;
}

export interface PortfolioSummary {
  portfolioId: number;
  userId: string;
  name: string;
  totalValue: number;
  totalPnL: number;
  totalPnLPercent: number;
  positions: Position[];
}

export interface AddPositionRequest {
  symbol: string;
  quantity: number;
  price: number;
}
