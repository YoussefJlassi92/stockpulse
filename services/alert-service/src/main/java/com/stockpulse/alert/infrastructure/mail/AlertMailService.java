package com.stockpulse.alert.infrastructure.mail;

import com.stockpulse.alert.domain.AlertRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class AlertMailService {

    private static final Logger log = LoggerFactory.getLogger(AlertMailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public AlertMailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.enabled}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    public void sendAlert(AlertRule rule, BigDecimal triggeredPrice) {
        if (!enabled) {
            log.debug("Mail disabled — skipping alert email for ruleId={}", rule.getId());
            return;
        }

        String subject = "StockPulse Alert: %s %s %s"
                .formatted(rule.getSymbol(), rule.getAlertType(), rule.getThresholdPrice());

        String body = """
                StockPulse Price Alert

                Symbol:          %s
                Alert type:      %s
                Threshold price: %s
                Current price:   %s
                Triggered at:    %s

                This is an automated notification from StockPulse.
                """.formatted(
                rule.getSymbol(),
                rule.getAlertType(),
                rule.getThresholdPrice(),
                triggeredPrice,
                OffsetDateTime.now());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(rule.getEmail());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("Alert email sent to {} for ruleId={} symbol={}", rule.getEmail(), rule.getId(), rule.getSymbol());
    }
}
