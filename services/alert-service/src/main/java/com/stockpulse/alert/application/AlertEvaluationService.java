package com.stockpulse.alert.application;

import com.stockpulse.alert.domain.AlertHistory;
import com.stockpulse.alert.domain.AlertRule;
import com.stockpulse.alert.domain.AlertTriggeredEvent;
import com.stockpulse.alert.infrastructure.kafka.AlertEventProducer;
import com.stockpulse.alert.infrastructure.mail.AlertMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Core evaluation engine that checks incoming stock prices against active alert rules.
 *
 * <p>On each price tick for a given symbol, all active {@link AlertRule}s for that
 * symbol are loaded and tested against the current price. When a rule's condition is
 * met the service:
 * <ol>
 *   <li>Persists an {@link AlertHistory} record with the outcome of the email attempt.</li>
 *   <li>Publishes an {@link AlertTriggeredEvent} to the {@code alerts.triggered} Kafka topic
 *       so that downstream consumers can react.</li>
 *   <li>Attempts to send an email notification via {@link AlertMailService} (no-op when
 *       {@code app.mail.enabled=false}).</li>
 * </ol>
 *
 * <p>A failure to send the email is caught and logged, but never propagates — the
 * history record reflects the actual outcome via the {@code emailSent} flag.
 */
@Service
@Transactional
public class AlertEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationService.class);

    private final AlertRuleRepository alertRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertEventProducer alertEventProducer;
    private final AlertMailService alertMailService;

    public AlertEvaluationService(
            AlertRuleRepository alertRuleRepository,
            AlertHistoryRepository alertHistoryRepository,
            AlertEventProducer alertEventProducer,
            AlertMailService alertMailService) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertHistoryRepository = alertHistoryRepository;
        this.alertEventProducer = alertEventProducer;
        this.alertMailService = alertMailService;
    }

    /**
     * Evaluates all active alert rules for the given symbol against {@code price}.
     *
     * <p>An {@link com.stockpulse.alert.domain.AlertType#ABOVE ABOVE} rule fires when
     * {@code price > thresholdPrice}; a {@link com.stockpulse.alert.domain.AlertType#BELOW BELOW}
     * rule fires when {@code price < thresholdPrice}. Equality never triggers.
     *
     * @param symbol the ticker symbol received from the market-data feed
     * @param price  the latest market price for the symbol
     */
    public void evaluateAlerts(String symbol, BigDecimal price) {
        List<AlertRule> rules = alertRuleRepository.findBySymbolAndActiveTrue(symbol);
        if (rules.isEmpty()) {
            log.debug("No active alert rules for symbol={}", symbol);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        for (AlertRule rule : rules) {
            boolean triggered = switch (rule.getAlertType()) {
                case ABOVE -> price.compareTo(rule.getThresholdPrice()) > 0;
                case BELOW -> price.compareTo(rule.getThresholdPrice()) < 0;
            };

            if (!triggered) {
                continue;
            }

            log.info("Alert triggered: ruleId={} userId={} symbol={} type={} threshold={} price={}",
                    rule.getId(), rule.getUserId(), symbol,
                    rule.getAlertType(), rule.getThresholdPrice(), price);

            boolean emailSent = false;
            try {
                alertMailService.sendAlert(rule, price);
                emailSent = true;
            } catch (Exception ex) {
                log.error("Failed to send alert email for ruleId={}: {}", rule.getId(), ex.getMessage(), ex);
            }

            AlertHistory history = AlertHistory.builder()
                    .alertRuleId(rule.getId())
                    .symbol(symbol)
                    .triggeredPrice(price)
                    .thresholdPrice(rule.getThresholdPrice())
                    .alertType(rule.getAlertType())
                    .emailSent(emailSent)
                    .triggeredAt(now)
                    .build();
            alertHistoryRepository.save(history);

            AlertTriggeredEvent event = new AlertTriggeredEvent(
                    rule.getId(),
                    rule.getUserId(),
                    symbol,
                    price,
                    rule.getThresholdPrice(),
                    rule.getAlertType(),
                    rule.getEmail(),
                    now);
            alertEventProducer.publish(event);
        }
    }
}
