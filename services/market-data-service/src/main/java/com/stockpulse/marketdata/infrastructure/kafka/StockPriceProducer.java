package com.stockpulse.marketdata.infrastructure.kafka;

import com.stockpulse.marketdata.domain.StockPriceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer that publishes {@link StockPriceDto} events to the
 * {@code stock.prices} topic.
 *
 * <p>The stock symbol is used as the message key so that all quotes for the
 * same symbol land on the same partition, preserving ordering per symbol.
 */
@Component
public class StockPriceProducer {

    private static final Logger log = LoggerFactory.getLogger(StockPriceProducer.class);

    private final KafkaTemplate<String, StockPriceDto> kafkaTemplate;
    private final String topic;

    public StockPriceProducer(
            KafkaTemplate<String, StockPriceDto> kafkaTemplate,
            @Value("${app.kafka.topics.stock-prices}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * Publishes a stock price event asynchronously.
     *
     * <p>Logs success at DEBUG level and failures at ERROR level; the caller
     * is not blocked waiting for broker acknowledgement.
     *
     * @param dto the stock price event to publish
     */
    public void publish(StockPriceDto dto) {
        CompletableFuture<SendResult<String, StockPriceDto>> future =
                kafkaTemplate.send(topic, dto.symbol(), dto);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish StockPriceDto for symbol {} to topic {}: {}",
                        dto.symbol(), topic, ex.getMessage(), ex);
            } else {
                log.debug("Published StockPriceDto for {} to {}-{}@{}",
                        dto.symbol(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
