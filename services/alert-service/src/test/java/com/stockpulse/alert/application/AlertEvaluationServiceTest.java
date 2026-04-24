package com.stockpulse.alert.application;

import com.stockpulse.alert.domain.AlertHistory;
import com.stockpulse.alert.domain.AlertRule;
import com.stockpulse.alert.domain.AlertTriggeredEvent;
import com.stockpulse.alert.domain.AlertType;
import com.stockpulse.alert.infrastructure.kafka.AlertEventProducer;
import com.stockpulse.alert.infrastructure.mail.AlertMailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEvaluationServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private AlertEventProducer alertEventProducer;

    @Mock
    private AlertMailService alertMailService;

    @InjectMocks
    private AlertEvaluationService alertEvaluationService;

    private AlertRule buildRule(Long id, String symbol, AlertType type, BigDecimal threshold) {
        return AlertRule.builder()
                .id(id)
                .userId("user-1")
                .symbol(symbol)
                .alertType(type)
                .thresholdPrice(threshold)
                .email("user@example.com")
                .active(true)
                .build();
    }

    @Test
    void evaluateAlerts_aboveThreshold_triggersAlertSavesHistoryAndPublishesEvent() {
        AlertRule rule = buildRule(1L, "AAPL", AlertType.ABOVE, new BigDecimal("150.00"));
        when(alertRuleRepository.findBySymbolAndActiveTrue("AAPL")).thenReturn(List.of(rule));
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        alertEvaluationService.evaluateAlerts("AAPL", new BigDecimal("155.00"));

        ArgumentCaptor<AlertHistory> historyCaptor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(historyCaptor.capture());
        AlertHistory saved = historyCaptor.getValue();
        assertThat(saved.getAlertRuleId()).isEqualTo(1L);
        assertThat(saved.getSymbol()).isEqualTo("AAPL");
        assertThat(saved.getTriggeredPrice()).isEqualByComparingTo("155.00");
        assertThat(saved.getThresholdPrice()).isEqualByComparingTo("150.00");
        assertThat(saved.getAlertType()).isEqualTo(AlertType.ABOVE);
        assertThat(saved.isEmailSent()).isTrue();

        ArgumentCaptor<AlertTriggeredEvent> eventCaptor = ArgumentCaptor.forClass(AlertTriggeredEvent.class);
        verify(alertEventProducer).publish(eventCaptor.capture());
        AlertTriggeredEvent event = eventCaptor.getValue();
        assertThat(event.alertRuleId()).isEqualTo(1L);
        assertThat(event.symbol()).isEqualTo("AAPL");
        assertThat(event.triggeredPrice()).isEqualByComparingTo("155.00");
        assertThat(event.alertType()).isEqualTo(AlertType.ABOVE);
    }

    @Test
    void evaluateAlerts_belowThreshold_triggersAlertSavesHistoryAndPublishesEvent() {
        AlertRule rule = buildRule(2L, "TSLA", AlertType.BELOW, new BigDecimal("200.00"));
        when(alertRuleRepository.findBySymbolAndActiveTrue("TSLA")).thenReturn(List.of(rule));
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        alertEvaluationService.evaluateAlerts("TSLA", new BigDecimal("195.00"));

        ArgumentCaptor<AlertHistory> historyCaptor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getAlertType()).isEqualTo(AlertType.BELOW);
        assertThat(historyCaptor.getValue().getTriggeredPrice()).isEqualByComparingTo("195.00");

        verify(alertEventProducer).publish(any(AlertTriggeredEvent.class));
    }

    @Test
    void evaluateAlerts_priceEqualsThreshold_doesNotTrigger() {
        AlertRule aboveRule = buildRule(1L, "AAPL", AlertType.ABOVE, new BigDecimal("150.00"));
        AlertRule belowRule = buildRule(2L, "AAPL", AlertType.BELOW, new BigDecimal("150.00"));
        when(alertRuleRepository.findBySymbolAndActiveTrue("AAPL")).thenReturn(List.of(aboveRule, belowRule));

        alertEvaluationService.evaluateAlerts("AAPL", new BigDecimal("150.00"));

        verify(alertHistoryRepository, never()).save(any());
        verify(alertEventProducer, never()).publish(any());
        verify(alertMailService, never()).sendAlert(any(), any());
    }

    @Test
    void evaluateAlerts_aboveThreshold_priceNotAbove_doesNotTrigger() {
        AlertRule rule = buildRule(1L, "AAPL", AlertType.ABOVE, new BigDecimal("150.00"));
        when(alertRuleRepository.findBySymbolAndActiveTrue("AAPL")).thenReturn(List.of(rule));

        alertEvaluationService.evaluateAlerts("AAPL", new BigDecimal("140.00"));

        verify(alertHistoryRepository, never()).save(any());
        verify(alertEventProducer, never()).publish(any());
    }

    @Test
    void evaluateAlerts_noActiveRulesForSymbol_doesNothing() {
        when(alertRuleRepository.findBySymbolAndActiveTrue("MSFT")).thenReturn(List.of());

        alertEvaluationService.evaluateAlerts("MSFT", new BigDecimal("300.00"));

        verify(alertHistoryRepository, never()).save(any());
        verify(alertEventProducer, never()).publish(any());
        verify(alertMailService, never()).sendAlert(any(), any());
    }

    @Test
    void evaluateAlerts_emailFails_historyRecordsEmailSentFalse() {
        AlertRule rule = buildRule(1L, "AAPL", AlertType.ABOVE, new BigDecimal("100.00"));
        when(alertRuleRepository.findBySymbolAndActiveTrue("AAPL")).thenReturn(List.of(rule));
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error")).when(alertMailService).sendAlert(eq(rule), any());

        alertEvaluationService.evaluateAlerts("AAPL", new BigDecimal("110.00"));

        ArgumentCaptor<AlertHistory> captor = ArgumentCaptor.forClass(AlertHistory.class);
        verify(alertHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().isEmailSent()).isFalse();

        verify(alertEventProducer).publish(any(AlertTriggeredEvent.class));
    }

    @Test
    void evaluateAlerts_multipleRules_triggersEachMatchingRule() {
        AlertRule rule1 = buildRule(1L, "AAPL", AlertType.ABOVE, new BigDecimal("100.00"));
        AlertRule rule2 = buildRule(2L, "AAPL", AlertType.ABOVE, new BigDecimal("120.00"));
        AlertRule rule3 = buildRule(3L, "AAPL", AlertType.BELOW, new BigDecimal("200.00"));
        when(alertRuleRepository.findBySymbolAndActiveTrue("AAPL")).thenReturn(List.of(rule1, rule2, rule3));
        when(alertHistoryRepository.save(any(AlertHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        // price=130 → triggers rule1 (130>100), rule2 (130>120), rule3 (130<200)
        alertEvaluationService.evaluateAlerts("AAPL", new BigDecimal("130.00"));

        verify(alertHistoryRepository, org.mockito.Mockito.times(3)).save(any(AlertHistory.class));
        verify(alertEventProducer, org.mockito.Mockito.times(3)).publish(any(AlertTriggeredEvent.class));
    }
}
