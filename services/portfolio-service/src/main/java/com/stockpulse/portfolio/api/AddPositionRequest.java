package com.stockpulse.portfolio.api;

import java.math.BigDecimal;

public record AddPositionRequest(
        String symbol,
        BigDecimal quantity,
        BigDecimal price
) {}
