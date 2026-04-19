package com.stockpulse.marketdata.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StockPriceDto(
        String symbol,
        BigDecimal price,
        Long volume,
        BigDecimal changePct,
        OffsetDateTime fetchedAt,
        String source
) {}