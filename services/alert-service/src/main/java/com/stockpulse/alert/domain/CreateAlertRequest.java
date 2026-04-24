package com.stockpulse.alert.domain;

import java.math.BigDecimal;

public record CreateAlertRequest(
        String userId,
        String symbol,
        BigDecimal thresholdPrice,
        AlertType alertType,
        String email
) {}
