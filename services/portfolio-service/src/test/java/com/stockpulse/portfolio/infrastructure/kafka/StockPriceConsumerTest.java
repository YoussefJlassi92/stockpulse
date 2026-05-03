package com.stockpulse.portfolio.infrastructure.kafka;

import com.stockpulse.portfolio.application.PortfolioService;
import com.stockpulse.portfolio.domain.StockPriceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockPriceConsumerTest {

    @Mock
    private PortfolioService portfolioService;

    @Mock
    private KafkaTemplate<String, StockPriceEvent> kafkaTemplate;

    @Mock
    private Acknowledgment ack;

    private StockPriceConsumer consumer;

    private static final String STOCK_PRICES_TOPIC = "stock.prices";
    private static final String DLQ_TOPIC = "stock.prices.DLQ";

    @BeforeEach
    void setUp() {
        consumer = new StockPriceConsumer(portfolioService, kafkaTemplate, STOCK_PRICES_TOPIC);
    }

    @Test
    void consume_happyPath_callsUpdatePositionPriceThenAcknowledges() {
        StockPriceEvent event = new StockPriceEvent(
                "AAPL",
                new BigDecimal("175.50"),
                1_000_000L,
                new BigDecimal("1.25"),
                OffsetDateTime.now(),
                "alpha-vantage");

        consumer.consume(event, ack);

        var order = inOrder(portfolioService, ack);
        order.verify(portfolioService).updatePositionPrice("AAPL", new BigDecimal("175.50"));
        order.verify(ack).acknowledge();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void consume_errorPath_sendsToDlqThenAcknowledgesToAvoidPartitionBlocking() {
        StockPriceEvent event = new StockPriceEvent(
                "TSLA",
                new BigDecimal("250.00"),
                500_000L,
                new BigDecimal("-0.50"),
                OffsetDateTime.now(),
                "alpha-vantage");

        doThrow(new RuntimeException("DB error"))
                .when(portfolioService).updatePositionPrice(eq("TSLA"), any(BigDecimal.class));

        consumer.consume(event, ack);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor   = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<StockPriceEvent> eventCaptor = ArgumentCaptor.forClass(StockPriceEvent.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(DLQ_TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo("TSLA");
        assertThat(eventCaptor.getValue()).isSameAs(event);

        // offset must be committed even on failure to prevent partition stall
        verify(ack).acknowledge();
    }

    @Test
    void consume_errorPath_acknowledgesAfterDlqSend() {
        StockPriceEvent event = new StockPriceEvent(
                "TSLA",
                new BigDecimal("250.00"),
                500_000L,
                new BigDecimal("-0.50"),
                OffsetDateTime.now(),
                "alpha-vantage");

        doThrow(new RuntimeException("DB error"))
                .when(portfolioService).updatePositionPrice(eq("TSLA"), any(BigDecimal.class));

        consumer.consume(event, ack);

        var order = inOrder(kafkaTemplate, ack);
        order.verify(kafkaTemplate).send(eq(DLQ_TOPIC), eq("TSLA"), any(StockPriceEvent.class));
        order.verify(ack).acknowledge();
    }
}
