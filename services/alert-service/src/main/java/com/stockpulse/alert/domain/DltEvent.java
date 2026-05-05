package com.stockpulse.alert.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dlt_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DltEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(precision = 15, scale = 4)
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private OffsetDateTime failedAt;

    @PrePersist
    void prePersist() {
        failedAt = OffsetDateTime.now();
    }
}
