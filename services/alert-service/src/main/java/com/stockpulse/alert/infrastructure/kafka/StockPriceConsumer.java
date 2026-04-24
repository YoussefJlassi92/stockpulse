package com.stockpulse.alert.infrastructure.kafka;

import com.stockpulse.alert.application.AlertEvaluationService;
import com.stockpulse.alert.domain.StockPriceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class StockPriceConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockPriceConsumer.class);

    private final AlertEvaluationService alertEvaluationService;
    private final KafkaTemplate<String, StockPriceEvent> kafkaTemplate;
    private final String dlqTopic;

    public StockPriceConsumer(
            AlertEvaluationService alertEvaluationService,
            KafkaTemplate<String, StockPriceEvent> kafkaTemplate,
            @Value("${app.kafka.topics.stock-prices}") String stockPricesTopic) {
        this.alertEvaluationService = alertEvaluationService;
        this.kafkaTemplate = kafkaTemplate;
        this.dlqTopic = stockPricesTopic + ".DLQ";
    }

    @KafkaListener(topics = "${app.kafka.topics.stock-prices}", groupId = "alert-service")
    public void consume(StockPriceEvent event) {
        try {
            alertEvaluationService.evaluateAlerts(event.symbol(), event.price());
            log.debug("Processed StockPriceEvent for symbol={} price={}", event.symbol(), event.price());
        } catch (Exception ex) {
            log.error("Failed to process StockPriceEvent for symbol={}: {}", event.symbol(), ex.getMessage(), ex);
            kafkaTemplate.send(dlqTopic, event.symbol(), event);
        }
    }
}
