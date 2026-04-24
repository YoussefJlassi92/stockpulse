package com.stockpulse.alert.application;

import com.stockpulse.alert.domain.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    List<AlertHistory> findByAlertRuleIdOrderByTriggeredAtDesc(Long alertRuleId);
}
