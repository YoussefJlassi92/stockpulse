package com.stockpulse.alert.infrastructure.kafka;

import com.stockpulse.alert.domain.AlertTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer that publishes {@link AlertTriggeredEvent}s to the
 * {@code alerts.triggered} topic.
 *
 * <p>The alert rule id is used as the message key so that all events for the
 * same rule land on the same partition, preserving per-rule ordering.
 */
@Component
public class AlertEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AlertEventProducer.class);

    private final KafkaTemplate<String, AlertTriggeredEvent> kafkaTemplate;
    private final String topic;

    public AlertEventProducer(
            KafkaTemplate<String, AlertTriggeredEvent> kafkaTemplate,
            @Value("${app.kafka.topics.alerts-triggered}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /**
     * Publishes an alert triggered event asynchronously.
     *
     * <p>Logs success at DEBUG level and failures at ERROR level; the caller
     * is not blocked waiting for broker acknowledgement.
     *
     * @param event the alert triggered event to publish
     */
    public void publish(AlertTriggeredEvent event) {
        CompletableFuture<SendResult<String, AlertTriggeredEvent>> future =
                kafkaTemplate.send(topic, String.valueOf(event.alertRuleId()), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish AlertTriggeredEvent for ruleId={} to topic {}: {}",
                        event.alertRuleId(), topic, ex.getMessage(), ex);
            } else {
                log.debug("Published AlertTriggeredEvent for ruleId={} to {}-{}@{}",
                        event.alertRuleId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
