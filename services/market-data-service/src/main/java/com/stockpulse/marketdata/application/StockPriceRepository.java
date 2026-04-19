package com.stockpulse.marketdata.application;

import com.stockpulse.marketdata.domain.StockPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {}
