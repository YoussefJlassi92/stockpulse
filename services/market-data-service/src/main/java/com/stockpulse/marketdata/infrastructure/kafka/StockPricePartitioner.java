package com.stockpulse.marketdata.infrastructure.kafka;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;
import java.util.Set;

/**
 * Custom Kafka partitioner that routes stock price events to dedicated partitions
 * based on the symbol's market segment.
 *
 * <p>Partition layout:
 * <ul>
 *   <li><b>Partition 0</b> — US Finance (JPM, GS, BAC, MS): groups the major US
 *       investment banks and financial institutions together so consumers processing
 *       financial-sector alerts can subscribe to a single partition.</li>
 *   <li><b>Partition 1</b> — US Tech (AAPL, MSFT, GOOGL, AMZN, META): high-volume
 *       symbols that benefit from isolation to avoid starving other segments.</li>
 *   <li><b>Partition 2</b> — EU Finance (BNP.PA, SAN.PA, ACA.PA): European
 *       exchange-listed financial stocks, segregated to simplify region-aware
 *       consumers.</li>
 *   <li><b>Partition 3 or 4</b> — Default: any symbol not matched above is hashed
 *       via {@code Math.abs(symbol.hashCode()) % 2 + 3}, distributing unknown
 *       symbols evenly across the two overflow partitions.</li>
 * </ul>
 *
 * <p>The topic must be created with at least 5 partitions for this strategy to work
 * correctly. See {@link KafkaTopicConfig} for the topic definition.
 */
public class StockPricePartitioner implements Partitioner {

    private static final int PARTITION_US_FINANCE = 0;
    private static final int PARTITION_US_TECH    = 1;
    private static final int PARTITION_EU_FINANCE = 2;
    private static final int OVERFLOW_BASE        = 3;
    private static final int OVERFLOW_COUNT       = 2;

    private static final Set<String> US_FINANCE = Set.of("JPM", "GS", "BAC", "MS");
    private static final Set<String> US_TECH    = Set.of("AAPL", "MSFT", "GOOGL", "AMZN", "META");
    private static final Set<String> EU_FINANCE = Set.of("BNP.PA", "SAN.PA", "ACA.PA");

    @Override
    public int partition(String topic, Object key, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {
        String symbol = key instanceof String s ? s : String.valueOf(key);
        return resolvePartition(symbol);
    }

    int resolvePartition(String symbol) {
        if (US_FINANCE.contains(symbol)) return PARTITION_US_FINANCE;
        if (US_TECH.contains(symbol))    return PARTITION_US_TECH;
        if (EU_FINANCE.contains(symbol)) return PARTITION_EU_FINANCE;
        return Math.abs(symbol.hashCode()) % OVERFLOW_COUNT + OVERFLOW_BASE;
    }

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
