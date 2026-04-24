package com.stockpulse.portfolio.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StockPriceEvent(
        String symbol,
        BigDecimal price,
        Long volume,
        BigDecimal changePct,
        OffsetDateTime fetchedAt,
        String source
) {}
