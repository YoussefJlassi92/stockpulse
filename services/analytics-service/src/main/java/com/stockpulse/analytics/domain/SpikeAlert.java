package com.stockpulse.analytics.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SpikeAlert(
        String symbol,
        BigDecimal previousPrice,
        BigDecimal currentPrice,
        BigDecimal changePercent,
        OffsetDateTime detectedAt
) {}
