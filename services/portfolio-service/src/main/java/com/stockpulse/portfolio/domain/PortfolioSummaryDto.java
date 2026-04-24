package com.stockpulse.portfolio.domain;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSummaryDto(
        Long portfolioId,
        String userId,
        String name,
        BigDecimal totalValue,
        BigDecimal totalPnL,
        BigDecimal totalPnLPercent,
        List<PositionDto> positions
) {}
