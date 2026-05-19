package com.stockpulse.portfolio.infrastructure.kafka;

import com.stockpulse.portfolio.application.PortfolioService;
import com.stockpulse.portfolio.application.PositionRepository;
import com.stockpulse.portfolio.domain.StockPriceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Kafka consumer for {@code stock.prices} events using <b>manual offset acknowledgment</b>.
 *
 * <h2>Why manual ack?</h2>
 * <p>With the default {@code BATCH} or {@code RECORD} auto-ack modes, Spring Kafka commits
 * the offset as soon as the listener method returns — even if the business logic threw an
 * exception. This means a failed DB write would silently discard the message. Manual ack
 * lets us commit the offset only after we are confident the event has been handled.
 *
 * <h2>Error strategy</h2>
 * <ol>
 *   <li>On success: call {@link Acknowledgment#acknowledge()} immediately after the DB
 *       write completes.</li>
 *   <li>On failure: log the error, forward the raw event to the dead-letter topic
 *       ({@code stock.prices.DLQ}), then still call {@code ack.acknowledge()}. Acknowledging
 *       on the error path is intentional — without it the consumer would stop polling that
 *       partition until the offset is committed elsewhere, causing the entire concurrency
 *       slot to stall.</li>
 * </ol>
 */
@Component
public class StockPriceConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockPriceConsumer.class);

    private final PortfolioService portfolioService;
    private final PositionRepository positionRepository;
    private final KafkaTemplate<String, StockPriceEvent> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final String dlqTopic;

    public StockPriceConsumer(
            PortfolioService portfolioService,
            PositionRepository positionRepository,
            KafkaTemplate<String, StockPriceEvent> kafkaTemplate,
            SimpMessagingTemplate messagingTemplate,
            @Value("${app.kafka.topics.stock-prices}") String stockPricesTopic) {
        this.portfolioService = portfolioService;
        this.positionRepository = positionRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.messagingTemplate = messagingTemplate;
        this.dlqTopic = stockPricesTopic + ".DLQ";
    }

    /**
     * Processes an incoming {@link StockPriceEvent} from the {@code stock.prices} topic.
     *
     * <p>The offset is committed via {@code ack.acknowledge()} in both the success and
     * error branches. See class-level Javadoc for the rationale.
     *
     * @param event the deserialized stock price event
     * @param ack   the manual acknowledgment handle provided by Spring Kafka
     */
    @KafkaListener(topics = "${app.kafka.topics.stock-prices}", groupId = "portfolio-service")
    public void consume(StockPriceEvent event, Acknowledgment ack) {
        try {
            portfolioService.updatePositionPrice(event.symbol(), event.price());
            log.debug("Processed StockPriceEvent for symbol={} price={}", event.symbol(), event.price());
            broadcastPositionUpdates(event.symbol());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process StockPriceEvent for symbol={}: {}", event.symbol(), ex.getMessage(), ex);
            kafkaTemplate.send(dlqTopic, event.symbol(), event);
            ack.acknowledge();
        }
    }

    private void broadcastPositionUpdates(String symbol) {
        positionRepository.findBySymbol(symbol).stream()
                .collect(Collectors.groupingBy(position -> position.getPortfolioId()))
                .forEach((portfolioId, positions) ->
                        positions.forEach(position ->
                                messagingTemplate.convertAndSend("/topic/portfolio/" + portfolioId, position)
                        )
                );
    }
}
