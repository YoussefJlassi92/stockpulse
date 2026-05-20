package com.stockpulse.alert;

import com.stockpulse.alert.application.AlertHistoryRepository;
import com.stockpulse.alert.application.AlertRuleRepository;
import com.stockpulse.alert.application.DltEventRepository;
import com.stockpulse.alert.domain.AlertTriggeredEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Smoke test: verifies the Spring context assembles correctly.
 * DataSource, JPA, Flyway and Kafka autoconfiguration are excluded
 * via test application.yml; infrastructure beans are replaced with mocks.
 *
 * <p>Bean dependency chain requiring mocks:
 * <ul>
 *   <li>AlertEvaluationService → AlertRuleRepository, AlertHistoryRepository</li>
 *   <li>AlertEvaluationService → AlertEventProducer → KafkaTemplate</li>
 *   <li>AlertEvaluationService → AlertMailService → JavaMailSender</li>
 *   <li>StockPriceConsumer → DltEventRepository</li>
 * </ul>
 */
@SpringBootTest
class AlertServiceApplicationTests {

	@MockitoBean
	AlertRuleRepository alertRuleRepository;

	@MockitoBean
	AlertHistoryRepository alertHistoryRepository;

	@MockitoBean
	DltEventRepository dltEventRepository;

	@MockitoBean
	JavaMailSender javaMailSender;

	@MockitoBean
	KafkaTemplate<String, AlertTriggeredEvent> kafkaTemplate;

	@Test
	void contextLoads() {
	}
}
