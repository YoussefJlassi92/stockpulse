package com.stockpulse.marketdata.domain;

import java.io.Serializable;
import java.time.OffsetDateTime;

public record StockPriceId(Long id, OffsetDateTime fetchedAt) implements Serializable {}
