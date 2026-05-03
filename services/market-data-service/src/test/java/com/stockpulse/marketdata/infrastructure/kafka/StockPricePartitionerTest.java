package com.stockpulse.marketdata.infrastructure.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StockPricePartitionerTest {

    private StockPricePartitioner partitioner;

    @BeforeEach
    void setUp() {
        partitioner = new StockPricePartitioner();
    }

    @ParameterizedTest
    @ValueSource(strings = {"JPM", "GS", "BAC", "MS"})
    void usFinanceSymbols_routeToPartition0(String symbol) {
        assertThat(partitioner.resolvePartition(symbol)).isEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AAPL", "MSFT", "GOOGL", "AMZN", "META"})
    void usTechSymbols_routeToPartition1(String symbol) {
        assertThat(partitioner.resolvePartition(symbol)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BNP.PA", "SAN.PA", "ACA.PA"})
    void euFinanceSymbols_routeToPartition2(String symbol) {
        assertThat(partitioner.resolvePartition(symbol)).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"IBM", "TSLA", "NVDA", "NFLX"})
    void unknownSymbols_routeToPartition3or4(String symbol) {
        int partition = partitioner.resolvePartition(symbol);
        assertThat(partition).isBetween(3, 4);
    }

    @Test
    void unknownSymbols_overflowPartitionDeterministic() {
        // same symbol always maps to the same partition
        int first  = partitioner.resolvePartition("IBM");
        int second = partitioner.resolvePartition("IBM");
        assertThat(first).isEqualTo(second);
    }

    @Test
    void overflowFormula_matchesMathAbsHashModTwoPlusThree() {
        String symbol = "IBM";
        int expected = Math.abs(symbol.hashCode()) % 2 + 3;
        assertThat(partitioner.resolvePartition(symbol)).isEqualTo(expected);
    }

    @Test
    void noSegmentOverlap_techSymbolNotRoutedToFinance() {
        // AAPL is US Tech (1), must never land in Finance (0) or EU (2)
        assertThat(partitioner.resolvePartition("AAPL")).isNotEqualTo(0).isNotEqualTo(2);
    }
}
