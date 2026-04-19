package com.stockpulse.marketdata.infrastructure.kafka;

import com.stockpulse.marketdata.domain.StockPriceDto;
import org.apache.kafka.clients.producer.ProducerRecord;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockPriceProducerTest {

    @Mock
    private KafkaTemplate<String, StockPriceDto> kafkaTemplate;

    private StockPriceProducer producer;
    private static final String TOPIC = "stock.prices";

    @BeforeEach
    void setUp() {
        producer = new StockPriceProducer(kafkaTemplate, TOPIC);
    }

    @Test
    void publish_sendsWithSymbolAsKey() {
        StockPriceDto dto = aDto("AAPL");
        ProducerRecord<String, StockPriceDto> record = new ProducerRecord<>(TOPIC, "AAPL", dto);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(TOPIC, 0), 0, 0, 0, 0, 0);
        SendResult<String, StockPriceDto> sendResult = new SendResult<>(record, metadata);

        when(kafkaTemplate.send(anyString(), anyString(), eq(dto)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        producer.publish(dto);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), keyCaptor.capture(), eq(dto));
        assertThat(keyCaptor.getValue()).isEqualTo("AAPL");
    }

    @Test
    void publish_doesNotThrowOnKafkaFailure() {
        StockPriceDto dto = aDto("MSFT");
        CompletableFuture<SendResult<String, StockPriceDto>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker unavailable"));

        when(kafkaTemplate.send(anyString(), anyString(), eq(dto))).thenReturn(failed);

        // must not propagate exception to caller
        producer.publish(dto);
    }

    private StockPriceDto aDto(String symbol) {
        return new StockPriceDto(symbol, new BigDecimal("150.00"), 1_000_000L,
                new BigDecimal("1.23"), OffsetDateTime.now(), "ALPHA_VANTAGE");
    }
}
