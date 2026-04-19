package com.stockpulse.marketdata.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Composite primary key class for {@link StockPrice}.
 * Must be a plain class (not a record): JPA requires a no-arg constructor,
 * and hibernate-commons-annotations 7.x has a known bug with records as @IdClass.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockPriceId implements Serializable {

    private Long id;
    private OffsetDateTime fetchedAt;
}
