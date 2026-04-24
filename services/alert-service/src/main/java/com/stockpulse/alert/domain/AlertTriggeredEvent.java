package com.stockpulse.alert.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AlertTriggeredEvent(
        Long alertRuleId,
        String userId,
        String symbol,
        BigDecimal triggeredPrice,
        BigDecimal thresholdPrice,
        AlertType alertType,
        String email,
        OffsetDateTime triggeredAt
) {}
