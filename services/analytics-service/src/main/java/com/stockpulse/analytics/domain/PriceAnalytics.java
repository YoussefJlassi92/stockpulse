package com.stockpulse.analytics.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceAnalytics(
        String symbol,
        OffsetDateTime windowStart,
        OffsetDateTime windowEnd,
        BigDecimal avgPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        long messageCount,
        BigDecimal priceChangePercent
) {}
