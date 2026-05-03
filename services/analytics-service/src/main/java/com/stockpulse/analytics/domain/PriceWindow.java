package com.stockpulse.analytics.domain;

import java.math.BigDecimal;

/**
 * Mutable accumulator for a single tumbling-window aggregation over stock prices.
 * Package-private — consumed only by the Kafka Streams topology and its Serde.
 */
public record PriceWindow(
        long count,
        BigDecimal sumPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal firstPrice,
        BigDecimal lastPrice
) {
    public static PriceWindow empty() {
        return new PriceWindow(0L, BigDecimal.ZERO, null, null, null, null);
    }

    public PriceWindow accumulate(BigDecimal price) {
        return new PriceWindow(
                count + 1,
                sumPrice.add(price),
                minPrice == null ? price : price.min(minPrice),
                maxPrice == null ? price : price.max(maxPrice),
                firstPrice == null ? price : firstPrice,
                price
        );
    }
}
