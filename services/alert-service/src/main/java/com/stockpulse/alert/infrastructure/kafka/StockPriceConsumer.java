package com.stockpulse.alert.infrastructure.kafka;

import com.stockpulse.alert.application.AlertEvaluationService;
import com.stockpulse.alert.application.DltEventRepository;
import com.stockpulse.alert.domain.DltEvent;
import com.stockpulse.alert.domain.StockPriceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class StockPriceConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockPriceConsumer.class);

    private final AlertEvaluationService alertEvaluationService;
    private final DltEventRepository dltEventRepository;

    public StockPriceConsumer(AlertEvaluationService alertEvaluationService,
                              DltEventRepository dltEventRepository) {
        this.alertEvaluationService = alertEvaluationService;
        this.dltEventRepository = dltEventRepository;
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true",
            dltTopicSuffix = ".dlt"
    )
    @KafkaListener(topics = "${app.kafka.topics.stock-prices}", groupId = "alert-service")
    public void consume(StockPriceEvent event) {
        alertEvaluationService.evaluateAlerts(event.symbol(), event.price());
        log.debug("Processed StockPriceEvent for symbol={} price={}", event.symbol(), event.price());
    }

    @DltHandler
    public void handleDlt(
            StockPriceEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        log.error("DLT: exhausted retries for symbol={} price={} topic={} error={}",
                event.symbol(), event.price(), topic, errorMessage);
        DltEvent dltEvent = new DltEvent();
        dltEvent.setTopic(topic);
        dltEvent.setSymbol(event.symbol());
        dltEvent.setPrice(event.price());
        dltEvent.setErrorMessage(errorMessage);
        dltEventRepository.save(dltEvent);
    }
}
