package com.stockpulse.alert.application;

import com.stockpulse.alert.domain.AlertRule;
import com.stockpulse.alert.domain.AlertType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @InjectMocks
    private AlertRuleService alertRuleService;

    @Test
    void createAlert_savesRuleWithCorrectFieldsAndReturnsIt() {
        AlertRule expected = AlertRule.builder()
                .id(1L)
                .userId("user-1")
                .symbol("AAPL")
                .thresholdPrice(new BigDecimal("150.00"))
                .alertType(AlertType.ABOVE)
                .email("user@example.com")
                .active(true)
                .build();
        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(expected);

        AlertRule result = alertRuleService.createAlert(
                "user-1", "aapl", new BigDecimal("150.00"), AlertType.ABOVE, "user@example.com");

        ArgumentCaptor<AlertRule> captor = ArgumentCaptor.forClass(AlertRule.class);
        verify(alertRuleRepository).save(captor.capture());
        AlertRule captured = captor.getValue();

        assertThat(captured.getUserId()).isEqualTo("user-1");
        assertThat(captured.getSymbol()).isEqualTo("AAPL");
        assertThat(captured.getThresholdPrice()).isEqualByComparingTo("150.00");
        assertThat(captured.getAlertType()).isEqualTo(AlertType.ABOVE);
        assertThat(captured.getEmail()).isEqualTo("user@example.com");
        assertThat(captured.isActive()).isTrue();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void createAlert_uppercasesSymbol() {
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

        alertRuleService.createAlert("user-1", "tsla", BigDecimal.TEN, AlertType.BELOW, "a@b.com");

        ArgumentCaptor<AlertRule> captor = ArgumentCaptor.forClass(AlertRule.class);
        verify(alertRuleRepository).save(captor.capture());
        assertThat(captor.getValue().getSymbol()).isEqualTo("TSLA");
    }

    @Test
    void getAlertsByUser_returnsAllRulesForUser() {
        List<AlertRule> rules = List.of(
                AlertRule.builder().id(1L).userId("user-1").build(),
                AlertRule.builder().id(2L).userId("user-1").build());
        when(alertRuleRepository.findByUserId("user-1")).thenReturn(rules);

        List<AlertRule> result = alertRuleService.getAlertsByUser("user-1");

        assertThat(result).hasSize(2).containsExactlyElementsOf(rules);
    }

    @Test
    void deactivateAlert_setsActiveFalseAndSaves() {
        AlertRule rule = AlertRule.builder().id(10L).active(true).build();
        when(alertRuleRepository.findById(10L)).thenReturn(Optional.of(rule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

        alertRuleService.deactivateAlert(10L);

        assertThat(rule.isActive()).isFalse();
        verify(alertRuleRepository).save(rule);
    }

    @Test
    void deactivateAlert_throwsIllegalArgumentExceptionWhenNotFound() {
        when(alertRuleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertRuleService.deactivateAlert(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }
}
