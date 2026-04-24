package com.stockpulse.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private Long portfolioId;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal avgBuyPrice;

    @Column(precision = 18, scale = 4)
    private BigDecimal currentPrice;

    private OffsetDateTime lastUpdated;

    public BigDecimal calculatePnL() {
        if (currentPrice == null) return BigDecimal.ZERO;
        return currentPrice.subtract(avgBuyPrice).multiply(quantity);
    }

    public BigDecimal calculatePnLPercent() {
        if (currentPrice == null || avgBuyPrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return currentPrice.subtract(avgBuyPrice)
                .divide(avgBuyPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
