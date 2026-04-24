package com.stockpulse.alert.application;

import com.stockpulse.alert.domain.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findBySymbolAndActiveTrue(String symbol);

    List<AlertRule> findByUserId(String userId);
}
