package com.stockpulse.marketdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * JPA entity mapping the {@code stock_prices} TimescaleDB hypertable.
 * Uses a composite primary key ({@code id} + {@code fetched_at}) via {@link StockPriceId}.
 *
 * <p>Intentionally NOT a record: JPA requires a no-arg constructor and
 * hibernate-commons-annotations 7.x has a known bug with records as entities.
 * {@link StockPriceDto} is the immutable record used outside the persistence layer.
 */
@Entity
@Table(name = "stock_prices")
@IdClass(StockPriceId.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StockPrice {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 10)
    private String symbol;

    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "change_pct", precision = 8, scale = 4)
    private BigDecimal changePct;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "fetched_at", nullable = false, updatable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "source", nullable = false, length = 50)
    private String source;
}
