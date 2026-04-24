package com.stockpulse.portfolio.domain;

import java.math.BigDecimal;

public record PositionDto(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice,
        BigDecimal pnl,
        BigDecimal pnlPercent
) {}
