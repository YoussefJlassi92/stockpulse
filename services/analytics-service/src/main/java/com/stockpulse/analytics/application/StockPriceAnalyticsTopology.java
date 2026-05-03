package com.stockpulse.analytics.application;

import com.stockpulse.analytics.domain.PriceAnalytics;
import com.stockpulse.analytics.domain.PriceWindow;
import com.stockpulse.analytics.domain.SpikeAlert;
import com.stockpulse.analytics.domain.StockPriceEvent;
import com.stockpulse.analytics.infrastructure.serde.PriceAnalyticsSerde;
import com.stockpulse.analytics.infrastructure.serde.PriceWindowSerde;
import com.stockpulse.analytics.infrastructure.serde.SpikeAlertSerde;
import com.stockpulse.analytics.infrastructure.serde.StockPriceEventSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Kafka Streams topology for real-time stock price analytics.
 *
 * <p>Both processing pipelines share a single source stream built from the
 * {@code stock.prices} topic, which avoids double-consuming the same topic.
 */
@Configuration
public class StockPriceAnalyticsTopology {

    private static final Logger log = LoggerFactory.getLogger(StockPriceAnalyticsTopology.class);

    static final String LAST_PRICE_STORE = "last-price-store";
    static final String PRICE_ANALYTICS_STORE = "price-analytics-store";

    private final String stockPricesTopic;
    private final String analyticsOutputTopic;
    private final String spikeAlertsTopic;
    private final Duration windowSize;
    private final BigDecimal spikeThreshold;

    public StockPriceAnalyticsTopology(
            @Value("${app.kafka.topics.stock-prices}") String stockPricesTopic,
            @Value("${app.kafka.topics.analytics-output}") String analyticsOutputTopic,
            @Value("${app.kafka.topics.spike-alerts}") String spikeAlertsTopic,
            @Value("${app.streams.window-size-minutes}") int windowSizeMinutes,
            @Value("${app.streams.spike-threshold-percent}") double spikeThresholdPercent) {
        this.stockPricesTopic = stockPricesTopic;
        this.analyticsOutputTopic = analyticsOutputTopic;
        this.spikeAlertsTopic = spikeAlertsTopic;
        this.windowSize = Duration.ofMinutes(windowSizeMinutes);
        this.spikeThreshold = BigDecimal.valueOf(spikeThresholdPercent);
    }

    @Bean
    public KStream<String, StockPriceEvent> stockPricesStream(StreamsBuilder builder) {
        KStream<String, StockPriceEvent> stream = builder.stream(
                stockPricesTopic,
                Consumed.with(Serdes.String(), new StockPriceEventSerde()));

        buildPriceAnalyticsPipeline(stream);
        buildSpikeDetectionPipeline(builder, stream);

        return stream;
    }

