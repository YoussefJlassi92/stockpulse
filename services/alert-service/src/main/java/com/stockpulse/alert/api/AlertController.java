package com.stockpulse.alert.api;

import com.stockpulse.alert.application.AlertRuleService;
import com.stockpulse.alert.domain.AlertRule;
import com.stockpulse.alert.domain.CreateAlertRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertRuleService alertRuleService;

    public AlertController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @PostMapping
    public ResponseEntity<AlertRule> createAlert(@RequestBody CreateAlertRequest request) {
        AlertRule created = alertRuleService.createAlert(
                request.userId(),
                request.symbol(),
                request.thresholdPrice(),
                request.alertType(),
                request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<AlertRule>> getAlertsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(alertRuleService.getAlertsByUser(userId));
    }

    @DeleteMapping("/{alertId}")
    public ResponseEntity<Void> deactivateAlert(@PathVariable Long alertId) {
        alertRuleService.deactivateAlert(alertId);
        return ResponseEntity.noContent().build();
    }
}
