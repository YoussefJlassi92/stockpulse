export interface StockPrice {
  symbol: string;
  price: number;
  changePct: number;
  volume: number;
  fetchedAt: string;
}

export interface PriceAnalytics {
  symbol: string;
  windowStart: string;
  windowEnd: string;
  avgPrice: number;
  minPrice: number;
  maxPrice: number;
  messageCount: number;
  priceChangePercent: number;
}

export interface SpikeAlert {
  symbol: string;
  previousPrice: number;
  currentPrice: number;
  changePercent: number;
  detectedAt: string;
}