    /**
     * Pipeline 1 — 5-minute tumbling-window price aggregation.
     *
     * <h2>Tumbling vs hopping windows</h2>
     * <ul>
     *   <li><b>Tumbling</b> (this pipeline): non-overlapping fixed-size windows. Each
     *       event belongs to exactly one window. Ideal for periodic summaries where you
     *       want a clean cut every N minutes without double-counting.</li>
     *   <li><b>Hopping</b>: overlapping windows defined by a size and an advance
     *       interval. Each event can appear in multiple windows, making them useful for
     *       rolling averages but at the cost of higher output volume and more complex
     *       downstream deduplication.</li>
     * </ul>
     *
     * <p>{@link TimeWindows#ofSizeWithNoGrace} sets the grace period to zero, meaning
     * late-arriving records are discarded. Combined with
     * {@link Suppressed#untilWindowCloses}, this ensures exactly one output record per
     * window per symbol — the final aggregate — rather than an intermediate update for
     * every incoming record.
     */
    private void buildPriceAnalyticsPipeline(KStream<String, StockPriceEvent> stream) {
        stream
                .groupByKey(Grouped.with(Serdes.String(), new StockPriceEventSerde()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(windowSize))
                .aggregate(
                        PriceWindow::empty,
                        (symbol, event, window) -> window.accumulate(event.price()),
                        Materialized
                                .<String, PriceWindow, WindowStore<Bytes, byte[]>>as(PRICE_ANALYTICS_STORE)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(new PriceWindowSerde()))
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((windowedKey, window) -> KeyValue.pair(
                        windowedKey.key(),
                        toPriceAnalytics(windowedKey, window)))
                .to(analyticsOutputTopic, Produced.with(Serdes.String(), new PriceAnalyticsSerde()));
    }

    /**
     * Pipeline 2 — Price spike detection.
     *
     * <h2>Detection logic</h2>
     * <p>A persistent {@code KeyValueStore} ("last-price-store") holds the most recent
     * price for every symbol. For each incoming event the processor:
     * <ol>
     *   <li>Reads the previous price from the store.</li>
     *   <li>Computes {@code changePercent = (current − previous) / previous × 100}.</li>
     *   <li>If {@code |changePercent| ≥ spikeThreshold} (default 3 %), forwards a
     *       {@link SpikeAlert} downstream.</li>
     *   <li>Writes the current price to the store — regardless of whether a spike
     *       was detected — so the next event compares against the latest price.</li>
     * </ol>
     *
     * <p>The first event for a given symbol never triggers a spike because there is no
     * previous price to compare against.
     */
    private void buildSpikeDetectionPipeline(StreamsBuilder builder,
                                             KStream<String, StockPriceEvent> stream) {
        builder.addStateStore(
                Stores.keyValueStoreBuilder(
                        Stores.inMemoryKeyValueStore(LAST_PRICE_STORE),
                        Serdes.String(),
                        Serdes.String()));

        stream
                .process(this::createSpikeDetector, Named.as("spike-detector"), LAST_PRICE_STORE)
                .to(spikeAlertsTopic, Produced.with(Serdes.String(), new SpikeAlertSerde()));
    }

    private Processor<String, StockPriceEvent, String, SpikeAlert> createSpikeDetector() {
        return new Processor<>() {
            private ProcessorContext<String, SpikeAlert> context;
            private KeyValueStore<String, String> store;

            @Override
            public void init(ProcessorContext<String, SpikeAlert> context) {
                this.context = context;
                this.store = context.getStateStore(LAST_PRICE_STORE);
            }

            @Override
            public void process(Record<String, StockPriceEvent> record) {
                String symbol = record.key();
                BigDecimal currentPrice = record.value().price();
                String prevStr = store.get(symbol);

                if (prevStr != null) {
                    BigDecimal previousPrice = new BigDecimal(prevStr);
                    if (previousPrice.compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal changePercent = currentPrice.subtract(previousPrice)
                                .divide(previousPrice, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));

                        if (changePercent.abs().compareTo(spikeThreshold) >= 0) {
                            log.info("Spike detected: symbol={} prev={} current={} change={}%",
                                    symbol, previousPrice, currentPrice, changePercent);
                            SpikeAlert alert = new SpikeAlert(
                                    symbol, previousPrice, currentPrice, changePercent,
                                    Instant.ofEpochMilli(record.timestamp()).atOffset(ZoneOffset.UTC));
                            context.forward(record.withValue(alert));
                        }
                    }
                }
                store.put(symbol, currentPrice.toString());
            }
        };
    }

    private PriceAnalytics toPriceAnalytics(Windowed<String> windowedKey, PriceWindow window) {
        BigDecimal avgPrice = window.count() == 0
                ? BigDecimal.ZERO
                : window.sumPrice().divide(BigDecimal.valueOf(window.count()), 4, RoundingMode.HALF_UP);

        BigDecimal priceChangePercent = (window.firstPrice() == null
                || window.firstPrice().compareTo(BigDecimal.ZERO) == 0)
                ? BigDecimal.ZERO
                : window.lastPrice().subtract(window.firstPrice())
                        .divide(window.firstPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

        return new PriceAnalytics(
                windowedKey.key(),
                windowedKey.window().startTime().atOffset(ZoneOffset.UTC),
                windowedKey.window().endTime().atOffset(ZoneOffset.UTC),
                avgPrice,
                window.minPrice(),
                window.maxPrice(),
                window.count(),
                priceChangePercent);
    }
}
