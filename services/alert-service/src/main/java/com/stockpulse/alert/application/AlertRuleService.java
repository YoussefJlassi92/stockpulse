package com.stockpulse.alert.application;

import com.stockpulse.alert.domain.AlertRule;
import com.stockpulse.alert.domain.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class AlertRuleService {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleService.class);

    private final AlertRuleRepository alertRuleRepository;

    public AlertRuleService(AlertRuleRepository alertRuleRepository) {
        this.alertRuleRepository = alertRuleRepository;
    }

    public AlertRule createAlert(String userId, String symbol, BigDecimal threshold,
                                 AlertType type, String email) {
        AlertRule rule = AlertRule.builder()
                .userId(userId)
                .symbol(symbol.toUpperCase())
                .thresholdPrice(threshold)
                .alertType(type)
                .email(email)
                .active(true)
                .build();
        AlertRule saved = alertRuleRepository.save(rule);
        log.debug("Created alert rule id={} userId={} symbol={} type={} threshold={}",
                saved.getId(), userId, symbol, type, threshold);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AlertRule> getAlertsByUser(String userId) {
        return alertRuleRepository.findByUserId(userId);
    }

    public void deactivateAlert(Long alertId) {
        AlertRule rule = alertRuleRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert rule not found: " + alertId));
        rule.setActive(false);
        alertRuleRepository.save(rule);
        log.debug("Deactivated alert rule id={}", alertId);
    }
}
