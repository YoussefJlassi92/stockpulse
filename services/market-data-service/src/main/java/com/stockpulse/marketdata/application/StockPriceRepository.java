package com.stockpulse.marketdata.application;

import com.stockpulse.marketdata.domain.StockPrice;
import com.stockpulse.marketdata.domain.StockPriceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPriceRepository extends JpaRepository<StockPrice, StockPriceId> {}
