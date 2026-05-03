package com.stockpulse.analytics.application;

import com.stockpulse.analytics.domain.PriceAnalytics;
import com.stockpulse.analytics.domain.SpikeAlert;
import com.stockpulse.analytics.domain.StockPriceEvent;
import com.stockpulse.analytics.infrastructure.serde.PriceAnalyticsSerde;
import com.stockpulse.analytics.infrastructure.serde.SpikeAlertSerde;
import com.stockpulse.analytics.infrastructure.serde.StockPriceEventSerde;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class StockPriceAnalyticsTopologyTest {

    private static final String INPUT_TOPIC     = "stock.prices";
    private static final String ANALYTICS_TOPIC = "stock.prices.analytics";
    private static final String SPIKES_TOPIC    = "stock.prices.spikes";

    private TopologyTestDriver driver;
    private TestInputTopic<String, StockPriceEvent> inputTopic;
    private TestOutputTopic<String, PriceAnalytics> analyticsTopic;
    private TestOutputTopic<String, SpikeAlert>     spikesTopic;

    @BeforeEach
    void setUp() {
        StockPriceAnalyticsTopology topology =
                new StockPriceAnalyticsTopology(INPUT_TOPIC, ANALYTICS_TOPIC, SPIKES_TOPIC, 5, 3.0);

        StreamsBuilder builder = new StreamsBuilder();
        topology.stockPricesStream(builder);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-analytics");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                org.apache.kafka.common.serialization.Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                org.apache.kafka.common.serialization.Serdes.StringSerde.class);

        driver = new TopologyTestDriver(builder.build(), props);

        inputTopic = driver.createInputTopic(INPUT_TOPIC,
                new StringSerializer(), new StockPriceEventSerde().serializer());
        analyticsTopic = driver.createOutputTopic(ANALYTICS_TOPIC,
                new StringDeserializer(), new PriceAnalyticsSerde().deserializer());
        spikesTopic = driver.createOutputTopic(SPIKES_TOPIC,
                new StringDeserializer(), new SpikeAlertSerde().deserializer());
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private StockPriceEvent event(String symbol, String price) {
        return new StockPriceEvent(symbol, new BigDecimal(price), 1_000_000L,
                BigDecimal.ZERO, OffsetDateTime.now(), "test");
    }

    private void pipe(String symbol, String price, Instant timestamp) {
        inputTopic.pipeInput(symbol, event(symbol, price), timestamp);
    }

    // ─── Pipeline 1: tumbling window analytics ────────────────────────────────

    @Test
    void analytics_3eventsInWindow_producesCorrectAggregateWhenWindowCloses() {
        Instant t0 = Instant.EPOCH;

        pipe("AAPL", "100.00", t0);
        pipe("AAPL", "110.00", t0.plusSeconds(60));
        pipe("AAPL", "120.00", t0.plusSeconds(120));

        // Record at t=5min+1s advances stream time past the window end, closing it
        pipe("AAPL", "115.00", t0.plusSeconds(301));

        List<PriceAnalytics> records = analyticsTopic.readValuesToList()
                .stream().filter(a -> a.symbol().equals("AAPL")).toList();

        assertThat(records).hasSize(1);
        PriceAnalytics a = records.get(0);
        assertThat(a.symbol()).isEqualTo("AAPL");
        assertThat(a.messageCount()).isEqualTo(3);
        assertThat(a.avgPrice()).isEqualByComparingTo("110.0000");
        assertThat(a.minPrice()).isEqualByComparingTo("100.00");
        assertThat(a.maxPrice()).isEqualByComparingTo("120.00");
        // priceChangePercent = (120 - 100) / 100 * 100 = 20%
        assertThat(a.priceChangePercent()).isEqualByComparingTo("20.0000");
        assertThat(a.windowStart()).isEqualTo(Instant.EPOCH.atOffset(ZoneOffset.UTC));
    }

    @Test
    void analytics_singleEventInWindow_avgEqualsPrice() {
        Instant t0 = Instant.EPOCH;
        pipe("MSFT", "300.00", t0);
        pipe("MSFT", "305.00", t0.plusSeconds(301)); // closes window

        List<PriceAnalytics> records = analyticsTopic.readValuesToList()
                .stream().filter(a -> a.symbol().equals("MSFT")).toList();

        assertThat(records).hasSize(1);
        PriceAnalytics a = records.get(0);
        assertThat(a.messageCount()).isEqualTo(1);
        assertThat(a.avgPrice()).isEqualByComparingTo("300.0000");
        assertThat(a.minPrice()).isEqualByComparingTo("300.00");
        assertThat(a.maxPrice()).isEqualByComparingTo("300.00");
        assertThat(a.priceChangePercent()).isEqualByComparingTo("0.0000");
    }

    @Test
    void analytics_twoSymbols_produceIndependentWindowsPerSymbol() {
        Instant t0 = Instant.EPOCH;
        pipe("JPM", "150.00", t0);
        pipe("GS",  "400.00", t0.plusSeconds(30));
        // trigger window close for both
        pipe("JPM", "155.00", t0.plusSeconds(301));
        pipe("GS",  "405.00", t0.plusSeconds(302));

        List<PriceAnalytics> all = analyticsTopic.readValuesToList();
        assertThat(all.stream().map(PriceAnalytics::symbol))
                .containsExactlyInAnyOrder("JPM", "GS");
    }

    // ─── Pipeline 2: spike detection ─────────────────────────────────────────

    @Test
    void spikeDetection_firstEventEstablishesBaseline_noAlertEmitted() {
        pipe("AAPL", "100.00", Instant.EPOCH);
        assertThat(spikesTopic.isEmpty()).isTrue();
    }

    @Test
    void spikeDetection_priceIncreaseAboveThreshold_emitsSpikeAlert() {
        Instant t0 = Instant.EPOCH;
        pipe("AAPL", "100.00", t0);
        pipe("AAPL", "105.00", t0.plusSeconds(1)); // +5%, above 3% threshold

        List<SpikeAlert> alerts = spikesTopic.readValuesToList();
        assertThat(alerts).hasSize(1);
        SpikeAlert alert = alerts.get(0);
        assertThat(alert.symbol()).isEqualTo("AAPL");
        assertThat(alert.previousPrice()).isEqualByComparingTo("100.00");
        assertThat(alert.currentPrice()).isEqualByComparingTo("105.00");
        assertThat(alert.changePercent()).isEqualByComparingTo("5.0000");
    }

    @Test
    void spikeDetection_priceDecreaseAboveThreshold_emitsSpikeAlert() {
        Instant t0 = Instant.EPOCH;
        pipe("TSLA", "200.00", t0);
        pipe("TSLA", "190.00", t0.plusSeconds(1)); // −5%, below −3% threshold

        List<SpikeAlert> alerts = spikesTopic.readValuesToList();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).changePercent()).isEqualByComparingTo("-5.0000");
    }

    @Test
    void spikeDetection_smallChange_noAlertEmitted() {
        Instant t0 = Instant.EPOCH;
        pipe("AAPL", "100.00", t0);
        pipe("AAPL", "101.00", t0.plusSeconds(1)); // +1%, below 3% threshold

        assertThat(spikesTopic.isEmpty()).isTrue();
    }

    @Test
    void spikeDetection_changeExactlyAtThreshold_emitsSpikeAlert() {
        Instant t0 = Instant.EPOCH;
        pipe("GS", "100.00", t0);
        pipe("GS", "103.00", t0.plusSeconds(1)); // exactly 3%

        assertThat(spikesTopic.readValuesToList()).hasSize(1);
    }

    @Test
    void spikeDetection_changeJustBelowThreshold_noAlertEmitted() {
        Instant t0 = Instant.EPOCH;
        pipe("GS", "100.00", t0);
        pipe("GS", "102.99", t0.plusSeconds(1)); // 2.99%

        assertThat(spikesTopic.isEmpty()).isTrue();
    }

    @Test
    void spikeDetection_consecutiveSpikes_eachAlertUsesLatestPrice() {
        Instant t0 = Instant.EPOCH;
        pipe("MSFT", "100.00", t0);
        pipe("MSFT", "110.00", t0.plusSeconds(1));  // +10% → spike
        pipe("MSFT", "121.00", t0.plusSeconds(2));  // +10% on 110 → spike

        List<SpikeAlert> alerts = spikesTopic.readValuesToList();
        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).previousPrice()).isEqualByComparingTo("100.00");
        assertThat(alerts.get(0).currentPrice()).isEqualByComparingTo("110.00");
        assertThat(alerts.get(1).previousPrice()).isEqualByComparingTo("110.00");
        assertThat(alerts.get(1).currentPrice()).isEqualByComparingTo("121.00");
    }

    @Test
    void spikeDetection_twoSymbolsIndependent_alertsTrackedPerSymbol() {
        Instant t0 = Instant.EPOCH;
        pipe("AAPL", "100.00", t0);
        pipe("JPM",  "50.00",  t0.plusSeconds(1));
        pipe("AAPL", "106.00", t0.plusSeconds(2));  // +6% on AAPL → spike
        pipe("JPM",  "51.00",  t0.plusSeconds(3));  // +2% on JPM → no spike

        List<SpikeAlert> alerts = spikesTopic.readValuesToList();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).symbol()).isEqualTo("AAPL");
    }
}
