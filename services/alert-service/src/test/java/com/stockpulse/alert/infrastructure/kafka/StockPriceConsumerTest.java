package com.stockpulse.alert.infrastructure.kafka;

import com.stockpulse.alert.application.AlertEvaluationService;
import com.stockpulse.alert.application.DltEventRepository;
import com.stockpulse.alert.domain.DltEvent;
import com.stockpulse.alert.domain.StockPriceEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockPriceConsumerTest {

    @Mock
    private AlertEvaluationService alertEvaluationService;

    @Mock
    private DltEventRepository dltEventRepository;

    @InjectMocks
    private StockPriceConsumer consumer;

    private static final StockPriceEvent EVENT = new StockPriceEvent(
            "AAPL", new BigDecimal("175.50"), 1_000_000L,
            new BigDecimal("1.25"), OffsetDateTime.now(), "alpha-vantage");

    @Test
    void consume_happyPath_callsEvaluateAlerts() {
        consumer.consume(EVENT);

        verify(alertEvaluationService).evaluateAlerts("AAPL", new BigDecimal("175.50"));
    }

    @Test
    void consume_transientFailure_eventuallySucceeds() {
        // Simulate two transient failures followed by a success (mimicking @RetryableTopic retries)
        doThrow(new RuntimeException("transient error"))
                .doThrow(new RuntimeException("transient error"))
                .doNothing()
                .when(alertEvaluationService).evaluateAlerts(eq("AAPL"), any(BigDecimal.class));

        assertThatThrownBy(() -> consumer.consume(EVENT))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("transient error");
        assertThatThrownBy(() -> consumer.consume(EVENT))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("transient error");
        assertThatCode(() -> consumer.consume(EVENT)).doesNotThrowAnyException();

        verify(alertEvaluationService, times(3)).evaluateAlerts("AAPL", new BigDecimal("175.50"));
    }

    @Test
    void handleDlt_allRetriesExhausted_savesDltEvent() {
        String topic = "stock.prices.dlt";
        String errorMsg = "evaluation failed permanently";

        consumer.handleDlt(EVENT, topic, errorMsg);

        ArgumentCaptor<DltEvent> captor = ArgumentCaptor.forClass(DltEvent.class);
        verify(dltEventRepository).save(captor.capture());
        DltEvent saved = captor.getValue();
        assertThat(saved.getTopic()).isEqualTo(topic);
        assertThat(saved.getSymbol()).isEqualTo("AAPL");
        assertThat(saved.getPrice()).isEqualByComparingTo("175.50");
        assertThat(saved.getErrorMessage()).isEqualTo(errorMsg);
    }
}
