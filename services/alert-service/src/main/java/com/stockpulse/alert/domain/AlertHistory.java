package com.stockpulse.alert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "alert_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Builder
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_rule_id", nullable = false)
    private Long alertRuleId;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(name = "triggered_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal triggeredPrice;

    @Column(name = "threshold_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal thresholdPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 10)
    private AlertType alertType;

    @Column(name = "email_sent", nullable = false)
    @Builder.Default
    private boolean emailSent = false;

    @Column(name = "triggered_at", nullable = false)
    private OffsetDateTime triggeredAt;

    @PrePersist
    void prePersist() {
        if (triggeredAt == null) {
            triggeredAt = OffsetDateTime.now();
        }
    }
}
