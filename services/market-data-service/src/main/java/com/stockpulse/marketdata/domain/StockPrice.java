package com.stockpulse.marketdata.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * JPA entity mapping the {@code stock_prices} TimescaleDB hypertable.
 * The composite PK (id + fetched_at) is defined in the Flyway migration;
 * JPA only maps the BIGSERIAL id column to avoid Hibernate 6.x limitations
 * with @GeneratedValue on composite keys.
 *
 * <p>Intentionally NOT a record: JPA requires a no-arg constructor and
 * hibernate-commons-annotations 7.x has a known bug with records as entities.
 * {@link StockPriceDto} is the immutable record used outside the persistence layer.
 */
@Entity
@Table(name = "stock_prices")
@Getter
@Setter
@NoArgsConstructor
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

    @Column(name = "fetched_at", nullable = false, updatable = false)
    private OffsetDateTime fetchedAt;

    @Column(name = "source", nullable = false, length = 50)
    private String source;
}
