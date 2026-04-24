package com.stockpulse.alert.infrastructure.kafka;

import com.stockpulse.alert.application.AlertEvaluationService;
import com.stockpulse.alert.domain.StockPriceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockPriceConsumerTest {

    @Mock
    private AlertEvaluationService alertEvaluationService;

    @Mock
    private KafkaTemplate<String, StockPriceEvent> kafkaTemplate;

    private StockPriceConsumer consumer;

    private static final String STOCK_PRICES_TOPIC = "stock.prices";
    private static final String DLQ_TOPIC = "stock.prices.DLQ";

    @BeforeEach
    void setUp() {
        consumer = new StockPriceConsumer(alertEvaluationService, kafkaTemplate, STOCK_PRICES_TOPIC);
    }

    @Test
    void consume_happyPath_callsEvaluateAlerts() {
        StockPriceEvent event = new StockPriceEvent(
                "AAPL",
                new BigDecimal("175.50"),
                1_000_000L,
                new BigDecimal("1.25"),
                OffsetDateTime.now(),
                "alpha-vantage");

        consumer.consume(event);

        verify(alertEvaluationService).evaluateAlerts("AAPL", new BigDecimal("175.50"));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void consume_errorPath_sendsEventToDlqTopic() {
        StockPriceEvent event = new StockPriceEvent(
                "TSLA",
                new BigDecimal("250.00"),
                500_000L,
                new BigDecimal("-0.50"),
                OffsetDateTime.now(),
                "alpha-vantage");

        doThrow(new RuntimeException("evaluation error"))
                .when(alertEvaluationService).evaluateAlerts(eq("TSLA"), any(BigDecimal.class));

        consumer.consume(event);

        verify(alertEvaluationService).evaluateAlerts("TSLA", new BigDecimal("250.00"));

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<StockPriceEvent> eventCaptor = ArgumentCaptor.forClass(StockPriceEvent.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(DLQ_TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo("TSLA");
        assertThat(eventCaptor.getValue()).isSameAs(event);
    }
}
