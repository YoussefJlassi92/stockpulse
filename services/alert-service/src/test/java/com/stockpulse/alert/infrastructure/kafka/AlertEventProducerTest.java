package com.stockpulse.alert.infrastructure.kafka;

import com.stockpulse.alert.domain.AlertTriggeredEvent;
import com.stockpulse.alert.domain.AlertType;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEventProducerTest {

    @Mock
    private KafkaTemplate<String, AlertTriggeredEvent> kafkaTemplate;

    private AlertEventProducer producer;

    private static final String TOPIC = "alerts.triggered";

    @BeforeEach
    void setUp() {
        producer = new AlertEventProducer(kafkaTemplate, TOPIC);
    }

    @Test
    void publish_sendsEventToCorrectTopicWithRuleIdAsKey() {
        AlertTriggeredEvent event = new AlertTriggeredEvent(
                42L,
                "user-1",
                "AAPL",
                new BigDecimal("155.00"),
                new BigDecimal("150.00"),
                AlertType.ABOVE,
                "user@example.com",
                OffsetDateTime.now());

        RecordMetadata metadata = new RecordMetadata(new TopicPartition(TOPIC, 0), 0, 0, 0, 0, 0);
        SendResult<String, AlertTriggeredEvent> sendResult = new SendResult<>(null, metadata);
        when(kafkaTemplate.send(anyString(), anyString(), any(AlertTriggeredEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        producer.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AlertTriggeredEvent> eventCaptor = ArgumentCaptor.forClass(AlertTriggeredEvent.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(TOPIC);
        assertThat(keyCaptor.getValue()).isEqualTo("42");
        assertThat(eventCaptor.getValue()).isSameAs(event);
    }

    @Test
    void publish_doesNotThrowWhenBrokerFails() {
        AlertTriggeredEvent event = new AlertTriggeredEvent(
                1L, "user-1", "TSLA",
                new BigDecimal("200.00"), new BigDecimal("250.00"),
                AlertType.BELOW, "a@b.com", OffsetDateTime.now());

        CompletableFuture<SendResult<String, AlertTriggeredEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failed);

        // must not throw
        producer.publish(event);
    }
}
