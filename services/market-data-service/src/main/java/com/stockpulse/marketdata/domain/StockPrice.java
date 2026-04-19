package com.stockpulse.marketdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stock_prices")
@IdClass(StockPriceId.class)
public record StockPrice(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id", nullable = false, updatable = false)
        Long id,

        @Column(name = "symbol", nullable = false, length = 10)
        String symbol,

        @Column(name = "price", nullable = false, precision = 15, scale = 4)
        BigDecimal price,

        @Column(name = "volume")
        Long volume,

        @Column(name = "change_pct", precision = 8, scale = 4)
        BigDecimal changePct,

        @Id
        @Column(name = "fetched_at", nullable = false, updatable = false)
        OffsetDateTime fetchedAt,

        @Column(name = "source", nullable = false, length = 50)
        String source

) {}
